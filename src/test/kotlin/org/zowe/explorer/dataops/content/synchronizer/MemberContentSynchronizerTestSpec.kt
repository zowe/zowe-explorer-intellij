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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.MaskedRequester
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributesService
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.XIBMDataType
import org.zowe.kotlinsdk.annotations.ZVersion

class MemberContentSynchronizerTestSpec : AppInitShouldSpec("dataops/content/synchronizer/MemberContentSynchronizer", {
  context("all functions") {
    var isApiCallSuccessful = false
    var isExceptionThrown = false

    val connConf = ConnectionConfig(
      "000",
      "connName",
      "url",
      true,
      ZVersion.ZOS_2_1,
      "owner"
    )
    val mockedMaskedRequester = mockk<MaskedRequester> {
      every { connectionConfig } returns connConf
    }
    val rdaName = "RemoteDatasetAttributes.name"
    val mockedRemoteDatasetAttributes = mockk<RemoteDatasetAttributes> {
      every { requesters } returns mutableListOf(mockedMaskedRequester)
      every { name } returns rdaName
    }
    val mockedRemoteDatasetAttributesService = mockk<RemoteDatasetAttributesService> {
      every { getAttributes(any<MFVirtualFile>()) } returns mockedRemoteDatasetAttributes
    }

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val dataOpsManager = DataOpsManager.getService()
    every { dataOpsManager.componentManager } returns ApplicationManager.getApplication()
    every {
      dataOpsManager.getAttributesService(RemoteDatasetAttributes::class.java, MFVirtualFile::class.java)
    } returns mockedRemoteDatasetAttributesService

    val memberContentSynchronizer = spyk(MemberContentSynchronizer(dataOpsManager))

    val mockedParentMFVirtualFile = mockk<MFVirtualFile> {
      every { getParent() } returns null
    }
    val pathToFile = "/path/to/file"
    val mockedMFVirtualFile = mockk<MFVirtualFile> {
      every { parent } returns mockedParentMFVirtualFile
      every { path } returns pathToFile
    }
    val rmaName = "RemoteMemberAttributes.name"
    val mockedRemoteMemberAttributes = mockk<RemoteMemberAttributes> {
      every { contentMode } returns XIBMDataType(XIBMDataType.Type.BINARY)
      every { parentFile } returns mockedMFVirtualFile
      every { name } returns rmaName
    }
    val mockedProgressIndicator = mockk<ProgressIndicator>()

    val mockedDataAPI = mockk<DataAPI>()
    val mockedResponse = mockk<retrofit2.Response<String>> {
      every {
        isSuccessful
      } answers {
        isApiCallSuccessful = true
        true
      }
      every { body() } returns "Response.body"
    }
    val mockedCall = mockk<retrofit2.Call<String>> {
      every { execute() } returns mockedResponse
    }
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
    val mockedResponse1 = mockk<retrofit2.Response<Void>> {
      every {
        isSuccessful
      } answers {
        isApiCallSuccessful = true
        true
      }
    }
    val mockedCall1 = mockk<retrofit2.Call<Void>> {
      every { execute() } returns mockedResponse1
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

    val testZosmfApi = ZosmfApi.getService()
    every { testZosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns mockedDataAPI
    every { testZosmfApi.getApiWithBytesConverter(DataAPI::class.java, any<ConnectionConfig>()) } returns mockedDataAPI

    val fetchRemoteContentBytes = memberContentSynchronizer.javaClass
      .getDeclaredMethod(
        "fetchRemoteContentBytes",
        RemoteMemberAttributes::class.java,
        ProgressIndicator::class.java
      )
    fetchRemoteContentBytes.isAccessible = true

    val parameters = arrayOf(mockedRemoteMemberAttributes, mockedProgressIndicator)

    val newContentBytes = "newContentBytes".toByteArray()
    val uploadNewContent = memberContentSynchronizer.javaClass
      .getDeclaredMethod(
        "uploadNewContent",
        RemoteMemberAttributes::class.java,
        ByteArray::class.java,
        ProgressIndicator::class.java
      )
    uploadNewContent.isAccessible = true

    val parameters1 = arrayOf(mockedRemoteMemberAttributes, newContentBytes, mockedProgressIndicator)

    val code = 405

    afterEach {
      isApiCallSuccessful = false
      isExceptionThrown = false
    }

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
