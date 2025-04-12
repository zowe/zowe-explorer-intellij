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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.JobsRequester
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.dataops.attributes.RemoteJobAttributesService
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import org.zowe.explorer.dataops.attributes.Requester
import org.zowe.explorer.utils.log
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.kotlinsdk.SpoolFile
import retrofit2.Response

class DependentFileContentSynchronizerTest : AppInitShouldSpec("dataops/content/synchronizer/DependentFileContentSynchronizer", {
  context("all functions") {
    var message: Boolean? = false
    var isResponsed = false

    val dataOpsManager = DataOpsManager.getService()
    every { dataOpsManager.componentManager } returns ApplicationManager.getApplication()

    var responseStringFun: () -> Response<String>
    var responseVoidFun: () -> Response<Void>?
    var responseStr: Response<String> = Response.success(200, "Successful request")
    val responseVoid: Response<Void>? = Response.success(null)
    responseStringFun = { responseStr }
    responseVoidFun = {
      isResponsed = true
      responseVoid
    }

    val logger = log<DependentFileContentSynchronizerTest>()
    val classUnderTest = object :
      DependentFileContentSynchronizer<MFVirtualFile, SpoolFile, JobsRequester, RemoteSpoolFileAttributes, RemoteJobAttributes>(
        dataOpsManager,
        logger
      ) {
      override val vFileClass = MFVirtualFile::class.java

      override val entityName = "jobs"

      override val attributesClass = RemoteSpoolFileAttributes::class.java

      override val parentAttributesClass = RemoteJobAttributes::class.java
      override fun executePutContentRequest(
        attributes: RemoteSpoolFileAttributes,
        parentAttributes: RemoteJobAttributes,
        requester: Requester<ConnectionConfig>,
        newContentBytes: ByteArray,
        progressIndicator: ProgressIndicator?
      ): Response<Void>? {
        return responseVoidFun()
      }

      override fun executeGetContentRequest(
        attributes: RemoteSpoolFileAttributes,
        parentAttributes: RemoteJobAttributes,
        requester: Requester<ConnectionConfig>,
        progressIndicator: ProgressIndicator?
      ): Response<*> {

        return responseStringFun()
      }

      fun fetchRemoteContentBytesForTest(
        attributes: RemoteSpoolFileAttributes,
        progressIndicator: ProgressIndicator?
      ): ByteArray {
        return fetchRemoteContentBytes(attributes, progressIndicator)
      }

      fun uploadNewContentForTest(
        attributes: RemoteSpoolFileAttributes,
        newContentBytes: ByteArray,
        progressIndicator: ProgressIndicator?
      ) {
        return uploadNewContent(attributes, newContentBytes, progressIndicator)
      }
    }

    val mockedMFVirtualFile = mockk<MFVirtualFile> {
      every { path } returns "path"
    }
    val mockedRemoteSpoolFileAttributes = mockk<RemoteSpoolFileAttributes> {
      every { name } returns "RemoteSpoolFileAttributesName"
      every { parentFile } returns mockedMFVirtualFile
    }
    val mockedProgressIndicator = mockk<ProgressIndicator>()

    val mockedRemoteJobAttributes = mockk<RemoteJobAttributes> {
      every { requesters } returns mutableListOf(mockk<JobsRequester>())
      every { name } returns "RemoteJobAttributesName"
    }
    val mockedRemoteJobAttributesService = mockk<RemoteJobAttributesService> {
      every { getAttributes(any<MFVirtualFile>()) } returns mockedRemoteJobAttributes
    }

    every {
      dataOpsManager.getAttributesService(RemoteJobAttributes::class.java, MFVirtualFile::class.java)
    } returns mockedRemoteJobAttributesService

    afterEach {
      message = false
      isResponsed = false
    }

    should("Fetch remote content bytes for the dataset member: successful") {
      classUnderTest.fetchRemoteContentBytesForTest(mockedRemoteSpoolFileAttributes, mockedProgressIndicator)
        .toString(Charsets.UTF_8) shouldBe "Successful request"
    }

    should("Fetch remote content bytes for the dataset member: error") {
      responseStr = Response.error(
        403, "{\"details\":[\"Unknown:Details\"]}".toResponseBody("application/json".toMediaTypeOrNull())
      )
      try {
        classUnderTest.fetchRemoteContentBytesForTest(mockedRemoteSpoolFileAttributes, mockedProgressIndicator)
      } catch (t: Throwable) {
        message = t.message?.contains("Cannot fetch data from ")
      }
      message shouldBe true
    }


    should("Fetch remote content bytes for the dataset member: null attributes") {
      every { mockedRemoteJobAttributesService.getAttributes(any<MFVirtualFile>()) } returns null
      try {
        classUnderTest.fetchRemoteContentBytesForTest(mockedRemoteSpoolFileAttributes, mockedProgressIndicator)
      } catch (t: Throwable) {
        message = t.message?.contains("Cannot find parent file attributes for file")
      }
      message shouldBe true
      every { mockedRemoteJobAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockedRemoteJobAttributes
    }

    should("Fetch remote content bytes for the dataset member: Throwable") {
      responseStringFun = { throw Throwable("Throwable") }
      try {
        classUnderTest.fetchRemoteContentBytesForTest(mockedRemoteSpoolFileAttributes, mockedProgressIndicator)
      } catch (t: Throwable) {
        message = t.message?.contains("Throwable")
      }
      message shouldBe true
    }

    should("Upload new content bytes of the dependent file to the mainframe: success") {
      classUnderTest.uploadNewContentForTest(
        mockedRemoteSpoolFileAttributes,
        "qwerty".toByteArray(),
        mockedProgressIndicator
      )
      isResponsed shouldBe true
    }

    should("Upload new content bytes of the dependent file to the mainframe: null attributes") {
      every { mockedRemoteJobAttributesService.getAttributes(any<MFVirtualFile>()) } returns null
      try {
        classUnderTest.uploadNewContentForTest(
          mockedRemoteSpoolFileAttributes,
          "qwerty".toByteArray(),
          mockedProgressIndicator
        )
      } catch (t: Throwable) {
        message = t.message?.contains("Cannot find parent file attributes for file")
      }
      message shouldBe true
      every { mockedRemoteJobAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockedRemoteJobAttributes
    }

    should("Upload new content bytes of the dependent file to the mainframe: Throwable") {
      responseVoidFun = { throw Throwable("Throwable") }
      try {
        classUnderTest.uploadNewContentForTest(
          mockedRemoteSpoolFileAttributes,
          "qwerty".toByteArray(),
          mockedProgressIndicator
        )
      } catch (t: Throwable) {
        message = t.message?.contains("Throwable")
      }
      message shouldBe true
    }

    should("Upload new content bytes of the dependent file to the mainframe: null response") {
      responseVoidFun = {
        isResponsed = true
        null
      }
      classUnderTest.uploadNewContentForTest(
        mockedRemoteSpoolFileAttributes,
        "qwerty".toByteArray(),
        mockedProgressIndicator
      )
      isResponsed shouldBe true
    }
  }
})
