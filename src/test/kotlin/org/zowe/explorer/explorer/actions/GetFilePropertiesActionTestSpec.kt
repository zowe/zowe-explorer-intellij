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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ExplorerUnit
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.utils.clone
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.explorer.utils.initialize
import org.zowe.explorer.utils.isBeingEditingNow
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.zowe.kotlinsdk.*
import java.nio.charset.Charset

class GetFilePropertiesActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    context("actionPerformed") {
      val getPropertiesEvent = mockk<AnActionEvent>(relaxed = true)
      val project = mockk<Project>()
      every { getPropertiesEvent.project } returns project
      val virtualFile = mockk<MFVirtualFile>()


      val fileView = mockk<FileExplorerView>(relaxed = true)
      val explorer = mockk<Explorer<ConnectionConfig, FilesWorkingSet>>()
      val fileUnit = mockk<ExplorerUnit<ConnectionConfig>>()
      every { getPropertiesEvent.getExplorerView<FileExplorerView>() } returns fileView

      val connectionConfig = mockk<ConnectionConfig>()
      every { connectionConfig.uuid } returns "uuid"

      context("Dataset attributes") {
        val dsNode = mockk<FileLikeDatasetNode>()
        every { dsNode.virtualFile } returns virtualFile
        val nodeData = spyk(NodeData(dsNode, virtualFile, null))

        every { fileView.mySelectedNodesData } returns listOf(nodeData)
        every { dsNode.unit } returns fileUnit
        every { fileUnit.connectionConfig } returns connectionConfig

        every { dsNode.explorer } returns explorer
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        val dataset = mockk<Dataset>()
        every { dataset.name } returns "name"
        every { dataset.migrated } returns HasMigrated.NO
        every { dataset.datasetOrganization } returns DatasetOrganization.PO
        every { dataset.volumeSerial } returns "serial"
        every { dataset.dsnameType } returns DsnameType.PDS
        every { dataset.catalogName } returns "catalog"
        every { dataset.volumeSerials } returns "vol"
        every { dataset.deviceType } returns "devtype"
        every { dataset.recordFormat } returns RecordFormat.FB
        every { dataset.recordLength } returns 80
        every { dataset.blockSize } returns 80
        every { dataset.sizeInTracks } returns 80
        every { dataset.spaceUnits } returns SpaceUnits.TRACKS
        every { dataset.usedTracksOrBlocks } returns 80
        every { dataset.extendsUsed } returns 80
        every { dataset.creationDate } returns "0001-01-01"
        every { dataset.lastReferenceDate } returns "0001-01-01"
        every { dataset.expirationDate } returns "0001-01-01"
        every { dataset.spaceOverflowIndicator } returns "ind"

        val excludes: MutableList<String> = ArrayList()
        excludes.add("excl_name")
        val queryMask = spyk(DSMask("mask", excludes))

        should("not call dialog.showAndGet() when attributes is not of RemoteDatasetAttributes,RemoteUssAttributes or RemoteMemberAttributes types") {

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return mockk<FileAttributes>()
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("USS file properties dialog should not be used.")
          }
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set member properties dialog should not be used.")
          }


          mockkConstructor(DatasetPropertiesDialog::class)
          every { anyConstructed<DatasetPropertiesDialog>().showAndGet() } returns true

