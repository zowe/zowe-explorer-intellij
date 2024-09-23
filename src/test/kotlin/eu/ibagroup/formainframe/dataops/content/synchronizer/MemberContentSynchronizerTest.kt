/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.api.ZosmfApi
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.MaskedRequester
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributesService
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestZosmfApiImpl
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion

class MemberContentSynchronizerTest : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("MemberContentSynchronizer:") {
    val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
    val mockedRemoteDatasetAttributes = mockk<RemoteDatasetAttributes>()
    val mockedMaskedRequester = mockk<MaskedRequester>()
    val connConf = ConnectionConfig("000", "connName", "url", true, ZVersion.ZOS_2_1, "owner")
    every { mockedMaskedRequester.connectionConfig } returns connConf
    every { mockedRemoteDatasetAttributes.requesters } returns mutableListOf(mockedMaskedRequester)
    val rdaName = "RemoteDatasetAttributes.name"
    every { mockedRemoteDatasetAttributes.name } returns rdaName
    val mockedRemoteDatasetAttributesService = mockk<RemoteDatasetAttributesService>()
    every { mockedRemoteDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockedRemoteDatasetAttributes
    dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
      override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
        attributesClass: Class<out A>,
        vFileClass: Class<out F>
      ): AttributesService<A, F> {
        @Suppress("UNCHECKED_CAST")
        return mockedRemoteDatasetAttributesService as AttributesService<A, F>
      }
    }

    val memberContentSynchronizer = spyk(MemberContentSynchronizer(dataOpsManager))

    val mockedMFVirtualFile = mockk<MFVirtualFile>()
    val mockedParentMFVirtualFile = mockk<MFVirtualFile>()
    every { mockedParentMFVirtualFile.getParent() } returns null
    every { mockedMFVirtualFile.parent } returns mockedParentMFVirtualFile
    val pathToFile = "/path/to/file"
    every { mockedMFVirtualFile.path } returns pathToFile
    val mockedRemoteMemberAttributes = mockk<RemoteMemberAttributes>()
    every { mockedRemoteMemberAttributes.contentMode } returns XIBMDataType(XIBMDataType.Type.BINARY)
    every { mockedRemoteMemberAttributes.parentFile } returns mockedMFVirtualFile
    val rmaName = "RemoteMemberAttributes.name"
    every { mockedRemoteMemberAttributes.name } returns rmaName
    val mockedProgressIndicator = mockk<ProgressIndicator>()

    val mockedDataAPI = mockk<DataAPI>()
    val mockedCall = mockk<retrofit2.Call<String>>()
    val mockedResponse = mockk<retrofit2.Response<String>>()
    var isApiCallSuccessful = false
    every { mockedResponse.isSuccessful } answers {
      isApiCallSuccessful = true
      true
    }
    every { mockedResponse.body() } returns "Response.body"
    every { mockedCall.execute() } returns mockedResponse
    every {
      mockedDataAPI.retrieveMemberContent(
        any<String>(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns mockedCall
    val mockedCall1 = mockk<retrofit2.Call<Void>>()
    val mockedResponse1 = mockk<retrofit2.Response<Void>>()
    every { mockedCall1.execute() } returns mockedResponse1
    every { mockedResponse1.isSuccessful } answers {
      isApiCallSuccessful = true
      true
    }
    every {
      mockedDataAPI.writeToDatasetMember(
        any<String>(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any(),
        any()
      )
    } returns mockedCall1
    val testZosmfApi = ZosmfApi.getService() as TestZosmfApiImpl
    testZosmfApi.testInstance = object : TestZosmfApiImpl() {
      override fun <Api : Any> getApi(apiClass: Class<out Api>, connectionConfig: ConnectionConfig): Api {
        @Suppress("UNCHECKED_CAST")
        return mockedDataAPI as Api
      }

      override fun <Api : Any> getApiWithBytesConverter(
        apiClass: Class<out Api>,
        connectionConfig: ConnectionConfig
      ): Api {
        @Suppress("UNCHECKED_CAST")
        return mockedDataAPI as Api
      }
    }

    val fetchRemoteContentBytes = memberContentSynchronizer.javaClass.getDeclaredMethod(
      "fetchRemoteContentBytes",
      RemoteMemberAttributes::class.java,
      ProgressIndicator::class.java
    )
    fetchRemoteContentBytes.isAccessible = true
    val parameters = arrayOfNulls<Any>(2)
    parameters[0] = mockedRemoteMemberAttributes
    parameters[1] = mockedProgressIndicator

    val newContentBytes = "newContentBytes".toByteArray()
    val uploadNewContent = memberContentSynchronizer.javaClass.getDeclaredMethod(
      "uploadNewContent",
      RemoteMemberAttributes::class.java,
      ByteArray::class.java,
      ProgressIndicator::class.java
    )
    uploadNewContent.isAccessible = true
    val parameters1 = arrayOfNulls<Any>(3)
    parameters1[0] = mockedRemoteMemberAttributes
    parameters1[1] = newContentBytes
    parameters1[2] = mockedProgressIndicator

    var isExceptionThrown = false

    afterEach {
      isApiCallSuccessful = false
      isExceptionThrown = false
    }

    val code = 405

    should("Fetch remote content bytes for the dataset member") {
      val res = fetchRemoteContentBytes.invoke(memberContentSynchronizer, *parameters) as ByteArray
      res shouldBe "Response.body".toByteArray()
      isApiCallSuccessful shouldBe true
    }

    should("Failed fetchRemoteContentBytes") {
      isApiCallSuccessful = true
      every { mockedResponse.isSuccessful } answers {
        isApiCallSuccessful = false
        false
      }
      every { mockedResponse.code() } returns code
      try {
        fetchRemoteContentBytes.invoke(memberContentSynchronizer, *parameters)
      } catch (e: Exception) {
        isExceptionThrown = true
        e.cause?.shouldHaveMessage("Cannot fetch data from $rdaName($rmaName)\nCode: $code")
      }
      isExceptionThrown shouldBe true
      isApiCallSuccessful shouldBe false
      every { mockedResponse.isSuccessful } answers {
        isApiCallSuccessful = true
        true
      }
    }

    should("Throw fetchRemoteContentBytes") {
      every { mockedCall.execute() } throws Exception()
      try {
        fetchRemoteContentBytes.invoke(memberContentSynchronizer, *parameters)
      } catch (e: Exception) {
        isExceptionThrown = true
      }
      isExceptionThrown shouldBe true
      every { mockedCall.execute() } returns mockedResponse
    }

    should("null getAttributes for fetchRemoteContentBytes") {
      every { mockedRemoteDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns null
      try {
        fetchRemoteContentBytes.invoke(memberContentSynchronizer, *parameters)
      } catch (e: Exception) {
        isExceptionThrown = true
        e.cause?.shouldHaveMessage("Cannot find parent library attributes for library $pathToFile")
      }
      isExceptionThrown shouldBe true
      every { mockedRemoteDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockedRemoteDatasetAttributes
    }

    should("null body") {
      every { mockedResponse.body() } returns null
      try {
        val res = fetchRemoteContentBytes.invoke(memberContentSynchronizer, *parameters)
        println(res)
      } catch (e: Exception) {
        isExceptionThrown = true
        println(e.cause)
      }
      //res shouldBe null
      isApiCallSuccessful shouldBe true
    }

    should("Upload new content of the member to the mainframe") {
      every { mockedRemoteDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockedRemoteDatasetAttributes
      uploadNewContent.invoke(memberContentSynchronizer, *parameters1)
      isApiCallSuccessful shouldBe true
    }

    should("Failed uploadNewContent") {
      every { mockedRemoteMemberAttributes.contentMode } returns XIBMDataType(XIBMDataType.Type.TEXT)
      isApiCallSuccessful = true
      every { mockedResponse1.isSuccessful } answers {
        isApiCallSuccessful = false
        false
      }
      every { mockedResponse1.code() } returns code
      try {
        uploadNewContent.invoke(memberContentSynchronizer, *parameters1)
      } catch (e: Exception) {
        isExceptionThrown = true
        e.cause?.shouldHaveMessage("Cannot upload data to $rdaName($rmaName)\nCode: $code")
      }
      isExceptionThrown shouldBe true
      isApiCallSuccessful shouldBe false
    }

    should("Throw uploadNewContent") {
      every { mockedCall1.execute() } throws Exception()
      try {
        uploadNewContent.invoke(memberContentSynchronizer, *parameters1)
      } catch (e: Exception) {
        isExceptionThrown = true
      }
      isExceptionThrown shouldBe true
    }

    should("null getAttributes for uploadNewContent") {
      every { mockedRemoteDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns null
      try {
        uploadNewContent.invoke(memberContentSynchronizer, *parameters1)
      } catch (e: Exception) {
        isExceptionThrown = true
        e.cause?.shouldHaveMessage("Cannot find parent library attributes for library $pathToFile")
      }
      isExceptionThrown shouldBe true
    }

  }
})