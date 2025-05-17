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

import com.intellij.mock.MockFileDocumentManagerImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.telemetry.NotificationsService
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.zowe.explorer.testutils.AppInitShouldSpec

class DocumentedSyncProviderTest : AppInitShouldSpec("dataops/content/synchronizer/DocumentedSyncProvider", {
  context("DocumentedSyncProvider") {
    var isNewTextWritten = false
    var isUssAttr = false
    var isNullDock = false
    var getFileDocumentManager = false
    var getEncodingManager = false
    var notified = false

    val mockedDocument: Document = mockk<Document> {
      every { text } returns "qwerty"
      every { isWritable } returns false
      every { setReadOnly(any()) } returns Unit
      every {
        setText(any())
      } answers {
        isNewTextWritten = true
      }
    }

    val mockedVirtualFile = mockk<VirtualFile> {
      every { fileType } returns UnknownFileType.INSTANCE
      every { isDirectory } returns false
      every { getUserData(any<Key<Document>>()) } returns mockedDocument
      every { charset = any() } just Runs
      every { detectedLineSeparator = any() } just Runs
    }
    every { mockedVirtualFile.hashCode() } returns 13

    val mockedEncodingManager = mockk<EncodingManager> {
      every { setEncoding(any(), any()) } just Runs
    }
    mockkStatic(EncodingManager::getInstance)
    every {
      EncodingManager.getInstance()
    } answers {
      getEncodingManager = true
      mockedEncodingManager
    }

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      notified = true
    }

    val f: (CharSequence) -> Document? = { mockedDocument }
    val mockedMockFileDocumentManager = spyk(MockFileDocumentManagerImpl(Key.create("MockDocument"), f))
    mockkStatic(FileDocumentManager::getInstance)
    every {
      FileDocumentManager.getInstance()
    } answers {
      getFileDocumentManager = true
      mockedMockFileDocumentManager
    }

    val mockedFileAttributes = mockk<FileAttributes>()
    val dataOpsManager = DataOpsManager.getService()
    every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockedFileAttributes

    val documentedSyncProvider = spyk(DocumentedSyncProvider(file = mockedVirtualFile))

    beforeEach {
      every {
        mockedMockFileDocumentManager.getDocument(any())
      } answers {
        callOriginal()
      }
      every {
        documentedSyncProvider.loadNewContent(any())
      } answers {
        callOriginal()
      }
    }

    afterEach {
      getFileDocumentManager = false
      isNewTextWritten = false
      getEncodingManager = false
      notified = false
      isUssAttr = false
      isNullDock = false
    }

    should("get hash code") {
      documentedSyncProvider.hashCode() shouldBe 13
    }

    should("equals") {
      val mockedVirtualFileTmp = mockk<VirtualFile>()
      documentedSyncProvider.equals(documentedSyncProvider) shouldBe true
      documentedSyncProvider.equals(mockedVirtualFileTmp) shouldBe false
      documentedSyncProvider.equals(DocumentedSyncProvider(mockedVirtualFile)) shouldBe true
      documentedSyncProvider.equals(DocumentedSyncProvider(mockedVirtualFileTmp)) shouldBe false
    }

    should("Extracts content from the file document") {
      documentedSyncProvider.retrieveCurrentContent() shouldBe "qwerty".toByteArray()
      getFileDocumentManager shouldBe true
    }

    should("Extracts content from the empty file document") {
      every { mockedVirtualFile.getUserData(any<Key<Document>>()) } returns null
      documentedSyncProvider.retrieveCurrentContent() shouldBe "".toByteArray()
      getFileDocumentManager shouldBe true
      every { mockedVirtualFile.getUserData(any<Key<Document>>()) } returns mockedDocument
    }

    should("Update content in null document") {
      every {
        mockedMockFileDocumentManager.getDocument(any())
      } answers {
        isNullDock = true
        null
      }
      documentedSyncProvider.loadNewContent("131313".toByteArray())
      isNewTextWritten shouldBe false
      isNullDock shouldBe true
      documentedSyncProvider.isReadOnly shouldBe true
    }

    should("Update content in file document") {
      every { mockedDocument.isWritable } returns true
      documentedSyncProvider.loadNewContent("131313".toByteArray())
      documentedSyncProvider.isReadOnly shouldBe false
      isNewTextWritten shouldBe true
    }

    should("Update content in file document to the same one") {
      documentedSyncProvider.loadNewContent("qwerty".toByteArray())
      isNewTextWritten shouldBe false
    }

    should("Throw exception in loadNewContent.") {
      every {
        documentedSyncProvider.loadNewContent(any())
      } answers {
        throw Exception("test exception")
      }
      documentedSyncProvider.putInitialContent("131313".toByteArray())
      getEncodingManager shouldBe true
      isNewTextWritten shouldBe false
    }

    should("Put initial content in file document.") {
      documentedSyncProvider.putInitialContent("141414".toByteArray())
      getEncodingManager shouldBe true
      isNewTextWritten shouldBe true
    }

    should("Put initial content again.") {
      documentedSyncProvider.putInitialContent("141414".toByteArray())
      getEncodingManager shouldBe false
      isNewTextWritten shouldBe false
    }

    should("Test throwable: Permission denied") {
      val e = Exception("Permission denied")
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
    }

    should("Test throwable: Any exception") {
      val e = Exception("Any exception")
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
    }

    should("Test throwable: Empty message exception") {
      val e = Exception()
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
    }

    should("Test throwable: CallException with dot") {
      val response: retrofit2.Response<String> = retrofit2.Response.error(
        403,
        "{\"details\":[\"Unknown:Details\"]}"
          .toResponseBody("application/json".toMediaTypeOrNull())
      )
      val e = CallException(response, "CallException.Exception")
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
    }

    should("Test throwable: CallException null title and details") {
      val response: retrofit2.Response<String> = retrofit2.Response.error(
        403,
        "".toResponseBody("application/json".toMediaTypeOrNull())
      )
      val e = CallException(response, "")
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
    }

    should("onSyncSuccess") {
      documentedSyncProvider.onSyncSuccess()
      verify { documentedSyncProvider.onSyncSuccess() }
    }

    should("not null RemoteUssAttributes.charset") {
      val mockedRemoteUssAttributes = mockk<RemoteUssAttributes> {
        every {
          charset
        } answers {
          isUssAttr = true
          DEFAULT_BINARY_CHARSET
        }
      }
      every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockedRemoteUssAttributes
      documentedSyncProvider.retrieveCurrentContent()
      isUssAttr shouldBe true
    }
  }
})