          // Simulate the action
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)

          // Verify node.fetchAttributesForNodeIfMissing() is NOT called
          verify(exactly = 0) { dsNode.fetchAttributesForNodeIfMissing(any(), any(), any(), any()) }
          verify(exactly = 0) { anyConstructed<DatasetPropertiesDialog>().showAndGet() }
        }

        should("call node.fetchAttributesForNodeIfMissing() when node is FileLikeDatasetNode") {

          val attributes =
            spyk(RemoteDatasetAttributes(dataset, "test", mutableListOf(MaskedRequester(connectionConfig, queryMask))))
          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          val funcOnNotNeeded = slot<() -> Unit>()
          val funcOnComplete = slot<() -> Unit>()
          every {dsNode.fetchAttributesForNodeIfMissing(attributes,dataOpsManager,capture(funcOnNotNeeded),capture(funcOnComplete))} returns Unit

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("USS file properties dialog should not be used.")
          }
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set member properties dialog should not be used.")
          }

          mockkConstructor(DatasetPropertiesDialog::class)
          every { anyConstructed<DatasetPropertiesDialog>().showAndGet() } returns true

          // Simulate the action
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val newAttributes = dataOpsManager.tryToGetAttributes(virtualFile) as RemoteDatasetAttributes
          val dialog = DatasetPropertiesDialog(project, DatasetState(newAttributes))

          verify(exactly = 1) {
            dsNode.fetchAttributesForNodeIfMissing(
              initAttributes = attributes,
              dataOpsManager = dataOpsManager,
              funcOnNotNeeded = any(),
              funcOnComplete = any()
            )
          }
          assertNotNull(newAttributes, "newAttributes should not be null")
          assertTrue(dialog.showAndGet())
        }

      }

      context("Member attributes") {
        val dsNode = mockk<FileLikeDatasetNode>()
        every { dsNode.virtualFile } returns virtualFile
        val nodeData = spyk(NodeData(dsNode, virtualFile, null))

        every { fileView.mySelectedNodesData } returns listOf(nodeData)
        every { dsNode.explorer } returns explorer
        every { dsNode.unit } returns fileUnit
        every { dsNode.unit.connectionConfig } returns connectionConfig
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        val dataset = mockk<Dataset>()
        every { dataset.name } returns "name"

        should("get member properties") {
          val member = mockk<Member>()

          val fileAttr =
            spyk(RemoteMemberAttributes(member, virtualFile, XIBMDataType(XIBMDataType.Type.TEXT)))

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return fileAttr
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set properties dialog should not be used.")
          }
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("USS file properties dialog should not be used.")
          }
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit

          mockkConstructor(MemberPropertiesDialog::class)
          every { anyConstructed<MemberPropertiesDialog>().showAndGet() } returns true

          // Perform action:
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val dialog = MemberPropertiesDialog(project, MemberState(fileAttr))

          assertTrue(dialog.showAndGet())
        }

      }

      context("USS file attributes") {

        val ussFileNode = mockk<UssFileNode>()
        every { ussFileNode.virtualFile } returns virtualFile
        every {virtualFile.isDirectory} returns false
        val nodeData = spyk(NodeData(ussFileNode, virtualFile, null))

        every { fileView.mySelectedNodesData } returns listOf(nodeData)
        every { ussFileNode.explorer } returns explorer
        every { ussFileNode.unit } returns fileUnit
        every { ussFileNode.unit.connectionConfig } returns connectionConfig
        every { explorer.componentManager } returns ApplicationManager.getApplication()

        val ussFile = mockk<UssFile>()
        every { ussFile.name } returns "name"
        every { ussFile.isDirectory } returns false
        val fileMode = mockk<FileMode>()
        every { fileMode.owner } returns 1
        every { ussFile.fileMode } returns fileMode
        every { ussFile.size } returns 0L
        every { ussFile.uid } returns 0L
        every { ussFile.gid } returns 0L
        every { ussFile.user } returns "User"
        every { ussFile.groupId } returns "Group"
        every { ussFile.modificationTime } returns "Time"
        every { ussFile.target } returns "Target"

        should("get USS file properties when old charset = new charset") {

          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", connectionConfig))
          every {attributes.path} returns "path"

          val oldCharset = Charset.forName("IBM-1047")
          every {attributes.charset} returns oldCharset

          val initFileMode = mockk<FileMode>()
          every {initFileMode.owner} returns 1
          every {attributes.fileMode?.owner} returns 1
          every {attributes.fileMode?.group} returns 1
          every {attributes.fileMode?.all} returns 1
          every {attributes.fileMode?.prefix} returns ""
          every {attributes.fileMode?.clone()} returns initFileMode

          val newFileMode = mockk<FileMode>()
          every {newFileMode.owner} returns 2

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set properties dialog should not be used.")
          }
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Member properties dialog should not be used.")
          }

          mockkConstructor(UssFilePropertiesDialog::class)
          every { anyConstructed<UssFilePropertiesDialog>().showAndGet() } returns true

          // Run action:
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val dialog = UssFilePropertiesDialog(project, UssFileState(attributes,true))

          assertTrue(dialog.showAndGet())
          assertNotNull(attributes.fileMode, "USS file attributes should not be null")
          verify {attributes.charset }
          assertEquals(oldCharset, attributes.charset, "The charset is not modified")

        }

        should("get USS file properties when old charset != new charset") {

          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", connectionConfig))
          every {attributes.path} returns "path"

          val oldCharset = Charset.forName("IBM-1047")
          val newCharset = Charset.forName("IBM-500")
          every { attributes.charset } returns newCharset

          val initFileMode = mockk<FileMode>()
          every {initFileMode.owner} returns 1
          every {attributes.fileMode?.owner} returns 1
          every {attributes.fileMode?.group} returns 1
          every {attributes.fileMode?.all} returns 1
          every {attributes.fileMode?.prefix} returns ""
          every {attributes.fileMode?.clone()} returns initFileMode

          val newFileMode = mockk<FileMode>()
          every {newFileMode.owner} returns 2

          val bytes = byteArrayOf(116, 101, 120, 116)
          val contentSynchronizerMock = mockk<ContentSynchronizer>()
          every { contentSynchronizerMock.successfulContentStorage(any()) } returns bytes

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }
            override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
              return contentSynchronizerMock
            }
            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set properties dialog should not be used.")
          }
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Member properties dialog should not be used.")
          }

          mockkObject(ChangeEncodingDialog)
          every { ChangeEncodingDialog["initialize"](any<() -> Unit>()) } returns Unit
          mockkConstructor(ChangeEncodingDialog::class)
          every { anyConstructed<ChangeEncodingDialog>().show() } returns Unit

          mockkConstructor(UssFilePropertiesDialog::class)
          every { anyConstructed<UssFilePropertiesDialog>().showAndGet() } returns true

          every { attributes.charset = oldCharset } just Runs

          // Run action:
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val dialog = UssFilePropertiesDialog(project, UssFileState(attributes,true))

          assertTrue(dialog.showAndGet())
          assertNotNull(attributes.fileMode, "USS file attributes should not be null")
          verify {attributes.charset }
          assertNotEquals(oldCharset, attributes.charset, "The charset is not modified")

        }

        should("get USS file properties when old charset != new charset and isDirectory=true") {

          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", connectionConfig))
          every {attributes.path} returns "path"

          every {virtualFile.isDirectory} returns true
          val oldCharset = Charset.forName("IBM-1047")
          val newCharset = Charset.forName("IBM-500")
          every { attributes.charset } returns newCharset

          val initFileMode = mockk<FileMode>()
          every {initFileMode.owner} returns 1
          every {attributes.fileMode?.owner} returns 1
          every {attributes.fileMode?.group} returns 1
          every {attributes.fileMode?.all} returns 1
          every {attributes.fileMode?.prefix} returns ""
          every {attributes.fileMode?.clone()} returns initFileMode
          val newFileMode = mockk<FileMode>()
          every {newFileMode.owner} returns 2

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkStatic(::initialize)
          every { initialize(any()) } returns Unit
          mockkObject(DatasetPropertiesDialog)
          mockkObject(UssFilePropertiesDialog)
          mockkObject(MemberPropertiesDialog)

          every { DatasetPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Data set properties dialog should not be used.")
          }
          every { UssFilePropertiesDialog["initialize"](any<() -> Unit>()) } returns Unit
          every { MemberPropertiesDialog["initialize"](any<() -> Unit>()) } answers {
            throw IllegalStateException("Member properties dialog should not be used.")
          }

          mockkConstructor(UssFilePropertiesDialog::class)
          every { anyConstructed<UssFilePropertiesDialog>().showAndGet() } returns true

          every { attributes.charset = oldCharset } just Runs

          // Run action:
          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val dialog = UssFilePropertiesDialog(project, UssFileState(attributes,true))

          assertTrue(dialog.showAndGet())
          assertNotNull(attributes.fileMode, "USS file attributes should not be null")
          verify {attributes.charset }
          assertNotEquals(oldCharset, attributes.charset, "The charset is not modified")

        }

        should("if dialog.showAndGet() is false the charset is not changed") {

          val attributes = spyk(RemoteUssAttributes("rootPath", ussFile, "url", connectionConfig))
          every {attributes.path} returns "path"

          val oldCharset = Charset.forName("IBM-1047")
          every { attributes.charset } returns oldCharset

          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          mockkConstructor(UssFilePropertiesDialog::class)
          every { anyConstructed<UssFilePropertiesDialog>().showAndGet() } returns false

          GetFilePropertiesAction().actionPerformed(getPropertiesEvent)
          val dialog = UssFilePropertiesDialog(project, UssFileState(attributes,virtualFile.isBeingEditingNow()))

          assertFalse(dialog.showAndGet())
          assertNotNull(attributes.fileMode, "USS file attributes should not be null")
          verify { attributes.charset = oldCharset }
          assertEquals(oldCharset, attributes.charset, "The charset is not modified")
        }

      }

      context("Data set attributes - update()") {

        val dsNode = mockk<FileLikeDatasetNode >()
        every { dsNode.virtualFile } returns virtualFile
        every { virtualFile.isDirectory } returns false

        every { dsNode.unit } returns fileUnit
        every { fileUnit.connectionConfig } returns connectionConfig

        val dataset = mockk<Dataset>()
        every { dataset.name } returns "name"
        every { dataset.migrated } returns HasMigrated.NO
        every { dataset.datasetOrganization } returns DatasetOrganization.PO
        every { dataset.volumeSerial } returns "serial"
        every { dataset.dsnameType } returns DsnameType.PDS
        every { dataset.catalogName } returns "catalog"
        every { dataset.volumeSerials } returns "vol"
        every { dataset.deviceType } returns "devtype"
        every { dataset.recordFormat } returns RecordFormat.FB
        every { dataset.recordLength } returns 80
        every { dataset.blockSize } returns 80
        every { dataset.sizeInTracks } returns 80
        every { dataset.spaceUnits } returns SpaceUnits.TRACKS
        every { dataset.usedTracksOrBlocks } returns 80
        every { dataset.extendsUsed } returns 80
        every { dataset.creationDate } returns "0001-01-01"
        every { dataset.lastReferenceDate } returns "0001-01-01"
        every { dataset.expirationDate } returns "0001-01-01"
        every { dataset.spaceOverflowIndicator } returns "ind"

        val excludes: MutableList<String> = ArrayList()
        excludes.add("excl_name")
        val queryMask = spyk(DSMask("mask", excludes))

        val presentation = mockk<Presentation>(relaxed = true)
        every { getPropertiesEvent.presentation } returns presentation
        every { presentation.isVisible } returns true
        every { presentation.setVisible(true) } returns Unit


        should("not call dataOpsManager.tryToGetAttributes when node is null") {

          val nodeData = mockk<NodeData<ConnectionConfig>>(relaxed = true)
          val node = mockk<UssFileNode>()
          every{nodeData?.node} returns null

          var test = false
          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              test = true
              return mockk<FileAttributes>()
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          // Simulate the action
          GetFilePropertiesAction().update(getPropertiesEvent)

          // Verify dataOpsManager.tryToGetAttributes() is NOT called
          assertNotNull(node)
          assertFalse(test)
        }

        should("call dataOpsManager.tryToGetAttributes when node is FileLikeDatasetNode") {

          val nodeData = mockk<NodeData<ConnectionConfig>>(relaxed = true)
          val node = mockk<FileLikeDatasetNode>()
          every{nodeData.node} returns node

          val attributes =
            spyk(RemoteDatasetAttributes(dataset, "test", mutableListOf(MaskedRequester(connectionConfig, queryMask))))
          every {attributes.isMigrated} returns true
          val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
          dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
            override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
              return attributes
            }

            override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
              throw IllegalStateException("No operation is expected to be performed.")
            }
          }

          // Simulate the action
          GetFilePropertiesAction().update(getPropertiesEvent)
          val datasetAttributes = dataOpsManager.tryToGetAttributes(virtualFile) as RemoteDatasetAttributes

          // Verify dataOpsManager.tryToGetAttributes() is NOT called
          assertNotNull(node)
          assertNotNull(datasetAttributes)
          assertFalse(presentation.isEnabled)
        }
      }

    }
  }

})
