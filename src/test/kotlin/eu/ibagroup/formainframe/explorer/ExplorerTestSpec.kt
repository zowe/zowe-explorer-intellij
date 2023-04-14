/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.ide.dnd.*
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.ProjectViewImpl
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.treeStructure.Tree
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.*
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import groovy.lang.Tuple4
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.*
import java.util.stream.Stream
import javax.swing.JRootPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class ExplorerTestSpec : ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: FilesWorkingSetImpl") {

    context("addUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf()

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1, "filesWSuuid1", "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf()
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
        FilesWorkingSetConfig(
          uuid1,
          "filesWSuuid1",
          "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf(UssPath("/u/test1"))
        ),
        FilesWorkingSetConfig(
          uuid1,
          "filesWSuuid1",
          "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf()
        )
      )

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual1: Optional<FilesWorkingSetConfig>? = null
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
        actual1 =
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf(UssPath("/u/test1"))
          ).optional
        actual1
      }

      // addUssPath when clone and collection.add succeeds
      should("add USS path to a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual1 shouldBe expected
        }
      }

      // addUssPath when clone succeeds but collection.add is not
      should("add USS path to a config if collection.add is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))
        val actual2 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual2 shouldBe expected
        }
      }

      // addUssPath with null config
      should("not add USS path to a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.addUssPath(UssPath("/u/test2"))
        val actual3 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual3 shouldBe expected
        }
      }
    }

    context("removeUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf(UssPath("/u/uss_path_to_remove"))

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1,
        "filesWSuuid1",
        "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf(UssPath("/u/uss_path_to_remove"))
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
        FilesWorkingSetConfig(
          uuid1, "filesWSuuid1", "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf()
        ),
        FilesWorkingSetConfig(
          uuid1, "filesWSuuid1", "connUuid",
          mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
          mutableListOf(UssPath("/u/uss_path_to_remove"))
        )
      )

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual4: Optional<FilesWorkingSetConfig>? = null
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
        actual4 =
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf()
          ).optional
        actual4
      }

      // removeUssPath when clone and collection.remove succeeds
      should("remove USS path from a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual4 shouldBe expected
        }
      }

      // removeUssPath when clone succeeds but collection.remove is not
      should("remove USS path from a config if collection.remove is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual5 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual5 shouldBe expected
        }
      }

      // removeUssPath with null config
      should("not remove USS path from a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual6 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual6 shouldBe expected
        }
      }
    }
  }

  context("explorer module: ui/FileExplorerViewDropTarget") {

    var isUpdatePerformed = false
    var isDropPossible = false
    var isDragHighlighted = false

    beforeEach {
      isUpdatePerformed = false
      isDropPossible = false
      isDragHighlighted = false
    }

    val mockedDnDEvent = mockk<DnDEvent>()
    every { mockedDnDEvent.isDropPossible = any() as Boolean } answers {
      isDropPossible = true
    }
    every { mockedDnDEvent.setHighlighting(any() as RelativeRectangle, any() as Int) } answers {
      isDragHighlighted = true
      isUpdatePerformed = true
    }

    context("perform various drops and perform update") {

      val mockedJTree = mockk<Tree>()
      val mockedFileExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSetImpl>>()
      val mockedCopyPasterProvider = mockk<FileExplorerView.ExplorerCopyPasteSupport>()

      val fileExplorerViewDropTarget = spyk(
        FileExplorerViewDropTarget(mockedJTree, mockedFileExplorer, mockedCopyPasterProvider), recordPrivateCalls = true
      )

      mockkStatic(FileExplorerViewDragSource::class)
      mockkStatic(FileExplorerViewDragSource.ExplorerTransferableWrapper::class)

      val defaultBounds = Rectangle(50, 200, 100, 50)
      var defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
      var defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
      val defaultSourceTargetBounds = Tuple4(defaultSources, defaultTarget, defaultBounds, mockedJTree)
      mockkObject(defaultSourceTargetBounds)
      var defaultSourcesList = defaultSources.toList()
      mockkObject(defaultSourcesList)

      every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
      every { mockedCopyPasterProvider.pasteProvider } returns mockk()
      every { mockedCopyPasterProvider.copyProvider } returns mockk()
      every { mockedCopyPasterProvider.cutProvider } returns mockk()
      every { defaultSourceTargetBounds.v1 } returns defaultSources

      every { mockedCopyPasterProvider.project } returns mockk()
      every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
      every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true

      var isCutPerformed: Boolean
      var isCopyPerformed: Boolean
      var isPastePerformed: Boolean

      every { mockedCopyPasterProvider.cutProvider.performCut(any() as DataContext) } answers {
        isCutPerformed = true
      }
      every { mockedCopyPasterProvider.copyProvider.performCopy(any() as DataContext) } answers {
        isCopyPerformed = true
      }
      every { mockedCopyPasterProvider.pasteProvider.performPaste(any() as DataContext) } answers {
        isPastePerformed = true
      }

      // drop
      should("perform paste from project files to the mainframe files through the copy provider") {
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }
            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe files to the project files through the copy provider") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should ("perform paste from local/remote through the cut provider") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true

        }
      }

      should ("perform paste from local/remote if no cut/copy provider enabled") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns false
        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns false
        isCutPerformed = false
        isCopyPerformed = false
        isPastePerformed = false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe true

        }
      }

      should("perform paste from mainframe z/OS datasets to the USS files within one remote system") {
        val dataOpsManagerService = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        every { mockedFileExplorer.componentManager } returns ApplicationManager.getApplication()
        val mockedAttributeService = mockk<AttributesService<FileAttributes, VirtualFile>>()
        val mockedParentDatasetAttributes = mockk<RemoteDatasetAttributes>()
        dataOpsManagerService.testInstance = object : TestDataOpsManagerImpl(mockedFileExplorer.componentManager) {
          override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
            attributesClass: Class<out A>,
            vFileClass: Class<out F>
          ): AttributesService<A, F> {
            return mockedAttributeService as AttributesService<A, F>
          }
        }
        every { mockedAttributeService.getAttributes(any() as VirtualFile) } returns mockedParentDatasetAttributes

        defaultTarget = TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD"))
        defaultSources = arrayOf(TreePath(arrayOf("ARST.*", "ARST.TEST", "SAMPLE")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssFileNode>()
        val mockedNodeSource = mockk<FileLikeDatasetNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java) } returns mockk()
        val targetAttributes = mockk<RemoteUssAttributes>()
        val sourceAttributes = mockk<RemoteMemberAttributes>()
        every { sourceAttributes.parentFile } returns mockk()
        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<UssRequester>()
        val requester2 = mockk<MaskedRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { mockedParentDatasetAttributes.requesters } returns mutableListOf(requester2)
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileTarget) } returns targetAttributes
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileSource) } returns sourceAttributes

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files to the datasets within one remote system") {
        defaultTarget = TreePath(arrayOf("ARST.*", "ARST.TEST"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java) } returns mockk()
        val targetAttributes = mockk<RemoteDatasetAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()
        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<MaskedRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileTarget) } returns targetAttributes
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileSource) } returns sourceAttributes

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files to the datasets within one remote system if cutProvider is not enabled") {
        defaultTarget = TreePath(arrayOf("ARST.*", "ARST.TEST"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCutPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<LibraryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java) } returns mockk()
        val targetAttributes = mockk<RemoteDatasetAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()
        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test1:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<MaskedRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileTarget) } returns targetAttributes
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileSource) } returns sourceAttributes

        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns false
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isPastePerformed shouldBe true
        }
      }

      should("perform paste from mainframe USS files of the first mainframe to the USS files of the other") {
        defaultTarget = TreePath(arrayOf("/u/root", "/test", "/test1"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        defaultSourcesList = defaultSources.toList()
        mockkObject(defaultTarget)
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        isCopyPerformed = false
        isPastePerformed = false

        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()

        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedNodeTarget = mockk<UssFileNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java) } returns mockk()
        val targetAttributes = mockk<RemoteUssAttributes>()
        val sourceAttributes = mockk<RemoteUssAttributes>()
        val conn1 = mockk<ConnectionConfig>()
        val conn2 = mockk<ConnectionConfig>()
        every { conn1.url } returns "https://test1:10443"
        every { conn2.url } returns "https://test2:10443"
        every { conn1.isAllowSelfSigned } returns true
        every { conn2.isAllowSelfSigned } returns true
        val requester1 = mockk<UssRequester>()
        val requester2 = mockk<UssRequester>()
        every { requester1.connectionConfig } returns conn1
        every { requester2.connectionConfig } returns conn2
        every { targetAttributes.requesters } returns mutableListOf(requester1)
        every { sourceAttributes.requesters } returns mutableListOf(requester2)
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileTarget) } returns targetAttributes
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileSource) } returns sourceAttributes

        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      // update
      should("highlight places where paste is possible") {
        defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns true
        every { mockedCopyPasterProvider.isPastePossibleFromPath(any() as List<TreePath>, any() as List<TreePath>) } returns true
        every { mockedJTree.parent } returns mockk<JRootPane>()
        every { mockedJTree.isShowing } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isUpdatePerformed shouldBe true
          isDragHighlighted shouldBe true
        }
      }

      should("highlight is not possible if Tuple4 of sources and target is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns null
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if sources from Tuple4 is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        every { defaultSourceTargetBounds.v1 } returns null
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if data flavour is not supported") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is not possible if target virtual file is null") {
        every { fileExplorerViewDropTarget["getSourcesTargetAndBounds"](mockedDnDEvent) } returns defaultSourceTargetBounds
        defaultTarget = TreePath(arrayOf("project", "project", "test1", "test2"))
        defaultSources = arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        mockkObject(defaultTarget)
        every { mockedDnDEvent.isDataFlavorSupported(any() as DataFlavor) } returns true
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        val mockedProject = mockk<Project>()
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { ProjectViewImpl.getInstance(mockedProject) } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane.tree } returns mockedJTree
        val mockedStructureTreeModelNode = mockk<DefaultMutableTreeNode>()
        val mockedNode = mockk<ProjectViewNode<*>>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNode
        every { mockedStructureTreeModelNode.userObject } returns mockedNode
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is possible if target virtual file is not null and paste is possible") {
        defaultSourcesList = defaultSources.toList()
        val sources = defaultSourcesList[0]
        mockkObject(sources)
        val mockedStructureTreeModelNode = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedPsiNode = mockk<PsiDirectoryNode>()
        val mockedNodeSource = mockk<UssFileNode>()
        val mockedVirtualFileSource = mockk<MFVirtualFile>()
        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNode
        every { sources.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNode.userObject } returns mockedPsiNode
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedPsiNode.virtualFile } returns mockk<MFVirtualFile>()
        every { mockedNodeSource.virtualFile } returns mockedVirtualFileSource

        every { mockedFileExplorer.componentManager } returns mockk()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java) } returns mockk()
        val sourceAttributes = mockk<RemoteUssAttributes>()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileSource) } returns sourceAttributes

        every { mockedCopyPasterProvider.isPastePossible(any() as List<VirtualFile>, any() as List<NodeData<*>>) } returns true
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isDragHighlighted shouldBe true
          isUpdatePerformed shouldBe true
        }
      }

      should("highlight is not possible if target virtual file is not null and paste is not possible") {
        every { mockedCopyPasterProvider.isPastePossible(any() as List<VirtualFile>, any() as List<NodeData<*>>) } returns false
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

      should("highlight is possible if paste is possible and isCopiedFromRemote is false") {
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }
            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        every { defaultSourceTargetBounds.v4 } returns mockedJTree
        defaultTarget = TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2"))
        val source = TreePath(arrayOf("project", "project", "test1", "test2"))
        mockkObject(source)
        defaultSources = arrayOf(source)
        every { defaultSourceTargetBounds.v1 } returns defaultSources
        every { defaultSourceTargetBounds.v2 } returns defaultTarget
        mockkObject(defaultTarget)

        val mockedStructureTreeModelNodeSource = mockk<DefaultMutableTreeNode>()
        val mockedStructureTreeModelNodeTarget = mockk<DefaultMutableTreeNode>()
        val mockedNodeSource = mockk<PsiDirectoryNode>()
        val mockedNodeTarget = mockk<UssDirNode>()
        every { source.lastPathComponent } returns mockedStructureTreeModelNodeSource
        every { mockedStructureTreeModelNodeSource.userObject } returns mockedNodeSource
        every { mockedNodeSource.virtualFile } returns mockk<MFVirtualFile>()

        every { defaultTarget.lastPathComponent } returns mockedStructureTreeModelNodeTarget
        every { mockedStructureTreeModelNodeTarget.userObject } returns mockedNodeTarget
        val mockedVirtualFileTarget = mockk<MFVirtualFile>()
        every { mockedNodeTarget.virtualFile } returns mockedVirtualFileTarget
        val targetAttributes = mockk<RemoteUssAttributes>()
        every { mockedFileExplorer.componentManager.getService(DataOpsManager::class.java).tryToGetAttributes(mockedVirtualFileTarget) } returns targetAttributes
        every { mockedCopyPasterProvider.isPastePossibleForFiles(any() as List<VirtualFile>, any() as List<VirtualFile>) } returns true

        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDropPossible shouldBe true
          isDragHighlighted shouldBe true
          isUpdatePerformed shouldBe true
        }
      }

      should("highlight is not possible if everything is not met conditions") {
        every { mockedDnDEvent.attachedObject } answers {
          FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        }
        every { defaultSourceTargetBounds.v4 } returns mockk()
        fileExplorerViewDropTarget.update(mockedDnDEvent)
        assertSoftly {
          isDragHighlighted shouldBe false
          isUpdatePerformed shouldBe false
        }
      }

    }
  }

  context("explorer module: ui/ExplorerDropTarget additional tests") {
    context("getSourcesTargetAndBounds") {

      val mockedDnDEvent = mockk<DnDEvent>()
      var isDropPossibleForEvent = false
      every { mockedDnDEvent.setDropPossible(any() as Boolean, any() as String) } answers {
        isDropPossibleForEvent = false
      }
      val mockedJTree = mockk<Tree>()
      val mockedProjectTree = mockk<Tree>()
      val mockedFileExplorer = mockk<Explorer<ConnectionConfig, FilesWorkingSetImpl>>()
      val mockedCopyPasterProvider = mockk<FileExplorerView.ExplorerCopyPasteSupport>()
      val mockedProject = mockk<Project>()

      var isCutPerformed = false
      var isCopyPerformed = false
      var isPastePerformed = false

      beforeEach {
        isCutPerformed = false
        isCopyPerformed = false
        isPastePerformed = false
      }

      val fileExplorerViewDropTarget = spyk(
        FileExplorerViewDropTarget(mockedJTree, mockedFileExplorer, mockedCopyPasterProvider), recordPrivateCalls = true
      )

      should("perform drop with defined getSourcesTargetAndBounds when tree is myTree") {
        val mockedPoint = Point(500, 220)
        mockkObject(mockedPoint)

        every { mockedDnDEvent.point } returns mockedPoint
        every { mockedDnDEvent.currentOverComponent } returns mockedJTree
        every { mockedJTree.getClosestPathForLocation(any() as Int, any() as Int) } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedJTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        val mockedDnDWrapper = FileExplorerViewDragSource.ExplorerTransferableWrapper(mockedJTree)
        val mockedDefaultWrapper =  object : TransferableWrapper {
          override fun asFileList(): MutableList<File>? {
            TODO("Not yet implemented")
          }
          override fun getTreeNodes(): Array<TreeNode>? {
            TODO("Not yet implemented")
          }
          override fun getPsiElements(): Array<PsiElement>? {
            TODO("Not yet implemented")
          }
        }
        mockkObject(mockedDnDWrapper)
        mockkObject(mockedDefaultWrapper)
        every { mockedDnDWrapper.treePaths } returns arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))
        every { mockedDefaultWrapper.treePaths } returns arrayOf(TreePath(arrayOf("u/root", "/test_1", "/u/ZOSMFAD", "test_2")))

        mockkStatic(FileExplorerViewDragSource::class)
        mockkStatic(FileExplorerViewDragSource.ExplorerTransferableWrapper::class)

        every { mockedCopyPasterProvider.pasteProvider } returns mockk()
        every { mockedCopyPasterProvider.copyProvider } returns mockk()
        every { mockedCopyPasterProvider.cutProvider } returns mockk()

        every { mockedCopyPasterProvider.project } returns mockk()
        every { mockedCopyPasterProvider.cutProvider.isCutEnabled(any() as DataContext) } returns true
        every { mockedCopyPasterProvider.copyProvider.isCopyEnabled(any() as DataContext) } returns true

        every { mockedCopyPasterProvider.cutProvider.performCut(any() as DataContext) } answers {
          isCutPerformed = true
        }
        every { mockedCopyPasterProvider.copyProvider.performCopy(any() as DataContext) } answers {
          isCopyPerformed = true
        }
        every { mockedCopyPasterProvider.pasteProvider.performPaste(any() as DataContext) } answers {
          isPastePerformed = true
        }

        every { mockedDnDEvent.attachedObject } returns mockedDefaultWrapper

        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when tree is mot myTree, but project tree is null") {
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isDropPossibleForEvent shouldBe false
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when tree is project tree") {
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { ProjectViewImpl.getInstance(mockedProject) } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane } returns mockk()
        every { ProjectViewImpl.getInstance(mockedProject).currentProjectViewPane.tree } returns mockedProjectTree
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        every { mockedProjectTree.getClosestPathForLocation(any() as Int, any() as Int) } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when event tree is new tree, but current tree is the project tree") {
        every { mockedDnDEvent.currentOverComponent } returns mockk()
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when bounds are not suitable") {
        every { mockedDnDEvent.currentOverComponent } returns mockedProjectTree
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 400, 100, 50)
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when project is null to get project tree") {
        every { mockedCopyPasterProvider.project } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when project tree getClosestPathForLocation returns null") {
        every { mockedCopyPasterProvider.project } returns mockedProject
        every { mockedProjectTree.getClosestPathForLocation(any() as Int, any() as Int) } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when transfer data (sources) is not an instance of TransferableWrapper") {
        every { mockedProjectTree.getClosestPathForLocation(any() as Int, any() as Int) } returns TreePath(arrayOf("project", "project", "test1", "test2"))
        every { mockedProjectTree.getPathBounds(any() as TreePath) } returns Rectangle(50, 200, 100, 50)
        every { mockedDnDEvent.attachedObject } answers {
          object : DnDSource {
            override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
              TODO("Not yet implemented")
            }
            override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
              TODO("Not yet implemented")
            }
          }
        }
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCopyPerformed shouldBe true
          isPastePerformed shouldBe true
        }
      }

      should("perform drop with defined getSourcesTargetAndBounds when current event point is null") {
        every { mockedDnDEvent.point } returns null
        fileExplorerViewDropTarget.drop(mockedDnDEvent)
        assertSoftly {
          isCutPerformed shouldBe false
          isCopyPerformed shouldBe false
          isPastePerformed shouldBe false
        }
      }
    }
  }

  context("explorer module: ui/ExplorerPasteProvider") {
    // performPaste
    should("perform paste without conflicts") {}
    should("perform paste accepting conflicts") {}
    should("perform paste declining conflicts") {}
  }
  context("explorer module: actions/RenameAction") {
    // actionPerformed
    should("perform rename on dataset") {}
    should("perform rename on dataset member") {}
    should("perform rename on USS file") {}
    should("perform rename on USS directory") {}
  }
  context("explorer module: actions/PurgeJobAction") {
    // actionPerformed
    should("perform purge on job successfully") {}
    should("perform purge on job with error") {}
  }
  context("explorer module: actions/GetJobPropertiesAction") {
    // actionPerformed
    should("get job properties") {}
    should("get spool file properties") {}
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    // actionPerformed
    should("get dataset properties") {}
    should("get dataset member properties") {}
    should("get USS file properties") {}
  }
  context("explorer module: actions/ForceRenameAction") {
    // actionPerformed
    should("perform force rename on USS file") {}
    should("not rename dataset") {}
  }
  context("explorer module: actions/ChangeContentModeAction") {
    // isSelected
    should("check if the 'Use binary mode' selected for a file") {}
    // setSelected
    should("select 'Use binary mode' for a file") {}
    should("unselect 'Use binary mode' for a file") {}
  }
  context("explorer module: actions/AllocateDatasetAction") {
    // actionPerformed
    should("perform dataset allocate with default parameters") {}
  }
  context("explorer module: actions/AddMemberAction") {
    // actionPerformed
    should("perform dataset member allocate with default parameters") {}
  }
})
