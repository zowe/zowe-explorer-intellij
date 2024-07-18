/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.MaskedRequester
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributesService
import org.zowe.explorer.dataops.fetch.DatasetFileFetchProvider
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.testutils.testServiceImpl.TestZosmfApiImpl
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.service
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.DataSetsList
import org.zowe.kotlinsdk.Dataset
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.HasMigrated
import org.zowe.kotlinsdk.XIBMAttr
import retrofit2.Call
import retrofit2.Response

class DatasetFileFetchProviderTestSpec : ShouldSpec({

  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }

  afterSpec {
    clearAllMocks()
  }

  context("dataops module: fetch") {

    context("DatasetFileFetchProvider common") {
      val mockedConnectionConfig = mockk<ConnectionConfig>()
      every { mockedConnectionConfig.authToken } returns "auth_token"
      every { mockedConnectionConfig.url } returns "test_url"
      val mockedQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, DSMask>>()
      val mockedRequest = mockk<DSMask>()
      every { mockedRequest.mask } returns "TESTMASK"
      every { mockedRequest.volser } returns "TESTVOL"

      val progressMockk = mockk<ProgressIndicator>()
      every { progressMockk.fraction = any() as Double } just Runs

      every { mockedQuery.request } returns mockedRequest
      every { mockedQuery.connectionConfig } returns mockedConnectionConfig

      val dataset1 = mockk<Dataset>()
      val dataset2 = mockk<Dataset>()
      val dataset3 = mockk<Dataset>()
      every { dataset1.name } returns "DATASET1"
      every { dataset2.name } returns "DATASET2"
      every { dataset3.name } returns "DATASET3"
      every { dataset1.migrated } returns HasMigrated.NO
      every { dataset2.migrated } returns HasMigrated.NO
      every { dataset3.migrated } returns HasMigrated.NO
      every { dataset1.datasetOrganization } returns DatasetOrganization.PS
      every { dataset2.datasetOrganization } returns DatasetOrganization.PS
      every { dataset3.datasetOrganization } returns DatasetOrganization.PO
      val dataSetsList = mockk<DataSetsList>()
      every { dataSetsList.items } returns mutableListOf(dataset1, dataset2, dataset3)
      every { dataSetsList.totalRows } returns 3
      val mockedCall = mockk<Call<DataSetsList>>()
      val mockedResponse = mockk<Response<DataSetsList>>()
      every { mockedCall.execute() } returns mockedResponse
      every { mockedResponse.body() } returns dataSetsList

      val zosmfApi = ApplicationManager.getApplication().service<ZosmfApi>() as TestZosmfApiImpl
      zosmfApi.testInstance = mockk()
      val mockedApi = mockk<DataAPI>()
      every { zosmfApi.testInstance.getApi(DataAPI::class.java, mockedConnectionConfig) } returns mockedApi
      every {
        mockedApi.listDataSets(
          authorizationToken = any() as String,
          dsLevel = any() as String,
          volser = any() as String,
          xIBMAttr = any() as XIBMAttr,
          xIBMMaxItems = any() as Int,
          start = any() as String
        )
      } returns mockedCall
      every {
        mockedApi.listDataSets(
          authorizationToken = any() as String,
          dsLevel = any() as String,
          volser = any() as String,
          xIBMAttr = any() as XIBMAttr,
          xIBMMaxItems = any() as Int,
          start = any() as String
        ).cancelByIndicator(progressMockk)
      } returns mockedCall

      val dataOpsManagerService =
        ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl

      // needed for cleanupUnusedFile test
      val mockedVirtualFile = mockk<MFVirtualFile>()
      val mockedFileAttributes = mockk<RemoteDatasetAttributes>()
      val mockedAttributesService = mockk<RemoteDatasetAttributesService>()
      val mockedMaskedRequester = mockk<MaskedRequester>()
      val requesters = mutableListOf(mockedMaskedRequester)

      afterEach { unmockkAll() }

      val datasetFileFetchProviderForTest =
        spyk(DatasetFileFetchProvider(dataOpsManagerService), recordPrivateCalls = true)

      should("dataset fetch provider fetchResponse test") {
        val fetchResponseMethodRef =
          datasetFileFetchProviderForTest::class.java.declaredMethods.first { it.name == "fetchResponse" }
        fetchResponseMethodRef.trySetAccessible()
        every { mockedResponse.isSuccessful } returns true

        val datasetAttributes =
          (fetchResponseMethodRef.invoke(datasetFileFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>)

        assertSoftly {
          datasetAttributes shouldHaveSize dataSetsList.items.size
        }
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file") {
        var cleanupPerformed = false
        val cleanupUnusedFileMethodRef =
          datasetFileFetchProviderForTest::class.java.declaredMethods.first { it.name == "cleanupUnusedFile" }
        cleanupUnusedFileMethodRef.trySetAccessible()
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
            attributesClass: Class<out A>,
            vFileClass: Class<out F>
          ): AttributesService<A, F> {
            return mockedAttributesService as AttributesService<A, F>
          }
        }

        every { mockedAttributesService.getAttributes(mockedVirtualFile) } returns mockedFileAttributes
        every { mockedAttributesService.clearAttributes(mockedVirtualFile) } just Runs

        every { mockedMaskedRequester.connectionConfig } returns mockedConnectionConfig
        every { mockedMaskedRequester.queryVolser } returns "TESTVOL"
        every { mockedFileAttributes.requesters } returns requesters

        every { mockedVirtualFile.delete(any() as DatasetFileFetchProvider) } answers {
          cleanupPerformed = true
        }

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          cleanupPerformed shouldBe true
        }
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file") {
        var cleanupPerformed = false
        val cleanupUnusedFileMethodRef =
          datasetFileFetchProviderForTest::class.java.declaredMethods.first { it.name == "cleanupUnusedFile" }
        cleanupUnusedFileMethodRef.trySetAccessible()

        every { mockedAttributesService.getAttributes(mockedVirtualFile) } returns mockedFileAttributes
        every { mockedAttributesService.clearAttributes(mockedVirtualFile) } just Runs

        every { mockedMaskedRequester.connectionConfig } returns mockedConnectionConfig
        every { mockedMaskedRequester.queryVolser } returns "ANOTHER"
        every { mockedFileAttributes.requesters } returns requesters
        every {
          mockedAttributesService.updateAttributes(
            mockedVirtualFile,
            any() as RemoteDatasetAttributes.() -> Unit
          )
        } answers {
          cleanupPerformed = true
          secondArg<RemoteDatasetAttributes.() -> Unit>().invoke(mockedFileAttributes)
        }

        every { mockedVirtualFile.delete(any() as DatasetFileFetchProvider) } answers {
          cleanupPerformed = true
        }

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          cleanupPerformed shouldBe true
        }
      }
    }
  }
})
