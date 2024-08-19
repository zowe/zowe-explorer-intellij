/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.common.ui.cleanInvalidateOnExpand
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.dataops.operations.mover.MoveCopyOperation
import org.zowe.explorer.dataops.operations.mover.names.DatasetOrDirResolver
import org.zowe.explorer.dataops.operations.mover.names.DefaultNameResolver
import org.zowe.explorer.dataops.operations.mover.names.SeqToPDSResolver
import org.zowe.explorer.explorer.AbstractExplorerBase
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.FileExplorerContentProvider
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.getAncestorNodes
import org.zowe.explorer.utils.ui.WindowsLikeMessageDialog
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.vfs.MFVirtualFileSystem
import org.zowe.explorer.vfs.MFVirtualFileSystemModel
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Icon
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.jvm.isAccessible

class ExplorerPasteProviderTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("explorer module: ui/ExplorerPasteProvider") {
    context("perform paste via paste provider") {
      val mockedDataContext = mockk<DataContext>()
      val mockedFileExplorer = mockk<FileExplorer>()
      val mockedFileExplorerView = mockk<FileExplorerView>()
      val mockedCopyPasterProvider = mockk<FileExplorerView.ExplorerCopyPasteSupport>()
      val mockedProject = mockk<Project>()
      every { mockedCopyPasterProvider.project } returns mockedProject

      val mockedExplorerPasteProvider = spyk(
        ExplorerPasteProvider(), recordPrivateCalls = true
      )

      var dataOpsManagerService = service<DataOpsManager>() as TestDataOpsManagerImpl
      dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
        override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
          return mockk()
        }

