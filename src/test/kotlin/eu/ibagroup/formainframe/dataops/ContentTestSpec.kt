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

package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.content.service.SyncProcessService
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestSyncProcessServiceImpl
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.reflect.KFunction

class ContentTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("dataops module: content/synchronizer") {

    var wasWarningShown = false

    val virtualFileMock = mockk<VirtualFile>()
    every { virtualFileMock.name } returns "fileName"

    val syncProcessService = SyncProcessService.getService() as TestSyncProcessServiceImpl

    beforeEach {
      wasWarningShown = false

      val showWarningDialogRef: (Project, String, String) -> Unit = Messages::showWarningDialog
      mockkStatic(showWarningDialogRef as KFunction<*>)
      every { Messages.showWarningDialog(any<Project>(), any<String>(), any<String>()) } answers {
        wasWarningShown = true
      }

      syncProcessService.testInstance = TestSyncProcessServiceImpl()
    }

    afterEach {
      unmockkAll()
    }

    // syncUtils.removeLastNewLine
    should("remove last blank line") {}
    should("not remove last non-blank line") {}
    // syncUtils.checkFileForSync
    should("check file when it is syncing") {
      syncProcessService.testInstance = object : TestSyncProcessServiceImpl() {
        override fun isFileSyncingNow(file: VirtualFile): Boolean {
          return true
        }
      }

      val result = checkFileForSync(mockk(), virtualFileMock)

      assertSoftly {
        result shouldBe true
        wasWarningShown shouldBe true
      }
    }
    should("check file when it is not syncing") {
      val result = checkFileForSync(mockk(), virtualFileMock)

      assertSoftly {
        result shouldBe false
        wasWarningShown shouldBe false
      }
    }
    should("check file and dependent files when are they syncing") {
      syncProcessService.testInstance = object : TestSyncProcessServiceImpl() {
        override fun isFileSyncingNow(file: VirtualFile): Boolean {
          return true
        }

        override fun areDependentFilesSyncingNow(file: VirtualFile): Boolean {
          return true
        }
      }

      val result = checkFileForSync(mockk(), virtualFileMock, checkDependentFiles = true)

      assertSoftly {
        result shouldBe true
        wasWarningShown shouldBe true
      }
    }
    should("check file and dependent files when are they not syncing") {
      val result = checkFileForSync(mockk(), virtualFileMock, checkDependentFiles = true)

      assertSoftly {
        result shouldBe false
        wasWarningShown shouldBe false
      }
    }
    // SyncAction.actionPerformed
    should("synchronize the file with the remote file") {}
    // MemberContentSynchronizer.fetchRemoteContentBytes
    should("fetch remote content bytes") {}
    // MemberContentSynchronizer.uploadNewContent
    should("upload new content to the mainframe") {}
    // DocumentedSyncProvider.putInitialContent
    should("put initial file content when the file is read-only") {}
  }
  context("dataops module: content/adapters") {
    // SeqDatasetContentAdapter.adaptContentToMainframe
    should("adapt content for the dataset with variable length") {}
    should("adapt content for the dataset with variable print length") {}
    should("adapt content for the dataset with fixed length") {}
    // SeqDatasetContentAdapter.adaptContentFromMainframe
    should("adapt content for the dataset from mainframe with variable print length") {}
  }
  context("dataops module: content/service") {

    val virtualFileMock = mockk<VirtualFile>()
    val progressIndicatorMock = mockk<ProgressIndicator>()

    beforeEach {
      val isAncestorRef: (VirtualFile, VirtualFile, Boolean) -> Boolean = VfsUtilCore::isAncestor
      mockkStatic(isAncestorRef as KFunction<*>)
    }

    afterEach {
      unmockkAll()
    }

    // SyncProcessServiceImpl.isFileSyncingNow
    // TODO: rewrite to the service usage
//    should("file is syncing now") {
//      every { progressIndicatorMock.isRunning } returns true
//
//      val result = SyncProcessService.getService().isFileSyncingNow(virtualFileMock)
//
//      assertSoftly {
//        result shouldBe true
//      }
//    }
    should("file is not syncing now") {
      every { progressIndicatorMock.isRunning } returns false

      val result = SyncProcessService.getService().isFileSyncingNow(virtualFileMock)

      assertSoftly {
        result shouldBe false
      }
    }
    // SyncProcessServiceImpl.areDependentFilesSyncingNow
    // TODO: rewrite to the service usage
//    should("dependent files are syncing now") {
//      every { progressIndicatorMock.isRunning } returns true
//      every { VfsUtilCore.isAncestor(virtualFileMock, any(), true) } returns true
//
//      val result = SyncProcessService.getService().areDependentFilesSyncingNow(virtualFileMock)
//
//      assertSoftly {
//        result shouldBe true
//      }
//    }
    should("dependent files are not syncing now") {
      every { progressIndicatorMock.isRunning } returns true
      every { VfsUtilCore.isAncestor(virtualFileMock, any(), true) } returns false

      val result = SyncProcessService.getService().areDependentFilesSyncingNow(virtualFileMock)

      assertSoftly {
        result shouldBe false
      }
    }
    should("dependent files are not syncing now because no sync is running") {
      every { progressIndicatorMock.isRunning } returns false

      val result = SyncProcessService.getService().areDependentFilesSyncingNow(virtualFileMock)

      assertSoftly {
        result shouldBe false
      }
    }
  }
})
