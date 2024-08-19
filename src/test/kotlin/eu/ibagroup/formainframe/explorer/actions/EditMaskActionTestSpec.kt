/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.MaskState
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerContentProvider
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.explorer.ui.AddOrEditMaskDialog
import eu.ibagroup.formainframe.explorer.ui.DSMaskNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestUIComponentManager
import eu.ibagroup.formainframe.utils.MaskType
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.service
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import java.util.*

class EditMaskActionTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: actions/EditMaskAction") {
    val editMaskAction = EditMaskAction()

    val uuid = "test"
    val selectedNodeMock = mockk<NodeData<ConnectionConfig>>()
    val fileExplorerViewMock = mockk<FileExplorerView>()
    val anActionEventMock = mockk<AnActionEvent>()
    val explorerTreeNodeMock = mockk<ExplorerTreeNode<ConnectionConfig, *>>()
    val filesWorkingSetMock = mockk<FilesWorkingSet>()
    val configServiceMock = mockk<ConfigService>()
    val credentialServiceMock = mockk<CredentialService>()
    val crudableMock = mockk<Crudable>()
    val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()

    val explorerTreeStructBaseMock = object : TestExplorerTreeStructureBase(mockk(), mockk()) {
      override fun registerNode(node: ExplorerTreeNode<*, *>) {}
    }

    beforeEach {
      val uiComponentManagerService: TestUIComponentManager =
        ApplicationManager.getApplication().service<UIComponentManager>() as TestUIComponentManager
      uiComponentManagerService.testInstance = object : TestUIComponentManager() {
        override fun <E : Explorer<*, *>> getExplorerContentProvider(
          clazz: Class<out E>
        ): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>> {
          return mockk()
        }
      }

      every { filesWorkingSetMock.explorer } returns mockk()

      // Needed here to initialize other components somewhere (probably bug?)
      UssDirNode(UssPath("test"), mockk(), explorerTreeNodeMock, filesWorkingSetMock, explorerTreeStructBaseMock)

      every { selectedNodeMock.node } returns mockk()
      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeMock)
      every { anActionEventMock.getExplorerView<FileExplorerView>() } returns fileExplorerViewMock

      every { filesWorkingSetMock.uuid } returns uuid
      every { filesWorkingSetMock.connectionConfig } returns null
      every { explorerTreeNodeMock.value } returns filesWorkingSetMock

      every {
        crudableMock.getByUniqueKey(filesWorkingSetConfigMock::class.java, uuid)
      } returns Optional.of(filesWorkingSetConfigMock)

      mockkObject(ConfigService)
      every { ConfigService.instance } returns configServiceMock
      every { configServiceMock.crudable } returns crudableMock

      mockkObject(CredentialService)
      every { CredentialService.instance } returns credentialServiceMock
      every { credentialServiceMock.getUsernameByKey(any<String>()) } returns "test"

      every { anActionEventMock.project } returns mockk()

      mockkObject(AddOrEditMaskDialog)
      every { AddOrEditMaskDialog["initialize"](any<() -> Unit>()) } returns Unit

      mockkConstructor(AddOrEditMaskDialog::class)
      every { anyConstructed<AddOrEditMaskDialog>().showAndGet() } returns true
    }

    afterEach {
      clearAllMocks()
      unmockkAll()
    }

    context("actionPerformed") {
      var updated = false
      var changed = false

      beforeEach {
        updated = false
        changed = false

        every {
          crudableMock.update(any())
        } answers {
          updated = true
          mockk()
        }
      }
      context("generic") {
        should("not perform edit action if explorer view is null") {
          every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe false }
        }
        should("not perform edit action if selected node is not a DS or USS mask") {
          every { selectedNodeMock.node } returns mockk<ExplorerTreeNode<ConnectionConfig, *>>()

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe false }
        }
        should("not perform edit action if selected node is a USS directory") {
          val ussDirNode =
            UssDirNode(
              UssPath("test"),
              mockk(),
              explorerTreeNodeMock,
              filesWorkingSetMock,
              explorerTreeStructBaseMock,
              mockk()
            )

          every { selectedNodeMock.node } returns ussDirNode

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe false }
        }
      }
      context("edit USS mask") {
        lateinit var ussMaskNode: UssDirNode

        beforeEach {
          updated = false
          changed = false

          ussMaskNode =
            UssDirNode(UssPath("test"), mockk(), explorerTreeNodeMock, filesWorkingSetMock, explorerTreeStructBaseMock)

          every { selectedNodeMock.node } returns ussMaskNode
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf(UssPath("test"))
          every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf()
        }
        should("perform edit on USS mask") {
          every {
            crudableMock.update(any())
          } answers {
            updated = true
            val wsConfToUpdate = firstArg<FilesWorkingSetConfig>()
            if (wsConfToUpdate.ussPaths.size == 1) {
              val ussPath = wsConfToUpdate.ussPaths.first()
              if (ussPath.path == "test_passed") {
                changed = true
              }
            }
            mockk()
          }

          every {
            anyConstructed<AddOrEditMaskDialog>().state
          } returns MaskStateWithWS(MaskState("test_passed", MaskType.USS), filesWorkingSetMock)

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }
        should("perform edit on USS mask changing mask type") {
          every {
            crudableMock.update(any())
          } answers {
            updated = true
            val wsConfToUpdate = firstArg<FilesWorkingSetConfig>()
            if (wsConfToUpdate.dsMasks.size == 1 && wsConfToUpdate.ussPaths.isEmpty()) {
              val dsMask = wsConfToUpdate.dsMasks.first()
              if (dsMask.mask == "TEST_PASSED") {
                changed = true
              }
            }
            mockk()
          }

          every {
            anyConstructed<AddOrEditMaskDialog>().state
          } returns MaskStateWithWS(MaskState("test_passed", MaskType.ZOS), filesWorkingSetMock)

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }
        should("not perform edit on USS mask if dialog is closed") {
          every { anyConstructed<AddOrEditMaskDialog>().showAndGet() } returns false

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe false }
        }
        should("not perform edit on USS mask if working set is not found") {
          every {
            crudableMock.getByUniqueKey(filesWorkingSetConfigMock::class.java, uuid)
          } returns Optional.ofNullable(null)

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe false }
        }
        should("not perform edit on USS mask if list of USS masks is empty") {
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf()

          var throwable: Throwable? = null
          runCatching {
            editMaskAction.actionPerformed(anActionEventMock)
          }.onFailure {
            throwable = it
          }

          val expected = IndexOutOfBoundsException("Index 0 out of bounds for length 0")

          assertSoftly {
            throwable shouldBe expected
            updated shouldBe false
          }
        }
        should("not perform edit on USS mask if selected mask is not found in list of USS masks") {
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf(UssPath("other"))

          var throwable: Throwable? = null
          runCatching {
            editMaskAction.actionPerformed(anActionEventMock)
          }.onFailure {
            throwable = it
          }

          val expected = IndexOutOfBoundsException("Index 0 out of bounds for length 0")

          assertSoftly {
            throwable shouldBe expected
            updated shouldBe false
          }
        }
      }
      context("edit DS mask") {
        lateinit var dsMaskNode: DSMaskNode

        beforeEach {
          updated = false
          changed = false

          dsMaskNode =
            DSMaskNode(
              DSMask("test", mutableListOf()),
              mockk(),
              explorerTreeNodeMock,
              filesWorkingSetMock,
              explorerTreeStructBaseMock
            )

          every { selectedNodeMock.node } returns dsMaskNode
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf()
          every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf(DSMask("test", mutableListOf()))
        }
        should("perform edit on DS mask") {
          every {
            crudableMock.update(any())
          } answers {
            updated = true
            val wsConfToUpdate = firstArg<FilesWorkingSetConfig>()
            if (wsConfToUpdate.dsMasks.size == 1) {
              val dsMask = wsConfToUpdate.dsMasks.first()
              if (dsMask.mask == "TEST_PASSED") {
                changed = true
              }
            }
            mockk()
          }

          every {
            anyConstructed<AddOrEditMaskDialog>().state
          } returns MaskStateWithWS(MaskState("test_passed", MaskType.ZOS), filesWorkingSetMock)

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }
        should("perform edit on DS mask changing mask type") {
          every {
            crudableMock.update(any())
          } answers {
            updated = true
            val wsConfToUpdate = firstArg<FilesWorkingSetConfig>()
            if (wsConfToUpdate.ussPaths.size == 1 && wsConfToUpdate.dsMasks.isEmpty()) {
              val ussPath = wsConfToUpdate.ussPaths.first()
              if (ussPath.path == "test_passed") {
                changed = true
              }
            }
            mockk()
          }

          every {
            anyConstructed<AddOrEditMaskDialog>().state
          } returns MaskStateWithWS(MaskState("test_passed", MaskType.USS), filesWorkingSetMock)

          editMaskAction.actionPerformed(anActionEventMock)

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }
      }
    }

    context("update") {
      var enabledAndVisible = true

      beforeEach {
        enabledAndVisible = true
        every { anActionEventMock.presentation } returns mockk()
        every { anActionEventMock.presentation.isEnabledAndVisible = any() } answers {
          enabledAndVisible = firstArg<Boolean>()
          mockk()
        }
      }
      should("edit action is enabled and visible for dataset mask node") {
        val dsMaskNode =
          DSMaskNode(
            DSMask("test", mutableListOf()),
            mockk(),
            explorerTreeNodeMock,
            filesWorkingSetMock,
            explorerTreeStructBaseMock
          )

        every { selectedNodeMock.node } returns dsMaskNode

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe true }
      }
      should("edit action is enabled and visible for USS mask node") {
        val ussMaskNode =
          UssDirNode(
            UssPath("test"),
            mockk(),
            explorerTreeNodeMock,
            filesWorkingSetMock,
            explorerTreeStructBaseMock
          )

        every { selectedNodeMock.node } returns ussMaskNode

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe true }
      }
      should("edit action is not enabled and visible for USS dir node") {
        val ussDirNode =
          UssDirNode(
            UssPath("test"),
            mockk(),
            explorerTreeNodeMock,
            filesWorkingSetMock,
            explorerTreeStructBaseMock,
            mockk()
          )

        every { selectedNodeMock.node } returns ussDirNode

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe false }
      }
      should("edit action is not enabled and visible for other types of nodes") {
        every { selectedNodeMock.node } returns mockk()

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe false }
      }
      should("edit action is not enabled and visible if selected more than one node") {
        every { fileExplorerViewMock.mySelectedNodesData } returns listOf(mockk(), mockk())

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe false }
      }
      should("edit action is not enabled and not visible if explorer view is null") {
        every { anActionEventMock.getExplorerView<FileExplorerView>() } returns null

        editMaskAction.update(anActionEventMock)

        assertSoftly { enabledAndVisible shouldBe false }
      }
    }

    context("isDumbAware") {
      should("action is dumb aware") {
        val actual = editMaskAction.isDumbAware

        assertSoftly { actual shouldBe true }
      }
    }
  }
})
