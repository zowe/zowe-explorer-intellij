/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.*
import retrofit2.Response

class DatasetFileFetchProviderTestSpec : AppInitShouldSpec("dataops/fetch/DatasetFileFetchProvider", {
  context("all functions") {
    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val mockedConnectionConfig = mockk<ConnectionConfig> {
      every { uuid } returns "test_uuid"
      every { url } returns "test_url"
    }
    val mockedQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, DSMask>> {
      every { request } returns mockk {
        every { mask } returns "TESTMASK"
        every { volser } returns "TESTVOL"
      }
      every { connectionConfig } returns mockedConnectionConfig
    }
    val dataSetsList = mockk<DataSetsList> {
      every { items } returns mutableListOf(
        mockk<Dataset> {
          every { name } returns "DATASET1"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PS
        },
        mockk<Dataset> {
          every { name } returns "DATASET2"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PS
        },
        mockk<Dataset> {
          every { name } returns "DATASET3"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PO
        }
      )
      every { totalRows } returns 3
    }
    val mockedResponse = mockk<Response<DataSetsList>> {
      every { body() } returns dataSetsList
      every { isSuccessful } returns true
    }

    val mockedApi = mockk<DataAPI> {
      every {
        listDataSets(
          authorizationToken = any<String>(),
          dsLevel = any<String>(),
          volser = any<String>(),
          xIBMAttr = any<XIBMAttr>(),
          xIBMMaxItems = any<Int>(),
          start = any<String>()
        )
      } returns mockk {
        every { execute() } returns mockedResponse
      }
    }

    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns mockedApi

    val dataOpsManagerService = DataOpsManager.getService()

    val datasetFileFetchProviderForTest =
      spyk(DatasetFileFetchProvider(dataOpsManagerService), recordPrivateCalls = true)

    context("fetchResponse") {
      val progressMockk = mockk<ProgressIndicator> {
        every { fraction = any<Double>() } just Runs
      }

      should("fetch dataset attributes") {
        val fetchResponseMethodRef = DatasetFileFetchProvider::class.java
          .getDeclaredMethod("fetchResponse", RemoteQuery::class.java, ProgressIndicator::class.java)
        fetchResponseMethodRef.isAccessible = true

        val datasetAttributes = fetchResponseMethodRef
          .invoke(datasetFileFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>

        assertSoftly { datasetAttributes shouldHaveSize dataSetsList.items.size }
      }
    }

    context("cleanupUnusedFile") {
      var didClearAttributes = false
      var didUpdateAttributes = false
      var didDeleteFile = false

      val mockedVirtualFile = mockk<MFVirtualFile> {
        every {
          delete(any<DatasetFileFetchProvider>())
        } answers {
          didDeleteFile = true
        }
      }
      val mockedMaskedRequester = mockk<MaskedRequester> {
        every { connectionConfig } returns mockedConnectionConfig
      }
      val requestersMock = mutableListOf(mockedMaskedRequester)
      val mockedFileAttributes = mockk<RemoteDatasetAttributes> {
        every { requesters } returns requestersMock
      }

      val mockedAttributesService = mockk<RemoteDatasetAttributesService> {
        every { getAttributes(mockedVirtualFile) } returns mockedFileAttributes
        every {
          clearAttributes(mockedVirtualFile)
        } answers {
          didClearAttributes = true
        }
        every {
          updateAttributes(mockedVirtualFile, any<RemoteDatasetAttributes.() -> Unit>())
        } answers {
          didUpdateAttributes = true
          secondArg<RemoteDatasetAttributes.() -> Unit>().invoke(mockedFileAttributes)
        }
      }

      every {
        dataOpsManagerService.getAttributesService(RemoteDatasetAttributes::class.java, MFVirtualFile::class.java)
      } returns mockedAttributesService

      val cleanupUnusedFileMethodRef = DatasetFileFetchProvider::class.java
        .getDeclaredMethod("cleanupUnusedFile", MFVirtualFile::class.java, RemoteQuery::class.java)
      cleanupUnusedFileMethodRef.isAccessible = true

      beforeEach {
        didDeleteFile = false
        didClearAttributes = false
        didUpdateAttributes = false
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file") {
        every { mockedMaskedRequester.queryVolser } returns "TESTVOL"

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          didUpdateAttributes shouldBe false
          didClearAttributes shouldBe true
          didDeleteFile shouldBe true
        }
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file (different volume)") {
        every { mockedMaskedRequester.queryVolser } returns "ANOTHER"

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          didUpdateAttributes shouldBe true
          didClearAttributes shouldBe false
          didDeleteFile shouldBe false
        }
      }
    }
  }
})
