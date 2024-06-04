/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.mock.MockFileDocumentManagerImpl
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.EncodingManager
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.service
import io.kotest.matchers.shouldBe
import io.mockk.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.reflect.KFunction

class DocumentedSyncProviderTest : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("DocumentedSyncProvider:") {
    var isNewTextWritten = false
    var isUssAttr = false
    var isNullDock = false
    var isMoreClicked = false
    var getFileDocumentManager = false
    var getEncodingManager = false
    var notified = false

    afterEach {
      getFileDocumentManager = false
      isNewTextWritten = false
      getEncodingManager = false
      notified = false
      isUssAttr = false
      isNullDock = false
      isMoreClicked = false
    }

    val mockedDocument: Document = mockk<Document>()
    every { mockedDocument.text } returns "qwerty"
    every { mockedDocument.isWritable } returns false
    every { mockedDocument.setReadOnly(any()) } returns Unit
    every { mockedDocument.setText(any()) } answers {
      isNewTextWritten = true
    }

    val mockedVirtualFile = mockk<VirtualFile>()
    every { mockedVirtualFile.hashCode() } returns 13
    every { mockedVirtualFile.fileType } returns UnknownFileType.INSTANCE
    every { mockedVirtualFile.isDirectory } returns false
    every { mockedVirtualFile.getUserData(any<Key<Document>>()) } returns mockedDocument
    every { mockedVirtualFile.charset = any() } just Runs
    every { mockedVirtualFile.detectedLineSeparator = any() } just Runs

    val mockedEncodingManager = mockk<EncodingManager>()
    every { mockedEncodingManager.setEncoding(any(), any()) } just Runs
    mockkStatic(EncodingManager::getInstance)
    every { EncodingManager.getInstance() } answers {
      getEncodingManager = true
      mockedEncodingManager
    }

    val mockedNotification: (
      Notification
    ) -> Unit = Notifications.Bus::notify
    mockkStatic(mockedNotification as KFunction<*>)
    every {
      mockedNotification(
        any<Notification>()
      )
    } answers {
      notified = true
    }

    val f: (CharSequence) -> Document? = { mockedDocument }
    val mockedMockFileDocumentManager = spyk(MockFileDocumentManagerImpl(Key.create<Document>("MockDocument"), f))
    mockkStatic(FileDocumentManager::getInstance)
    every { FileDocumentManager.getInstance() } answers {
      getFileDocumentManager = true
      mockedMockFileDocumentManager
    }

    val mockedFileAttributes = mockk<FileAttributes>()
    val mockedExplorer = mockk<Explorer<ConnectionConfig, WorkingSet<ConnectionConfig, *>>>()
    every { mockedExplorer.componentManager } returns ApplicationManager.getApplication()
    val dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    dataOpsManager.testInstance = object : TestDataOpsManagerImpl(mockedExplorer.componentManager) {
      override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
        return mockedFileAttributes
      }
    }

    lateinit var addMaskActionInst: AnAction
    val mockedAnActionEvent = mockk<AnActionEvent>()
    every { mockedAnActionEvent.project } returns mockk()

    val documentedSyncProvider = spyk(DocumentedSyncProvider(file = mockedVirtualFile))
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
      every { mockedMockFileDocumentManager.getDocument(any()) } answers {
        isNullDock = true
        null
      }
      documentedSyncProvider.loadNewContent("131313".toByteArray())
      isNewTextWritten shouldBe false
      isNullDock shouldBe true
      documentedSyncProvider.isReadOnly shouldBe true
      clearMocks(mockedMockFileDocumentManager)
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
      every { documentedSyncProvider.loadNewContent(any()) } answers {
        throw Exception("test exception")
      }
      documentedSyncProvider.putInitialContent("131313".toByteArray())
      getEncodingManager shouldBe true
      isNewTextWritten shouldBe false
      clearMocks(documentedSyncProvider)
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
      val mockedRemoteUssAttributes = mockk<RemoteUssAttributes>()
      every { mockedRemoteUssAttributes.charset } answers {
        isUssAttr = true
        DEFAULT_BINARY_CHARSET
      }
      dataOpsManager.testInstance = object : TestDataOpsManagerImpl(mockedExplorer.componentManager) {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          return mockedRemoteUssAttributes
        }
      }
      documentedSyncProvider.retrieveCurrentContent()
      isUssAttr shouldBe true
    }

    should("test notification") {
      val notifyRef: (Notification) -> Unit = Notifications.Bus::notify
      mockkStatic(notifyRef as KFunction<*>)
      mockkStatic(Notification::get)
      every { Notifications.Bus.notify(any<Notification>()) } answers {
        val notification = firstArg<Notification>()
        every { Notification.get(any()) } returns notification
        addMaskActionInst = notification.actions.first { it.templateText == "More" }
        notified = true
      }
      val showDialogRef: (Project?, String, String) -> Unit = Messages::showErrorDialog
      mockkStatic(showDialogRef as KFunction<*>)
      every {
        showDialogRef(
          any(), any<String>(), any<String>()
        )
      } answers {
        isMoreClicked = true
      }
      val e = Exception()
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
      addMaskActionInst.actionPerformed(mockedAnActionEvent)
      isMoreClicked shouldBe true
    }

    should("test notification with dot") {
      val e = Exception("Call.Exception")
      documentedSyncProvider.onThrowable(e)
      notified shouldBe true
      addMaskActionInst.actionPerformed(mockedAnActionEvent)
      isMoreClicked shouldBe true
    }

  }
})