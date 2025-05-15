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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.vfs.MFVirtualFile
import io.mockk.*
import org.zowe.explorer.dataops.operations.UssChangeModeOperation
import org.zowe.explorer.dataops.operations.UssChangeOwnerOperation
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.changeFileEncodingAction
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.kotlinsdk.*
import java.nio.charset.Charset

class GetFilePropertiesActionTestSpec : AppInitShouldSpec("explorer/actions/GetFilePropertiesAction", {
  context("all functions") {
    var didProduceErrorNotification = false

    val mockConnectionConfig = mockk<ConnectionConfig> {
      every { uuid } returns "uuid"
    }
    val fileUnit = mockk<ExplorerUnit<ConnectionConfig>> {
      every { connectionConfig } returns mockConnectionConfig
    }
    val mockProject = mockk<Project>()
    val fileView = mockk<FileExplorerView>(relaxed = true)
    val getPropertiesEvent = mockk<AnActionEvent>(relaxed = true) {
      every { project } returns mockProject
      every { getData(EXPLORER_VIEW) } returns fileView
    }

    val dataOpsManager = DataOpsManager.getService()

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      didProduceErrorNotification = true
    }

    val genericDatasetMock = mockk<Dataset> {
      every { name } returns "name"
      every { migrated } returns HasMigrated.NO
      every { datasetOrganization } returns DatasetOrganization.PO
      every { volumeSerial } returns "serial"
      every { dsnameType } returns DsnameType.PDS
      every { catalogName } returns "catalog"
      every { volumeSerials } returns "vol"
      every { deviceType } returns "devtype"
      every { recordFormat } returns RecordFormat.FB
      every { recordLength } returns 80
      every { blockSize } returns 80
      every { sizeInTracks } returns 80
      every { spaceUnits } returns SpaceUnits.TRACKS
      every { usedTracksOrBlocks } returns 80
      every { extendsUsed } returns 80
      every { creationDate } returns "0001-01-01"
      every { lastReferenceDate } returns "0001-01-01"
      every { expirationDate } returns "0001-01-01"
      every { spaceOverflowIndicator } returns "ind"
    }

    beforeEach {
      didProduceErrorNotification = false

      every { dataOpsManager.getContentSynchronizer(any<VirtualFile>()) } returns mockk()
      every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk<FileAttributes>()
      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
      } throws NotImplementedError("Unexpected call of the 'performOperation'")

      every { getPropertiesEvent.getData(EXPLORER_VIEW) } returns fileView
      every { fileUnit.connectionConfig } returns mockConnectionConfig
    }

    context("actionPerformed") {
      val mockExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSet>> {
        every { componentManager } returns ApplicationManager.getApplication()
      }

      context("common") {
        beforeEach {
          mockkConstructor(DatasetPropertiesDialog::class)
          every { fileView.mySelectedNodesData } returns listOf()
        }

        should("not show the dialog when explorer is null") {
          every { getPropertiesEvent.getData(EXPLORER_VIEW) } returns null

          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)

          verify(exactly = 0) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("not show the dialog when there is no selected nodes") {
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)

          verify(exactly = 0) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("not show the dialog when connection config is null") {
          val mockVirtualFile = mockk<MFVirtualFile>()
          val fileLikeDsNode = mockk<FileLikeDatasetNode> {
            every { virtualFile } returns mockVirtualFile
            every { unit } returns fileUnit
            every { explorer } returns mockExplorer
          }
          val nodeData = spyk(NodeData(fileLikeDsNode, mockVirtualFile, null))
          every { fileUnit.connectionConfig } returns null
          every { fileView.mySelectedNodesData } returns listOf(nodeData)

          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)

          verify(exactly = 0) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }
      }

      context("Dataset attributes") {
        val mockVirtualFile = mockk<MFVirtualFile>()
        val fileLikeDsNode = mockk<FileLikeDatasetNode> {
          every { virtualFile } returns mockVirtualFile
          every { unit } returns fileUnit
          every { explorer } returns mockExplorer
        }
        val nodeData = spyk(NodeData(fileLikeDsNode, mockVirtualFile, null))
        val queryMask = spyk(DSMask("mask", mutableListOf("excl_name")))
        val attributes = spyk(
          RemoteDatasetAttributes(
            genericDatasetMock,
            "test",
            mutableListOf(MaskedRequester(mockConnectionConfig, queryMask))
          )
        )

        mockkConstructor(DatasetPropertiesDialog::class)

        beforeEach {
          every { anyConstructed<DatasetPropertiesDialog>().showAndGet() } returns true

          every { fileView.mySelectedNodesData } returns listOf(nodeData)

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          clearMocks(fileLikeDsNode, answers = false, childMocks = false)
          clearConstructorMockk(DatasetPropertiesDialog::class, answers = false, childMocks = false)
        }

        should("not open the properties dialog when attributes are not of RemoteDatasetAttributes, RemoteUssAttributes or RemoteMemberAttributes types") {
          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk<FileAttributes>()

          // Simulate the action
          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          // Verify node.fetchAttributesForNodeIfMissing() is NOT called
          verify(exactly = 0) { fileLikeDsNode.fetchAttributesForNodeIfMissing(any(), any(), any(), any()) }
          verify(exactly = 0) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("just open the properties dialog when the node is FileLikeDatasetNode and fetch is not needed") {
          every {
            fileLikeDsNode
              .fetchAttributesForNodeIfMissing(
                attributes,
                dataOpsManager,
                any<() -> Unit>(),
                any<() -> Unit>()
              )
          } answers {
            runInEdtAndWait {
              thirdArg<() -> Unit>()()
            }
          }

          // Simulate the action
          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) {
            fileLikeDsNode.fetchAttributesForNodeIfMissing(
              initAttributes = attributes,
              dataOpsManager = dataOpsManager,
              funcOnNotNeeded = any<() -> Unit>(),
              funcOnComplete = any<() -> Unit>()
            )
          }
          verify(exactly = 1) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("fetch attributes for a node when the node is FileLikeDatasetNode and fetch is needed") {
          every {
            fileLikeDsNode
              .fetchAttributesForNodeIfMissing(
                attributes,
                dataOpsManager,
                any<() -> Unit>(),
                any<() -> Unit>()
              )
          } answers {
            runInEdtAndWait {
              arg<() -> Unit>(3)()
            }
          }

          // Simulate the action
          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) {
            fileLikeDsNode.fetchAttributesForNodeIfMissing(
              initAttributes = attributes,
              dataOpsManager = dataOpsManager,
              funcOnNotNeeded = any<() -> Unit>(),
              funcOnComplete = any<() -> Unit>()
            )
          }
          verify(exactly = 1) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("not fetch attributes for a node when the node is FileLikeDatasetNode and a content synchronizer is null") {
          every { dataOpsManager.getContentSynchronizer(any<VirtualFile>()) } returns null

          every {
            fileLikeDsNode
              .fetchAttributesForNodeIfMissing(
                attributes,
                dataOpsManager,
                any<() -> Unit>(),
                any<() -> Unit>()
              )
          } answers {
            runInEdtAndWait {
              arg<() -> Unit>(3)()
            }
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 0) {
            fileLikeDsNode.fetchAttributesForNodeIfMissing(
              initAttributes = attributes,
              dataOpsManager = dataOpsManager,
              funcOnNotNeeded = any<() -> Unit>(),
              funcOnComplete = any<() -> Unit>()
            )
          }
          verify(exactly = 1) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("not fetch attributes for a node when the node is LibraryNode and a content synchronizer is null") {
          val libraryNode = mockk<LibraryNode> {
            every { virtualFile } returns mockVirtualFile
            every { unit } returns mockk<FilesWorkingSet> {
              every { connectionConfig } returns mockConnectionConfig
            }
            every { explorer } returns mockExplorer
          }
          val libraryNodeData = spyk(NodeData(libraryNode, mockVirtualFile, null))

          every { dataOpsManager.getContentSynchronizer(any<VirtualFile>()) } returns null

          every {
            fileLikeDsNode
              .fetchAttributesForNodeIfMissing(
                attributes,
                dataOpsManager,
                any<() -> Unit>(),
                any<() -> Unit>()
              )
          } answers {
            runInEdtAndWait {
              arg<() -> Unit>(3)()
            }
          }
          every { fileView.mySelectedNodesData } returns listOf(libraryNodeData)

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 0) {
            fileLikeDsNode.fetchAttributesForNodeIfMissing(
              initAttributes = attributes,
              dataOpsManager = dataOpsManager,
              funcOnNotNeeded = any<() -> Unit>(),
              funcOnComplete = any<() -> Unit>()
            )
          }
          verify(exactly = 1) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }
      }

      context("Member attributes") {
        val mockVirtualFile = mockk<MFVirtualFile>()
        val fileLikeDsNode = mockk<FileLikeDatasetNode> {
          every { virtualFile } returns mockVirtualFile
          every { explorer } returns mockExplorer
          every { unit } returns fileUnit
        }
        val nodeData = spyk(NodeData(fileLikeDsNode, mockVirtualFile, null))

        every { fileView.mySelectedNodesData } returns listOf(nodeData)

        mockkConstructor(MemberPropertiesDialog::class)
        every { anyConstructed<MemberPropertiesDialog>().showAndGet() } returns true

        should("get member properties") {
          val member = Member()
          val fileAttr =
            spyk(RemoteMemberAttributes(member, mockVirtualFile, XIBMDataType(XIBMDataType.Type.TEXT)))

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns fileAttr

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)

            verify(exactly = 1) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
          }
        }
      }

      context("USS file attributes") {
        var didChangeEncoding = false

        var mockVirtualFile: MFVirtualFile = mockk<MFVirtualFile>()
        val ussFileNode = mockk<UssFileNode> {
          every { virtualFile } returns mockVirtualFile
          every { explorer } returns mockExplorer
          every { unit } returns fileUnit
          every { parent } returns mockk()
        }
        var nodeData = spyk(NodeData(ussFileNode, mockVirtualFile, null))
        val initFileMode = mockk<FileMode> {
          every { owner } returns 1
          every { group } returns 1
          every { all } returns 1
        }
        val ussFile = mockk<UssFile> {
          every { name } returns "name"
          every { isDirectory } returns false
          every { fileMode } returns initFileMode
          every { size } returns 0L
          every { uid } returns 0L
          every { gid } returns 0L
          every { user } returns "User"
          every { groupId } returns "Group"
          every { modificationTime } returns "Time"
          every { target } returns "Target"
        }

        every { fileView.mySelectedNodesData } returns listOf(nodeData)

        mockkConstructor(ChangeEncodingDialog::class)
        every { anyConstructed<ChangeEncodingDialog>().show() } returns Unit

        mockkStatic(::changeFileEncodingAction)

        beforeEach {
          didChangeEncoding = false

          mockVirtualFile = mockk<MFVirtualFile>()

          every { ussFileNode.virtualFile } returns mockVirtualFile
          every { ussFileNode.parent } returns mockk()

          nodeData = spyk(NodeData(ussFileNode, mockVirtualFile, null))

          mockkConstructor(UssFilePropertiesDialog::class)
          every { anyConstructed<UssFilePropertiesDialog>().showAndGet() } returns true

          every {
            changeFileEncodingAction(any(), any(), any(), any())
          } answers {
            didChangeEncoding = true
            true
          }
        }

        should("change file owner") {
          var didChangeOwner = false

          val oldOwner = "TESTOLD"
          val newOwner = "TESTNEW"
          val testUssFile = UssFile(
            name = "name",
            user = oldOwner
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }

          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.owner = newOwner
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<UssChangeOwnerOperation>(), any<ProgressIndicator>())
          } answers {
            val operation = firstArg<UssChangeOwnerOperation>()
            if (operation.request.parameters.owner == newOwner) {
              didChangeOwner = true
            }
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeOwner shouldBe true }
          assertSoftly { attributes.owner shouldBe newOwner }
        }

        should("produce an error notification when attempting to change owner") {
          val oldOwner = "TESTOLD"
          val newOwner = "TESTNEW"
          val testUssFile = UssFile(
            name = "name",
            user = oldOwner
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }

          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.owner = newOwner
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
          } throws Exception("Test exception")

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didProduceErrorNotification shouldBe true }
          assertSoftly { attributes.owner shouldBe oldOwner }
        }

        should("change file group ID") {
          val oldGroupId = "TESTGROUPOLD"
          val newGroupId = "TESTGROUPNEW"
          var didChangeGroupId = false

          val testUssFile = UssFile(
            name = "name",
            groupId = oldGroupId
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }

          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.groupId = newGroupId
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<UssChangeOwnerOperation>(), any<ProgressIndicator>())
          } answers {
            val operation = firstArg<UssChangeOwnerOperation>()
            if (operation.request.parameters.group == newGroupId) {
              didChangeGroupId = true
            }
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeGroupId shouldBe true }
          assertSoftly { attributes.groupId shouldBe newGroupId }
        }

        should("produce an error notification when attempting to change group ID") {
          val oldGroupId = "TESTGROUPOLD"
          val newGroupId = "TESTGROUPNEW"
          val testUssFile = UssFile(
            name = "name",
            groupId = oldGroupId
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }

          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.groupId = newGroupId
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
          } throws Exception("Test exception")

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didProduceErrorNotification shouldBe true }
          assertSoftly { attributes.groupId shouldBe oldGroupId }
        }

        should("change file mode") {
          val newFileMode = FileMode(owner = 7, group = 7, all = 7)
          var didChangeFileMode = false
          var didCleanCache = false

          val testUssFile = UssFile(
            name = "name",
            mode = "-r--r--r--"
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }
          val oldFileMode = attributes.fileMode?.clone()

          every {
            ussFileNode.parent
          } returns mockk<FileFetchNode<*, *, *, *, *, *>> {
            every {
              cleanCache(any<Boolean>(), any<Boolean>(), any<Boolean>(), any<Boolean>())
            } answers {
              didCleanCache = true
            }
          } as ExplorerTreeNode<ConnectionConfig, *>

          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.fileMode?.owner = newFileMode.owner
            attributes.fileMode?.group = newFileMode.group
            attributes.fileMode?.all = newFileMode.all
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<UssChangeModeOperation>(), any<ProgressIndicator>())
          } answers {
            val operation = firstArg<UssChangeModeOperation>()
            if (
              operation.request.parameters.mode.owner == newFileMode.owner
              && operation.request.parameters.mode.group == newFileMode.group
              && operation.request.parameters.mode.all == newFileMode.all
            ) {
              didChangeFileMode = true
            }
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeFileMode shouldBe true }
          assertSoftly { didCleanCache shouldBe true }
          assertSoftly { attributes.fileMode?.owner shouldBe newFileMode.owner }
          assertSoftly { attributes.fileMode?.group shouldBe newFileMode.group }
          assertSoftly { attributes.fileMode?.all shouldBe newFileMode.all }
          assertSoftly { attributes.fileMode?.owner shouldNotBe oldFileMode?.owner }
          assertSoftly { attributes.fileMode?.group shouldNotBe oldFileMode?.group }
          assertSoftly { attributes.fileMode?.all shouldNotBe oldFileMode?.all }
        }

        should("produce an error notification trying to change file mode") {
          val newFileMode = FileMode(owner = 7, group = 7, all = 7)

          val testUssFile = UssFile(
            name = "name",
            mode = "-r--r--r--"
          )
          val attributes = spyk(
            RemoteUssAttributes("rootPath", testUssFile, "url", mockConnectionConfig)
          ) {
            every { path } returns "path"
            every { charset } returns Charset.forName("IBM-1047")
          }
          val oldFileMode = attributes.fileMode?.clone()

          every { ussFileNode.parent } returns null
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.fileMode?.owner = newFileMode.owner
            attributes.fileMode?.group = newFileMode.group
            attributes.fileMode?.all = newFileMode.all
            true
          }
          every { mockVirtualFile.isDirectory } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes
          every {
            dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
          } throws Exception("Test exception")

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly {
            didProduceErrorNotification shouldBe true
            attributes.fileMode?.owner shouldNotBe newFileMode.owner
            attributes.fileMode?.group shouldNotBe newFileMode.group
            attributes.fileMode?.all shouldNotBe newFileMode.all
            attributes.fileMode?.owner shouldBe oldFileMode?.owner
            attributes.fileMode?.group shouldBe oldFileMode?.group
            attributes.fileMode?.all shouldBe oldFileMode?.all
          }
        }

        should("get USS file properties and do not change charset") {
          val oldCharset = Charset.forName("IBM-1047")
          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", mockConnectionConfig)) {
            every { path } returns "path"
          }
          attributes.charset = oldCharset

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          every { mockVirtualFile.isDirectory } returns false
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            true
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeEncoding shouldBe false }
          assertSoftly { attributes.charset shouldBe oldCharset }
        }

        should("get USS file properties and change charset") {
          val oldCharset = Charset.forName("IBM-1047")
          val newCharset = Charset.forName("IBM-500")
          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", mockConnectionConfig)) {
            every { path } returns "path"
          }
          attributes.charset = oldCharset

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          every { mockVirtualFile.isDirectory } returns false
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.charset = newCharset
            true
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeEncoding shouldBe true }
          assertSoftly { attributes.charset shouldBe newCharset }
        }

        should("get USS file properties try to change charset for directory") {
          val oldCharset = Charset.forName("IBM-1047")
          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", mockConnectionConfig)) {
            every { path } returns "path"
          }
          attributes.charset = oldCharset

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          every { mockVirtualFile.isDirectory } returns true
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            true
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeEncoding shouldBe false }
          assertSoftly { attributes.charset shouldBe oldCharset }
        }

        should("open and cancel a properties dialog trying to change charset") {
          val oldCharset = Charset.forName("IBM-1047")
          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", mockConnectionConfig)) {
            every { path } returns "path"
          }
          attributes.charset = oldCharset

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          every { mockVirtualFile.isDirectory } returns false
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            false
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { didChangeEncoding shouldBe false }
          assertSoftly { attributes.charset shouldBe oldCharset }
        }

        should("not change a charset in the properties dialog when user declined the change") {
          val oldCharset = Charset.forName("IBM-1047")
          val newCharset = Charset.forName("IBM-500")
          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", mockConnectionConfig)) {
            every { path } returns "path"
          }
          attributes.charset = oldCharset

          every { changeFileEncodingAction(any(), any(), any(), any()) } returns false

          every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns attributes

          every { mockVirtualFile.isDirectory } returns false
          every {
            anyConstructed<UssFilePropertiesDialog>().showAndGet()
          } answers {
            attributes.charset = newCharset
            true
          }

          runInEdtAndWait {
            GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          }

          verify(exactly = 1) { anyConstructed<UssFilePropertiesDialog>().showAndGet() }
          assertSoftly { attributes.charset shouldBe oldCharset }
        }
      }
    }

    context("update") {
      var testIsEnabled = true
      var testIsVisible = true

      val presentation = spyk(Presentation()) {
        every {
          setEnabled(any())
        } answers {
          testIsEnabled = firstArg()
        }
        every {
          setVisible(any())
        } answers {
          testIsVisible = firstArg()
        }
      }
      every { getPropertiesEvent.presentation } returns presentation

      beforeEach {
        testIsEnabled = true
        testIsVisible = true

        every { fileView.mySelectedNodesData } returns listOf()
      }

      should("not show 'Properties' option when the explorer view is null") {
        every { getPropertiesEvent.getData(EXPLORER_VIEW) } returns null

        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe false }
        assertSoftly { testIsVisible shouldBe false }
      }

      should("not show 'Properties' option when there is no selected node") {
        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe true }
        assertSoftly { testIsVisible shouldBe false }
      }

      should("show 'Properties' option for USS file node") {
        val ussFileNode = mockk<UssFileNode>()
        val mockNodeData = mockk<NodeData<ConnectionConfig>> {
          every { node } returns ussFileNode
        }
        every { fileView.mySelectedNodesData } returns listOf(mockNodeData)

        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe true }
        assertSoftly { testIsVisible shouldBe true }
      }

      should("show 'Properties' option for USS dir node") {
        val ussDirNode = mockk<UssDirNode>()
        val mockNodeData = mockk<NodeData<ConnectionConfig>> {
          every { node } returns ussDirNode
        }
        every { fileView.mySelectedNodesData } returns listOf(mockNodeData)

        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe true }
        assertSoftly { testIsVisible shouldBe true }
      }

      should("show enabled 'Properties' option for a non-migrated PS dataset node") {
        val datasetNode = mockk<FileLikeDatasetNode> {
          every { virtualFile } returns mockk()
        }
        val mockNodeData = mockk<NodeData<ConnectionConfig>> {
          every { node } returns datasetNode
        }
        every { fileView.mySelectedNodesData } returns listOf(mockNodeData)

        every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk<RemoteDatasetAttributes> {
          every { isMigrated } returns false
        }

        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe true }
        assertSoftly { testIsVisible shouldBe true }
      }

      should("show disabled 'Properties' option for a migrated PDS dataset node") {
        val datasetNode = mockk<LibraryNode> {
          every { virtualFile } returns mockk()
        }
        val mockNodeData = mockk<NodeData<ConnectionConfig>> {
          every { node } returns datasetNode
        }
        every { fileView.mySelectedNodesData } returns listOf(mockNodeData)

        every { dataOpsManager.tryToGetAttributes(any<VirtualFile>()) } returns mockk<RemoteDatasetAttributes> {
          every { isMigrated } returns true
        }

        GetFilePropertiesAction().update(getPropertiesEvent)

        assertSoftly { testIsEnabled shouldBe false }
        assertSoftly { testIsVisible shouldBe true }
      }
    }
  }
})