        override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
          @Suppress("UNCHECKED_CAST")
          return Unit as R
        }
      }
      mockkObject(dataOpsManagerService.testInstance)

      // Clipboard buffer
      val mockedClipboardBuffer = emptyList<MFVirtualFile>()
      every { mockedCopyPasterProvider.getSourceFilesFromClipboard() } returns mockedClipboardBuffer

      every { mockedFileExplorerView.isCut } returns AtomicBoolean(true)
      every { mockedFileExplorerView.ignoreVFSChangeEvents } returns AtomicBoolean(false)
      every { mockedFileExplorerView.copyPasteSupport } returns mockedCopyPasterProvider

      mockkObject(FileExplorerContentProvider)
      //mockkObject(FileExplorerContentProvider::getInstance)
      every {
        FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
      } returns mockedFileExplorerView

      var isPastePerformed: Boolean
      var exceptionIsThrown: Boolean
      var isLockPerformed = false
      var isUnlockPerformed = false
      var isCallbackCalled = false

      every { mockedCopyPasterProvider.bufferLock } returns mockk()
      every { mockedCopyPasterProvider.bufferLock.lock() } answers {
        isLockPerformed = true
      }
      every { mockedCopyPasterProvider.bufferLock.unlock() } answers {
        isUnlockPerformed = true
      }

      val mockedExplorerBase = mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSet, FilesWorkingSetConfig>>()
      every { mockedFileExplorerView.explorer } returns mockedExplorerBase

      every { mockedFileExplorerView.explorer.reportThrowable(any<Throwable>(), mockedProject) } answers {
        exceptionIsThrown = true
        isPastePerformed = false
      }

      val nodeToRefreshSource = mockk<UssDirNode>()
      val nodeToRefreshTarget = mockk<UssDirNode>()
      beforeEach {
        every {
          dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
        } returns DefaultNameResolver()

        // we do not need to refresh the nodes in below tests, so lets return default list for each node to refresh (USS for example)
        val mockedSourceVFile = mockk<MFVirtualFile>()
        val mockedDestinationVFile = mockk<MFVirtualFile>()
        val sourceParentFile = mockk<MFVirtualFile>()
        val targetParentFile = mockk<MFVirtualFile>()
        every { mockedFileExplorerView.myFsTreeStructure } returns mockk()
        every { mockedFileExplorerView.myStructure } returns mockk()
        every { nodeToRefreshSource.parent } returns nodeToRefreshSource
        every { nodeToRefreshTarget.parent } returns nodeToRefreshTarget
        every { nodeToRefreshSource.virtualFile } returns mockedSourceVFile
        every { nodeToRefreshTarget.virtualFile } returns mockedDestinationVFile
        every { mockedSourceVFile.parent } returns sourceParentFile
        every { mockedDestinationVFile.parent } returns targetParentFile
        every { sourceParentFile.parent } returns null
        every { targetParentFile.parent } returns null
        every { mockedSourceVFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceVFile.fileSystem.model } returns mockk<MFVirtualFileSystemModel>()
        every { mockedSourceVFile.fileSystem.model.deleteFile(any(), any()) } just Runs
        every { mockedDestinationVFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedDestinationVFile.fileSystem.model } returns mockk<MFVirtualFileSystemModel>()
        every { mockedDestinationVFile.fileSystem.model.deleteFile(any(), any()) } just Runs
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(any() as VirtualFile) } returns listOf(
          nodeToRefreshSource,
          nodeToRefreshTarget
        )

        // refresh nodes
        every { nodeToRefreshSource.query } returns mockk()
        every { nodeToRefreshTarget.query } returns mockk()
        mockkStatic("org.zowe.explorer.common.ui.TreeUtilsKt")
        every { cleanInvalidateOnExpand(any(), mockedFileExplorerView) } just Runs
        every { mockedFileExplorerView.myStructure.invalidate(nodeToRefreshSource, true) } returns mockk()
        every { mockedFileExplorerView.myStructure.invalidate(nodeToRefreshTarget, true) } returns mockk()
        every { nodeToRefreshSource.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }
        every { nodeToRefreshTarget.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }

        mockkStatic(::checkFileForSync)
      }

      // performPaste
      should("perform paste without conflicts USS file -> USS folder") {
        isPastePerformed = false
        every { mockedFileExplorerView.isCut } returns AtomicBoolean(false)

        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<UssFileNode>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedSourceFile.name } returns "test5"
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceFile.parent } returns null
        every { mockedSourceAttributes.isPastePossible } returns false
        every { mockedSourceAttributes.isDirectory } returns false
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        val childDestFileAttributes = mockk<RemoteUssAttributes>()
        every { childDestinationVirtualFile.name } returns "test_file"
        every { childDestFileAttributes.isDirectory } returns false

        // target to paste
        val mockedTargetFile = mockk<MFVirtualFile>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        val targetAttributes = mockk<RemoteUssAttributes>()
        every { mockedTargetFile.name } returns "test_folder"
        every { targetAttributes.isDirectory } returns true
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        // removeFromBuffer callback
        every { mockedCopyPasterProvider.removeFromBuffer(any() as ((NodeData<*>) -> Boolean)) } answers {
          isCallbackCalled = true
          firstArg<((NodeData<*>) -> Boolean)>().invoke(copyPasteNodeData)
        }

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any() as List<VirtualFile>,
            any() as List<VirtualFile>,
            any() as Boolean
          )
        } returns
          mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste without conflicts USS file -> USS folder, but refresh returns if parent node query is null") {
        isPastePerformed = false
        every { nodeToRefreshSource.query } answers {
          isPastePerformed = true
          null
        }
        every { nodeToRefreshTarget.query } answers {
          isPastePerformed = true
          null
        }
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste without conflicts USS file -> USS folder, but refresh returns if parent node is not FileFetchNode") {
        isPastePerformed = false
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(any() as VirtualFile) } answers {
          isPastePerformed = true
          listOf(
            mockk(),
            mockk()
          )
        }
        every { nodeToRefreshSource.parent } answers {
          isPastePerformed = true
          mockk()
        }
        every { nodeToRefreshTarget.parent } answers {
          isPastePerformed = true
          mockk()
        }
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste without conflicts USS file -> PDS without skipping") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = false

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        every { mockedFileExplorerView.isCut } returns AtomicBoolean(true)

        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<UssFileNode>()
        val mockedSourceNodeParent = mockk<UssDirNode>()
        val mockedSourceFileParent = mockk<MFVirtualFile>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedSourceFile.name } returns "test_file_source"
        every { mockedSourceNode.virtualFile } returns mockedSourceFile
        every { mockedSourceNode.parent } returns mockedSourceNodeParent
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceFile.fileSystem.model } returns mockk<MFVirtualFileSystemModel>()
        every { mockedSourceFile.fileSystem.model.deleteFile(any(), any()) } just Runs
        every { mockedSourceFile.parent } returns mockedSourceFileParent
        every { mockedSourceFileParent.parent } returns null
        every { mockedSourceAttributes.isPastePossible } returns false
        every { mockedSourceAttributes.isDirectory } returns false
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceFile.isInLocalFileSystem } returns false
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        val childDestFileAttributes = mockk<RemoteMemberAttributes>()
        every { childDestinationVirtualFile.name } returns "EMPTY"

        // target to paste
        val mockedTargetFile = mockk<MFVirtualFile>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val targetAttributes = mockk<RemoteDatasetAttributes>()
        every { mockedTargetFile.name } returns "test_library"
        every { targetAttributes.isDirectory } returns true
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        // overwrite default behavior to check if nodes are actually refreshed
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(mockedSourceFile) } returns listOf(mockedSourceNode)
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(mockedTargetFile) } returns listOf(mockedNodeTarget)
        every { mockedFileExplorerView.myStructure.invalidate(mockedSourceNodeParent, true) } returns mockk()
        every { mockedFileExplorerView.myStructure.invalidate(mockedNodeTarget, true) } returns mockk()
        every { mockedSourceNodeParent.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }
        every { mockedNodeTarget.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any<List<VirtualFile>>(), any<List<VirtualFile>>(), any<Boolean>()
          )
        } returns mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste without conflicts USS file -> PDS with skipping") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = false

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          false
        }

        every { mockedFileExplorerView.isCut } returns AtomicBoolean(false)

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe false
        }
      }

      should("perform paste without conflicts PS file -> PDS without skipping") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = false

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        every { mockedFileExplorerView.isCut } returns AtomicBoolean(true)

        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<FileLikeDatasetNode>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceNodeParent = mockk<DSMaskNode>()
        val mockedSourceFileParent = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteDatasetAttributes>()
        every { mockedSourceNode.virtualFile } returns mockedSourceFile
        every { mockedSourceNode.parent } returns mockedSourceNodeParent
        every { mockedSourceFile.name } returns "TEST.FILE"
        every { mockedSourceFile.isInLocalFileSystem } returns false
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceFile.parent } returns mockedSourceFileParent
        every { mockedSourceFileParent.parent } returns null
        every { mockedSourceFile.parent } returns null
        every { mockedSourceAttributes.name } returns "TEST.FILE"
        every { mockedSourceAttributes.isPastePossible } returns false
        every { mockedSourceAttributes.isDirectory } returns false
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        val childDestFileAttributes = mockk<RemoteMemberAttributes>()
        every { childDestinationVirtualFile.name } returns "EMPTY"

        // target to paste
        val mockedTargetFile = mockk<MFVirtualFile>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val targetAttributes = mockk<RemoteDatasetAttributes>()
        every { mockedTargetFile.name } returns "test_library"
        every { targetAttributes.isDirectory } returns true
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        // special config for PS/PDS files
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(mockedSourceFile) } returns listOf(mockedSourceNode)
        every { mockedFileExplorerView.myFsTreeStructure.findByVirtualFile(mockedTargetFile) } returns listOf(mockedNodeTarget)
        every { mockedFileExplorerView.myStructure.invalidate(mockedSourceNodeParent, true) } returns mockk()
        every { mockedFileExplorerView.myStructure.invalidate(mockedNodeTarget, true) } returns mockk()
        every { mockedSourceNodeParent.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }
        every { mockedNodeTarget.cleanCache(cleanBatchedQuery = true) } answers {
          isPastePerformed = true
        }

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any() as List<VirtualFile>,
            any() as List<VirtualFile>,
            any() as Boolean
          )
        } returns
          mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste to the same USS folder with conflicts and not overwriting") {
        var isShowYesNoDialogCalled = false
        var isShowDialogCalled = false
        isPastePerformed = false

        mockkObject(WindowsLikeMessageDialog.Companion)
        every {
          WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
            any(),
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
        } answers {
          isShowDialogCalled = true
          0
        }

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<UssFileNode>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedSourceAttributes.isDirectory } returns true
        every { mockedSourceAttributes.isPastePossible } returns true
        every { mockedSourceFile.name } returns "test_folder_source"
        every { mockedSourceFile.parent } returns null
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes
        // needed for cleaning buffer function after paste is performed (because conflict)
        mockkStatic("org.zowe.explorer.utils.OpenapiUtilsKt")
        every { mockedSourceFile.getAncestorNodes() } returns mutableListOf()

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        val childDestFileAttributes = mockk<RemoteUssAttributes>()
        every { childDestinationVirtualFile.name } returns "test_folder_source"
        every { childDestinationVirtualFile.isDirectory } returns false
        every { childDestinationVirtualFile.delete(any()) } just Runs
        every { childDestFileAttributes.isDirectory } returns false
        every { mockedSourceFile.children } returns arrayOf(childDestinationVirtualFile)

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns null
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns null

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any<List<VirtualFile>>(), any<List<VirtualFile>>(), any<Boolean>()
          )
        } returns mutableListOf(Pair(mockedSourceFile, mockedSourceFile))
        every { mockedSourceFile.findChild(any<String>()) } returns null

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowDialogCalled shouldBe true
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe false
        }
      }

      should("perform paste to the same USS folder with conflicts and not overwriting (not D&D") {
        isPastePerformed = false
        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns null
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe false
        }
      }

      should("decline move/download files") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = false
        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          false
        }

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe false
        }
      }

      should("perform paste to the same USS folder with conflicts and accept overwriting") {
        var isShowYesNoDialogCalled = false
        var isShowDialogCalled = false
        isPastePerformed = false

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        mockkObject(WindowsLikeMessageDialog.Companion)
        every {
          WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
            any(),
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
        } answers {
          isShowDialogCalled = true
          1
        }

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isShowDialogCalled shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("return if unrecognized dialog message") {
        isPastePerformed = true
        mockkObject(WindowsLikeMessageDialog.Companion)
        every {
          WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
            any(),
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
        } answers {
          isPastePerformed = false
          3
        }
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe false
        }
      }

      should("perform paste without conflicts USS file -> Local folder") {
        isPastePerformed = false
        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<UssFileNode>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedSourceFile.name } returns "test5"
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceFile.parent } returns null
        every { mockedSourceAttributes.isPastePossible } returns false
        every { mockedSourceAttributes.isDirectory } returns false
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

        // children of target
        val childDestinationVirtualFile = mockk<VirtualFile>()
        every { childDestinationVirtualFile.name } returns "test_file"

        // target to paste
        val mockedTargetFile = mockk<VirtualFile>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<PsiDirectoryNode>()
        every { mockedTargetFile.name } returns "test_folder"
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any() as List<VirtualFile>,
            any() as List<VirtualFile>,
            any() as Boolean
          )
        } returns
          mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns null
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns null

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe true
        }
      }

      should("perform paste with conflicts USS files (>5) -> Local folder + remote but declining download") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = true

        every { mockedFileExplorerView.isCut } returns AtomicBoolean(false)

        mockkObject(WindowsLikeMessageDialog.Companion)
        every {
          WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
            any(),
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
        } answers {
          isShowYesNoDialogCalled = true
          1
        }

        val showDialogSpecificMock: (
          Project?, String, String, Array<String>, Int, Icon?
        ) -> Int = Messages::showDialog
        mockkStatic(showDialogSpecificMock as KFunction<*>)
        every {
          showDialogSpecificMock(
            any(), any<String>(), any<String>(), any<Array<String>>(), any<Int>(), any() as Icon?
          )
        } answers {
          isShowYesNoDialogCalled = true
          0
        }

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          val dialogTitle = firstArg<String>()
          isPastePerformed = false
          !dialogTitle.contains("Downloading Files")
        }

        // source1 to paste
        val mockedSourceNodeData1 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode1 = mockk<UssFileNode>()
        val mockedSourceFile1 = mockk<MFVirtualFile>()
        val mockedSourceAttributes1 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile1.name } returns "test1"
        every { mockedSourceFile1.isDirectory } returns false
        every { mockedSourceFile1.parent } returns null
        every { mockedSourceFile1.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes1.isPastePossible } returns false
        every { mockedSourceAttributes1.isDirectory } returns false
        every { mockedSourceNodeData1.node } returns mockedSourceNode1
        every { mockedSourceNodeData1.file } returns mockedSourceFile1
        every { mockedSourceNodeData1.attributes } returns mockedSourceAttributes1

        //source2 to paste
        val mockedSourceNodeData2 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode2 = mockk<UssFileNode>()
        val mockedSourceFile2 = mockk<MFVirtualFile>()
        val mockedSourceAttributes2 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile2.name } returns "test2"
        every { mockedSourceFile2.isDirectory } returns true
        every { mockedSourceFile2.parent } returns null
        every { mockedSourceFile2.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes2.isPastePossible } returns false
        every { mockedSourceAttributes2.isDirectory } returns false
        every { mockedSourceNodeData2.node } returns mockedSourceNode2
        every { mockedSourceNodeData2.file } returns mockedSourceFile2
        every { mockedSourceNodeData2.attributes } returns mockedSourceAttributes2

        //source3 to paste
        val mockedSourceNodeData3 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode3 = mockk<UssFileNode>()
        val mockedSourceFile3 = mockk<MFVirtualFile>()
        val mockedSourceAttributes3 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile3.name } returns "test3"
        every { mockedSourceFile3.isDirectory } returns false
        every { mockedSourceFile3.parent } returns null
        every { mockedSourceFile3.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes3.isPastePossible } returns false
        every { mockedSourceAttributes3.isDirectory } returns false
        every { mockedSourceNodeData3.node } returns mockedSourceNode3
        every { mockedSourceNodeData3.file } returns mockedSourceFile3
        every { mockedSourceNodeData3.attributes } returns mockedSourceAttributes3

        //source4 to paste
        val mockedSourceNodeData4 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode4 = mockk<UssFileNode>()
        val mockedSourceFile4 = mockk<MFVirtualFile>()
        val mockedSourceAttributes4 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile4.name } returns "test4"
        every { mockedSourceFile4.parent } returns null
        every { mockedSourceFile4.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes4.isPastePossible } returns false
        every { mockedSourceAttributes4.isDirectory } returns false
        every { mockedSourceNodeData4.node } returns mockedSourceNode4
        every { mockedSourceNodeData4.file } returns mockedSourceFile4
        every { mockedSourceNodeData4.attributes } returns mockedSourceAttributes4

        //source5 to paste
        val mockedSourceNodeData5 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode5 = mockk<UssFileNode>()
        val mockedSourceFile5 = mockk<MFVirtualFile>()
        val mockedSourceAttributes5 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile5.name } returns "test5"
        every { mockedSourceFile5.parent } returns null
        every { mockedSourceFile5.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes5.isPastePossible } returns false
        every { mockedSourceAttributes5.isDirectory } returns false
        every { mockedSourceNodeData5.node } returns mockedSourceNode5
        every { mockedSourceNodeData5.file } returns mockedSourceFile5
        every { mockedSourceNodeData5.attributes } returns mockedSourceAttributes5

        //source6 to paste
        val mockedSourceNodeData6 = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode6 = mockk<UssFileNode>()
        val mockedSourceFile6 = mockk<MFVirtualFile>()
        val mockedSourceAttributes6 = mockk<RemoteUssAttributes>()
        every { mockedSourceFile6.name } returns "test6"
        every { mockedSourceFile6.isDirectory } returns true
        every { mockedSourceFile6.parent } returns null
        every { mockedSourceFile6.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceAttributes6.isPastePossible } returns false
        every { mockedSourceAttributes6.isDirectory } returns true
        every { mockedSourceNodeData6.node } returns mockedSourceNode6
        every { mockedSourceNodeData6.file } returns mockedSourceFile6
        every { mockedSourceNodeData6.attributes } returns mockedSourceAttributes6

        // children of target
        val childDestinationVirtualFile1 = mockk<VirtualFile>()
        every { childDestinationVirtualFile1.name } returns "test1"
        every { childDestinationVirtualFile1.isDirectory } returns true
        val childDestinationVirtualFile2 = mockk<MFVirtualFile>()
        every { childDestinationVirtualFile2.name } returns "test_mf_file"
        val childDestMFFile2Attr = mockk<RemoteUssAttributes>()
        val childDestinationVirtualFile3 = mockk<VirtualFile>()
        every { childDestinationVirtualFile3.name } returns "test2"
        every { childDestinationVirtualFile3.isDirectory } returns false

        // target to paste
        val mockedTargetFile1 = mockk<VirtualFile>()
        val mockedTargetFile2 = mockk<MFVirtualFile>()
        val mockedTargetFile3 = mockk<VirtualFile>()
        val mockedTargetFile2Attributes = mockk<RemoteUssAttributes>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<PsiDirectoryNode>()
        every { mockedTargetFile1.parent } returns null
        every { mockedTargetFile1.name } returns "test_folder"
        every { mockedTargetFile1.children } returns arrayOf(childDestinationVirtualFile1)
        every { mockedTargetFile3.parent } returns null
        every { mockedTargetFile3.name } returns "test_folder2"
        every { mockedTargetFile3.children } returns arrayOf(childDestinationVirtualFile3)
        every { mockedTargetFile2.parent } returns null
        every { mockedTargetFile2.name } returns "test_mf_folder"
        every { mockedTargetFile2.children } returns arrayOf(childDestinationVirtualFile2)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile1

        // CopyPaste node buffer
        val copyPasteNodeData1 = mockk<NodeData<*>>()
        val copyPasteNodeData2 = mockk<NodeData<*>>()
        val copyPasteNodeData3 = mockk<NodeData<*>>()
        val copyPasteNodeData4 = mockk<NodeData<*>>()
        val copyPasteNodeData5 = mockk<NodeData<*>>()
        val copyPasteNodeData6 = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(
          copyPasteNodeData1,
          copyPasteNodeData2,
          copyPasteNodeData3,
          copyPasteNodeData4,
          copyPasteNodeData5,
          copyPasteNodeData6
        )
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData1.node } returns mockedSourceNode1
        every { copyPasteNodeData1.file } returns mockedSourceFile1
        every { copyPasteNodeData1.attributes } returns mockedSourceAttributes1
        every { copyPasteNodeData2.node } returns mockedSourceNode2
        every { copyPasteNodeData2.file } returns mockedSourceFile2
        every { copyPasteNodeData2.attributes } returns mockedSourceAttributes2
        every { copyPasteNodeData3.node } returns mockedSourceNode3
        every { copyPasteNodeData3.file } returns mockedSourceFile3
        every { copyPasteNodeData3.attributes } returns mockedSourceAttributes3
        every { copyPasteNodeData4.node } returns mockedSourceNode4
        every { copyPasteNodeData4.file } returns mockedSourceFile4
        every { copyPasteNodeData4.attributes } returns mockedSourceAttributes4
        every { copyPasteNodeData5.node } returns mockedSourceNode5
        every { copyPasteNodeData5.file } returns mockedSourceFile5
        every { copyPasteNodeData5.attributes } returns mockedSourceAttributes5
        every { copyPasteNodeData6.node } returns mockedSourceNode6
        every { copyPasteNodeData6.file } returns mockedSourceFile6
        every { copyPasteNodeData6.attributes } returns mockedSourceAttributes6
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(
          mockedSourceNodeData1, mockedSourceNodeData2, mockedSourceNodeData3,
          mockedSourceNodeData4, mockedSourceNodeData5, mockedSourceNodeData6
        )

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any<List<VirtualFile>>(), any<List<VirtualFile>>(), any<Boolean>()
          )
        } returns
          mutableListOf(
            Pair(mockedTargetFile1, mockedSourceFile1),
            Pair(mockedTargetFile1, mockedSourceFile2),
            Pair(mockedTargetFile1, mockedSourceFile3),
            Pair(mockedTargetFile1, mockedSourceFile4),
            Pair(mockedTargetFile1, mockedSourceFile5),
            Pair(mockedTargetFile1, mockedSourceFile6),
            Pair(mockedTargetFile2, mockedSourceFile1),
            Pair(mockedTargetFile2, mockedSourceFile2),
            Pair(mockedTargetFile2, mockedSourceFile3),
            Pair(mockedTargetFile3, mockedSourceFile2)
          )

        every { mockedTargetFile1.findChild(any<String>()) } returns childDestinationVirtualFile1
        every { mockedTargetFile3.findChild(any<String>()) } returns childDestinationVirtualFile3

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile1) } returns null
        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile2) } returns childDestMFFile2Attr
        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile3) } returns null
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile1) } returns mockedSourceAttributes1
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile2) } returns mockedSourceAttributes2
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile3) } returns mockedSourceAttributes3
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile4) } returns mockedSourceAttributes4
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile5) } returns mockedSourceAttributes5
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile6) } returns mockedSourceAttributes6
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile1) } returns null
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile2) } returns mockedTargetFile2Attributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile3) } returns null

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every {
          mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        } returns arrayOf(mockedTargetFile1, mockedTargetFile2)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe false
        }
      }

      should("perform paste without conflicts Local -> USS") {
        var isShowYesNoDialogCalled = false
        isPastePerformed = false

        every { mockedFileExplorerView.isCut } returns AtomicBoolean(true)

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        // source to paste
        val mockedSourceFile = mockk<VirtualFile>()
        val mockedSourceAttributes = null
        every { mockedSourceFile.name } returns "test_local_file"
        every { mockedSourceFile.parent } returns null
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()

        // Clipboard buffer
        val mockedClipboardBufferLocal = listOf(mockedSourceFile)
        every { mockedCopyPasterProvider.getSourceFilesFromClipboard() } returns mockedClipboardBufferLocal

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        every { childDestinationVirtualFile.name } returns "test_mf_file"
        val childDestMFFileAttr = mockk<RemoteUssAttributes>()

        // target to paste
        val mockedTargetFile = mockk<MFVirtualFile>()
        val mockedTargetFileAttributes = mockk<RemoteUssAttributes>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        every { mockedTargetFile.name } returns "test_mf_folder"
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns emptyList()

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any<List<VirtualFile>>(), any<List<VirtualFile>>(), any<Boolean>()
          )
        } returns mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestMFFileAttr
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns mockedTargetFileAttributes

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform operation throws error") {
        var isShowYesNoDialogCalled = false
        exceptionIsThrown = false

        val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
        mockkStatic(showYesNoDialogMock as KFunction<*>)
        every {
          showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
        } answers {
          isShowYesNoDialogCalled = true
          true
        }

        dataOpsManagerService = service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalStateException("Test Error")
          }
        }
        mockkObject(dataOpsManagerService.testInstance)

        // source to paste
        val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
        val mockedSourceNode = mockk<UssFileNode>()
        val mockedSourceFile = mockk<MFVirtualFile>()
        val mockedSourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedSourceFile.name } returns "test5"
        every { mockedSourceAttributes.isPastePossible } returns false
        every { mockedSourceAttributes.isDirectory } returns false
        every { mockedSourceNodeData.node } returns mockedSourceNode
        every { mockedSourceNodeData.file } returns mockedSourceFile
        every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

        // children of target
        val childDestinationVirtualFile = mockk<MFVirtualFile>()
        val childDestFileAttributes = mockk<RemoteUssAttributes>()
        every { childDestinationVirtualFile.name } returns "test_file"
        every { childDestFileAttributes.isDirectory } returns false

        // target to paste
        val mockedTargetFile = mockk<MFVirtualFile>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        val targetAttributes = mockk<RemoteUssAttributes>()
        every { mockedTargetFile.name } returns "test_folder"
        every { targetAttributes.isDirectory } returns true
        every { mockedTargetFile.children } returns arrayOf(childDestinationVirtualFile)
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile

        // CopyPaste node buffer
        val copyPasteNodeData = mockk<NodeData<*>>()
        val copyPasteNodeDataList = listOf(copyPasteNodeData)
        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { copyPasteNodeData.node } returns mockedSourceNode
        every { copyPasteNodeData.file } returns mockedSourceFile
        every { copyPasteNodeData.attributes } returns mockedSourceAttributes
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes
        every {
          dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
        } returns DefaultNameResolver()

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        // selected node data ( always remote files )
        every { mockedFileExplorerView.mySelectedNodesData } returns listOf(mockedSourceNodeData)

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any<List<VirtualFile>>(), any<List<VirtualFile>>(), any<Boolean>()
          )
        } returns mutableListOf(Pair(mockedTargetFile, mockedSourceFile))

        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isShowYesNoDialogCalled shouldBe true
          exceptionIsThrown shouldBe true
        }
      }

      should("perform operation throws error and not D&D") {
        exceptionIsThrown = false
        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns null
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          exceptionIsThrown shouldBe true
        }
      }

      should("return if project is null") {
        isPastePerformed = true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } answers {
          isPastePerformed = false
          null
        }
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe false
        }
      }

      should("return if copy paste provider is null") {
        isPastePerformed = true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { FileExplorerContentProvider.getInstance().getExplorerView(any() as Project) } answers {
          isPastePerformed = false
          null
        }
        mockedExplorerPasteProvider.performPaste(mockedDataContext)
        assertSoftly {
          isPastePerformed shouldBe false
        }
      }

      should("filter operations with files that are currently being synchronized") {
        var filesToMoveTotal = 1

        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns null
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every {
          FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
        } returns mockedFileExplorerView
        every { mockedFileExplorerView.copyPasteSupport } returns mockedCopyPasterProvider
        every { checkFileForSync(any(), any(), any()) } returns true

        every { mockedExplorerPasteProvider["runMoveOrCopyTask"](
          any<String>(),
          any<Int>(),
          any<Boolean>(),
          any<List<MoveCopyOperation>>(),
          any<FileExplorerView.ExplorerCopyPasteSupport>(),
          any<FileExplorerView>(),
          any<Project>()
        ) } answers {
          filesToMoveTotal = secondArg<Int>()
          this
        }

        mockedExplorerPasteProvider.performPaste(mockedDataContext)

        clearMocks(mockedExplorerPasteProvider)

        assertSoftly {
          filesToMoveTotal shouldBe 0
        }
      }

      should("paste is not enabled and possible if project is null") {
        var isPasteEnabled = true
        var isPastePossible = true
        every {
          FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
        } returns mockedFileExplorerView
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } answers {
          isPasteEnabled = false
          isPastePossible = false
          null
        }
        mockedExplorerPasteProvider.isPasteEnabled(mockedDataContext)
        mockedExplorerPasteProvider.isPastePossible(mockedDataContext)
        assertSoftly {
          isPasteEnabled shouldBe false
          isPastePossible shouldBe false
        }
      }

      should("paste is not enabled and possible if current explorer view is JES explorer") {
        var isPasteEnabled = true
        var isPastePossible = true
        val jesExplorerView = mockk<JesExplorerView>()
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { FileExplorerContentProvider.getInstance().getExplorerView(any() as Project) } answers {
          isPasteEnabled = false
          isPastePossible = false
          jesExplorerView
        }
        mockedExplorerPasteProvider.isPasteEnabled(mockedDataContext)
        mockedExplorerPasteProvider.isPastePossible(mockedDataContext)
        assertSoftly {
          isPasteEnabled shouldBe false
          isPastePossible shouldBe false
        }
      }

      should("paste is enabled and possible if destination files are selected nodes data") {
        var isPasteEnabled = false
        var isPastePossible = false
        every { mockedFileExplorerView.copyPasteSupport.isPastePossibleAndEnabled(any() as List<VirtualFile>) } answers {
          isPasteEnabled = true
          isPastePossible = true
          true
        }
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns null
        every {
          FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
        } returns mockedFileExplorerView
        mockedExplorerPasteProvider.isPasteEnabled(mockedDataContext)
        mockedExplorerPasteProvider.isPastePossible(mockedDataContext)
        assertSoftly {
          isPasteEnabled shouldBe true
          isPastePossible shouldBe true
        }
      }

      should("paste is enabled and possible") {
        var isPasteEnabled = false
        var isPastePossible = false
        every { mockedFileExplorerView.copyPasteSupport.isPastePossibleAndEnabled(any() as List<VirtualFile>) } answers {
          isPasteEnabled = true
          isPastePossible = true
          true
        }
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every {
          FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
        } returns mockedFileExplorerView
        mockedExplorerPasteProvider.isPasteEnabled(mockedDataContext)
        mockedExplorerPasteProvider.isPastePossible(mockedDataContext)
        assertSoftly {
          isPasteEnabled shouldBe true
          isPastePossible shouldBe true
        }
      }

      should("optimizeOperation remote source") {
        val mockedSourceDir = mockk<MFVirtualFile>()
        every { mockedSourceDir.name } returns "dir5"
        every { mockedSourceDir.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceDir.parent } returns null
        val mockedSourceFile = mockk<MFVirtualFile>()
        every { mockedSourceFile.name } returns "test5"
        every { mockedSourceFile.fileSystem } returns mockk<MFVirtualFileSystem>()
        every { mockedSourceFile.parent } returns mockedSourceDir
        val sourceFilesRaw = listOf(mockedSourceDir, mockedSourceFile)
        mockedExplorerPasteProvider::class.declaredMemberFunctions.find { it.name == "optimizeOperation" }?.let {
          it.isAccessible = true
          val ret: List<VirtualFile> = it.call(mockedExplorerPasteProvider, sourceFilesRaw) as List<VirtualFile>
          ret.size shouldBe 1
          ret[0].name shouldBe "dir5"
        }
      }

      should("optimizeOperation local source") {
        val mockedSourceDir = mockk<VirtualFile>()
        every { mockedSourceDir.name } returns "dir5"
        every { mockedSourceDir.fileSystem } returns mockk<LocalFileSystem>()
        val mockedSourceFile = mockk<VirtualFile>()
        every { mockedSourceFile.name } returns "test5"
        every { mockedSourceFile.fileSystem } returns mockk<LocalFileSystem>()
        val sourceFilesRaw = listOf(mockedSourceDir, mockedSourceFile)
        mockedExplorerPasteProvider::class.declaredMemberFunctions.find { it.name == "optimizeOperation" }?.let {
          it.isAccessible = true
          val ret: List<VirtualFile> = it.call(mockedExplorerPasteProvider, sourceFilesRaw) as List<VirtualFile>
          ret.size shouldBe 2
        }
      }

      context("Test config resolution") {
        isPastePerformed = false
        every { mockedFileExplorerView.isCut } returns AtomicBoolean(false)

        val copyPasteNodeDataList = mutableListOf<NodeData<*>>()
        val destinationChildFiles = mutableListOf<MFVirtualFile>()

        val mockedTargetFile = mockk<MFVirtualFile>()
        val targetAttributes = mockk<RemoteUssAttributes>()

        afterEach {
          copyPasteNodeDataList.clear()
          destinationChildFiles.clear()
//          clearMocks(dataOpsManagerService.testInstance)
          every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes
        }

        // prepare base mocks
//        val mockedDataOpsManager = mockk<DataOpsManager>()
//        every { dataOpsManagerService.testInstance } returns mockedDataOpsManager
        every {
          FileExplorerContentProvider.getInstance().getExplorerView(any() as Project)
        } returns mockedFileExplorerView
        every { mockedFileExplorerView.copyPasteSupport } returns mockedCopyPasterProvider
        every { mockedCopyPasterProvider.getSourceFilesFromClipboard() } returns emptyList()

        // children of target
        fun addMockedTargetChildFile(fileName: String, isDirectory: Boolean = false) {
          val childDestinationVirtualFile = mockk<MFVirtualFile>()
          val childDestFileAttributes = mockk<RemoteUssAttributes>()

          every { childDestinationVirtualFile.isDirectory } returns isDirectory
          every { childDestinationVirtualFile.name } returns fileName
          every { childDestinationVirtualFile.delete(any()) } just Runs
          every { childDestFileAttributes.isDirectory } returns isDirectory
          every { dataOpsManagerService.testInstance.tryToGetAttributes(childDestinationVirtualFile) } returns childDestFileAttributes
          destinationChildFiles.add(childDestinationVirtualFile)
        }

        // target to paste

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        every { mockedTargetFile.name } returns "test_folder"
        every { targetAttributes.isDirectory } returns true
        every { mockedTargetFile.children } answers { destinationChildFiles.toTypedArray() }
        every {
          mockedTargetFile.findChild(any() as String)
        } answers { mockedTargetFile.children?.find { it.name == firstArg() } }
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedNodeTarget.virtualFile } returns mockedTargetFile
        every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributes


        // CopyPaste node buffer

        fun addMockedSourceFile(
          fileName: String,
          isPastePossible: Boolean = false,
          isDirectory: Boolean = false,
          parent: MFVirtualFile? = null,
          sourceAttributes: FileAttributes? = null
        ): MFVirtualFile {
          val mockedSourceFile = mockk<MFVirtualFile>()
          every { mockedSourceFile.name } returns fileName
          every { mockedSourceFile.isDirectory } returns isDirectory
          every { mockedSourceFile.parent } returns parent
          every { mockedSourceFile.isInLocalFileSystem } returns false

          val mockedSourceAttributes = if (sourceAttributes != null) {
            sourceAttributes
          } else {
            val attributes = mockk<RemoteUssAttributes>()
            every { attributes.isPastePossible } returns isPastePossible
            every { attributes.isDirectory } returns isDirectory
            attributes
          }

          val mockedSourceNodeData = mockk<NodeData<ConnectionConfig>>()
          val mockedSourceNode = mockk<UssFileNode>()
          every { mockedSourceNodeData.node } returns mockedSourceNode
          every { mockedSourceNodeData.file } returns mockedSourceFile
          every { mockedSourceNodeData.attributes } returns mockedSourceAttributes

          val copyPasteNodeData = mockk<NodeData<ConnectionConfig>>()
          every { copyPasteNodeData.node } returns mockedSourceNode
          every { copyPasteNodeData.file } returns mockedSourceFile
          every { copyPasteNodeData.attributes } returns mockedSourceAttributes
          every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedSourceFile) } returns mockedSourceAttributes

          copyPasteNodeDataList.add(copyPasteNodeData)
          return mockedSourceFile
        }

        val mockedCopyPasteBuffer = LinkedList(copyPasteNodeDataList)
        every { mockedCopyPasterProvider.copyPasteBuffer } returns mockedCopyPasteBuffer

        // removeFromBuffer callback
        every { mockedCopyPasterProvider.removeFromBuffer(any() as ((NodeData<*>) -> Boolean)) } answers {
          isCallbackCalled = true
          copyPasteNodeDataList.forEach {
            firstArg<((NodeData<*>) -> Boolean)>().invoke(it)
          }
        }


        every { mockedDataContext.getData(IS_DRAG_AND_DROP_KEY) } returns true
        every { mockedDataContext.getData(CommonDataKeys.PROJECT) } returns mockedProject
        every { mockedDataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns arrayOf(mockedTargetFile)
        every { mockedDataContext.getData(DRAGGED_FROM_PROJECT_FILES_ARRAY) } returns emptyList()

        // selected node data ( always remote files )
        every {
          mockedFileExplorerView.mySelectedNodesData
        } answers { copyPasteNodeDataList.mapNotNull { it.castOrNull() } }

        every {
          mockedCopyPasterProvider.getDestinationSourceFilePairs(
            any() as List<VirtualFile>,
            any() as List<VirtualFile>,
            any() as Boolean
          )
        } answers { copyPasteNodeDataList.mapNotNull { nodeData -> nodeData.file?.let { Pair(mockedTargetFile, it) } } }

        should("Skip 2 files one by one") {

          addMockedSourceFile("file.txt")
          addMockedSourceFile("file1.txt")
          addMockedTargetChildFile("file.txt")
          addMockedTargetChildFile("file1.txt")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DefaultNameResolver()
          mockkStatic(Messages::class)
          var skipNumber = 0

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            2
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any<String>(), any<String>(), any<String>(), any<String>(), any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            ++skipNumber
            0
          }
          mockedExplorerPasteProvider.performPaste(mockedDataContext)
          assertSoftly {
            skipNumber shouldBe 2
          }
        }
        should("Skip 1 file and 1 overwrite") {
          isPastePerformed = false
          addMockedSourceFile("file.txt")
          addMockedSourceFile("file1.txt")
          addMockedTargetChildFile("file.txt")
          addMockedTargetChildFile("file1.txt")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DefaultNameResolver()

          mockkStatic(Messages::class)
          var overwriteSelected = false
          var skipSelected = false

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            2
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (!overwriteSelected) {
              overwriteSelected = true
              1
            } else {
              skipSelected = true
              0
            }
          }

          var fileOverwritten = false
          var file1Skipped = true
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            if (op.source.name == "file.txt") {
              fileOverwritten = true
            }
            if (op.source.name == "file1.txt") {
              file1Skipped = false
            }
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            fileOverwritten shouldBe true
            file1Skipped shouldBe true
            skipSelected shouldBe true
          }
        }
        should("Use new name for 1 file and 1 overwrite") {
          isPastePerformed = false
          addMockedSourceFile("file.txt")
          addMockedSourceFile("file1.txt")
          addMockedTargetChildFile("file.txt")
          addMockedTargetChildFile("file_(1).txt")
          addMockedTargetChildFile("file1.txt")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DefaultNameResolver()

          mockkStatic(Messages::class)
          var decideOptionSelected = false
          var overwriteSelected = false
          var useNewNameSelected = false

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            2
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (!useNewNameSelected) {
              useNewNameSelected = true
              2
            } else {
              overwriteSelected = true
              1
            }
          }

          var fileNewName: String? = null
          var file1Overwritten = false
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            if (op.source.name == "file.txt") {
              fileNewName = op.newName
            }
            if (op.source.name == "file1.txt") {
              file1Overwritten = true
            }
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            fileNewName shouldBe "file_(2).txt"
            file1Overwritten shouldBe true
            overwriteSelected shouldBe true
          }
        }
        should("Use new name for dataset or directory") {
          isPastePerformed = false
          val datasetAttributes = mockk<RemoteDatasetAttributes>()
          every { datasetAttributes.isPastePossible } returns false
          every { datasetAttributes.isDirectory } returns false

          addMockedSourceFile("directory.test", isDirectory = true)
          addMockedSourceFile("DATASET.TEST", sourceAttributes = datasetAttributes)
          addMockedTargetChildFile("directory.test", true)
          addMockedTargetChildFile("DATASET.TEST")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DatasetOrDirResolver(dataOpsManagerService)
          mockkStatic(Messages::class)
          var decideOptionSelected = false

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (!decideOptionSelected) {
              decideOptionSelected = true
              2
            } else {
              2
            }
          }

          var dirNewName: String? = null
          var datasetNewName: String? = null
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            if (op.source.name == "directory.test") {
              dirNewName = op.newName
            }
            if (op.source.name == "DATASET.TEST") {
              datasetNewName = op.newName
            }
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            dirNewName shouldBe "directory.test_(1)"
            datasetNewName shouldBe "DATASET.TEST_(1)"
          }
        }
        should("Use new name for sequential dataset (moving to pds)") {

          isPastePerformed = false
          val datasetAttributes = mockk<RemoteDatasetAttributes>()
          every { datasetAttributes.name } returns "DATASET.TEST"
          every { datasetAttributes.isDirectory } returns false
          every { datasetAttributes.isPastePossible } returns false
          val targetAttributesPDS = mockk<RemoteDatasetAttributes>()

          addMockedSourceFile("DATASET.TEST", sourceAttributes = datasetAttributes)
          addMockedTargetChildFile("TEST")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns SeqToPDSResolver(dataOpsManagerService)
          every { dataOpsManagerService.testInstance.tryToGetAttributes(mockedTargetFile) } returns targetAttributesPDS

          mockkStatic(Messages::class)
          var decideOptionSelected = false

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showOkCancelDialog(
              any() as String,
              any() as String,
              any() as String,
              any() as String,
              any() as Icon?,
            )
          } answers {
            if (!decideOptionSelected) {
              decideOptionSelected = true
              Messages.OK
            } else {
              Messages.CANCEL
            }
          }

          var memberNewName: String? = null
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            memberNewName = op.newName
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            memberNewName shouldBe "TEST1"
          }
        }
        should("Resolve conflicts between directory and file") {
          isPastePerformed = false
          addMockedSourceFile("dir1")
          addMockedSourceFile("dir2", isPastePossible = true, isDirectory = true)
          addMockedTargetChildFile("dir1", true)
          addMockedTargetChildFile("dir2")
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DefaultNameResolver()

          mockkStatic(Messages::class)

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            2
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            1
          }

          var dir1NewName: String? = null
          var dir2NewName: String? = null
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            if (op.source.name == "dir1") {
              dir1NewName = op.newName
            }
            if (op.source.name == "dir2") {
              dir2NewName = op.newName
            }
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            dir1NewName shouldBe "dir1_(1)"
            dir2NewName shouldBe "dir2_(1)"
          }
        }

        should("Resolve conflicts for copying to same directory (Skip all)") {
          isPastePerformed = false
          val sourceFile = addMockedSourceFile("file.txt", parent = mockedTargetFile)
          destinationChildFiles.add(sourceFile)

          mockkStatic(Messages::class)
          var skipSelected = false

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (!skipSelected) {
              skipSelected = true
            }
            0
          }

          var operationPerformed = false
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            operationPerformed = true
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            skipSelected shouldBe true
            operationPerformed shouldBe false
          }
        }

        should("Resolve conflicts for copying to same directory (Overwrite all)") {
          isPastePerformed = false
          val sourceFile = addMockedSourceFile("file.txt", parent = mockedTargetFile)
          destinationChildFiles.add(sourceFile)
          mockkStatic(Messages::class)
          var overwriteSelected = false
          var notPossibleMessageShown = false

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            overwriteSelected = true
            1
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES

          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (thirdArg<String>() == "Not Resolvable Conflicts") {
              notPossibleMessageShown = true
            }
            0
          }

          var operationPerformed = false
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            operationPerformed = true
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            overwriteSelected shouldBe true
            notPossibleMessageShown shouldBe true
            operationPerformed shouldBe false
          }
        }

        should("Resolve conflicts for copying to same directory (Decide for each)") {
          isPastePerformed = false
          val sourceFile1 = addMockedSourceFile("file1.txt", parent = mockedTargetFile)
          destinationChildFiles.add(sourceFile1)
          val sourceFile2 = addMockedSourceFile("file2.txt", parent = mockedTargetFile)
          destinationChildFiles.add(sourceFile2)
          every {
            dataOpsManagerService.testInstance.getNameResolver(any() as VirtualFile, any() as VirtualFile)
          } returns DefaultNameResolver()
          mockkStatic(Messages::class)

          mockkObject(WindowsLikeMessageDialog.Companion)
          every {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              any(),
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
          } answers {
            2
          }

          every {
            Messages.showYesNoDialog(
              any() as Project?, any() as String, any() as String, any() as String, any() as String, any() as Icon?
            )
          } returns Messages.YES
          every {
            Messages.showDialog(
              any() as Project?,
              any() as String,
              any() as String,
              any() as Array<String>,
              0,
              any() as Icon?,
              null
            )
          } answers {
            if (secondArg<String>().contains(sourceFile1.name)) {
              1
            } else if (secondArg<String>().contains(sourceFile2.name)) {
              0
            } else {
              throw IllegalArgumentException("Unknown dialog called, that should not be shown.")
            }
          }

          var file1CopiedWithNewName = false
          var file2Copied = false
          every {
            dataOpsManagerService.testInstance.performOperation(any() as MoveCopyOperation, any() as ProgressIndicator)
          } answers {
            val op = firstArg<MoveCopyOperation>()
            if (op.source.name == sourceFile1.name && op.newName == "file1_(1).txt") {
              file1CopiedWithNewName = true
            }
            if (op.source.name == sourceFile2.name) {
              file2Copied = true
            }
          }

          mockedExplorerPasteProvider.performPaste(mockedDataContext)

          assertSoftly {
            file1CopiedWithNewName shouldBe true
            file2Copied shouldBe false
          }
        }
      }
    }
  }

})
