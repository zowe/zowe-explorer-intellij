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
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.ws.*
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.utils.MaskType
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.lang.RuntimeException
import java.util.*

class EditMaskActionTestSpec : AppInitShouldSpec("explorer/actions/EditMaskAction", {
  context("all functions") {
    val editMaskAction = EditMaskAction()

    val uuidMock = "test"
    val selectedNodeMock = mockk<NodeData<ConnectionConfig>>()
    val fileExplorerViewMock = mockk<FileExplorerView>()
    val anActionEventMock = mockk<AnActionEvent>()
    val filesWorkingSetMock = mockk<FilesWorkingSet> {
      every { name } returns "test"
      every { uuid } returns uuidMock
      every { explorer } returns mockk()
      every { connectionConfig } returns null
    }
    val explorerTreeNodeMock = mockk<ExplorerTreeNode<ConnectionConfig, *>> {
      every { value } returns filesWorkingSetMock
    }
    val filesWorkingSetConfigMock = mockk<FilesWorkingSetConfig>()

    val uiComponentManagerService = UIComponentManager.getService()
    every { uiComponentManagerService.getExplorerContentProvider(any<Class<Explorer<*, *>>>()) } returns mockk()

    val configService = ConfigService.getService()
    val configServiceCrudable = mockk<Crudable>()
    every { configService.crudable } returns configServiceCrudable

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val explorerTreeStructBaseMock = object : TestExplorerTreeStructureBase(mockk(), mockk()) {
      override fun registerNode(node: ExplorerTreeNode<*, *>) {}
    }

    mockkConstructor(AddOrEditMaskDialog::class)

    beforeEach {
      // Needed here to initialize other components somewhere (probably bug?)
      UssDirNode(UssPath("test"), mockk(), explorerTreeNodeMock, filesWorkingSetMock, explorerTreeStructBaseMock)

      every { selectedNodeMock.node } returns mockk()
      every { fileExplorerViewMock.mySelectedNodesData } returns listOf(selectedNodeMock)
      every { anActionEventMock.getData(EXPLORER_VIEW) } returns fileExplorerViewMock

      every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf()
      every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf()

      every {
        configServiceCrudable.getByUniqueKey(FilesWorkingSetConfig::class.java, uuidMock)
      } returns Optional.of(filesWorkingSetConfigMock)

      every { anActionEventMock.project } returns mockk()

      every { anyConstructed<AddOrEditMaskDialog>().showAndGet() } returns true
    }

    context("actionPerformed") {
      var updated = false
      var changed = false

      beforeEach {
        updated = false
        changed = false

        every {
          configServiceCrudable.update(any())
        } answers {
          updated = true
          mockk()
        }
      }

      context("generic") {
        should("not perform edit action if explorer view is null") {
          every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe false }
        }

        should("not perform edit action if selected node is not a DS or USS mask") {
          every { selectedNodeMock.node } returns mockk<ExplorerTreeNode<ConnectionConfig, *>>()

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

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

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe false }
        }
      }

      context("edit USS mask") {
        lateinit var ussMaskNode: UssDirNode

        beforeEach {
          updated = false
          changed = false

          ussMaskNode =
            UssDirNode(
              UssPath("test"),
              mockk(),
              explorerTreeNodeMock,
              filesWorkingSetMock,
              explorerTreeStructBaseMock
            )

          every { selectedNodeMock.node } returns ussMaskNode
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf(UssPath("test"))
          every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf()
        }

        should("perform edit on USS mask") {
          every {
            configServiceCrudable.update(any())
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

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }

        should("perform edit on USS mask changing mask type") {
          every {
            configServiceCrudable.update(any())
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

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }

        should("not perform edit on USS mask if dialog is closed") {
          every { anyConstructed<AddOrEditMaskDialog>().showAndGet() } returns false

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe false }
        }

        should("not perform edit on USS mask if working set is not found") {
          every {
            configServiceCrudable.getByUniqueKey(filesWorkingSetConfigMock::class.java, uuidMock)
          } returns Optional.ofNullable(null)

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe false }
        }

        should("not perform edit on USS mask if list of USS masks is empty") {
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf()


          runInEdtAndWait {
            val throwable = shouldThrow<RuntimeException> {
              editMaskAction.actionPerformed(anActionEventMock)
            }

            assertSoftly {
              (throwable is IndexOutOfBoundsException) shouldBe true
              (throwable.message ?: "") shouldContain "Index 0 out of bounds for length 0"
              updated shouldBe false
            }
          }
        }

        should("not perform edit on USS mask if selected mask is not found in list of USS masks") {
          every { filesWorkingSetConfigMock.ussPaths } returns mutableListOf(UssPath("other"))

          runInEdtAndWait {
            val throwable = shouldThrow<RuntimeException> {
              editMaskAction.actionPerformed(anActionEventMock)
            }

            assertSoftly {
              (throwable is IndexOutOfBoundsException) shouldBe true
              (throwable.message ?: "") shouldContain "Index 0 out of bounds for length 0"
              updated shouldBe false
            }
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
          every { filesWorkingSetConfigMock.dsMasks } returns mutableListOf(DSMask("test", mutableListOf()))
        }

        should("perform edit on DS mask") {
          every {
            configServiceCrudable.update(any())
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

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

          assertSoftly { updated shouldBe true }
          assertSoftly { changed shouldBe true }
        }

        should("perform edit on DS mask changing mask type") {
          every {
            configServiceCrudable.update(any())
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

          runInEdtAndWait {
            editMaskAction.actionPerformed(anActionEventMock)
          }

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
        every { anActionEventMock.getData(EXPLORER_VIEW) } returns null

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
