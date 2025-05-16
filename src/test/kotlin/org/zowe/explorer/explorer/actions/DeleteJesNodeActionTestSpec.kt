/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.JesExplorerView
import org.zowe.explorer.explorer.ui.JesFilterNode
import org.zowe.explorer.explorer.ui.JesWsNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import javax.swing.Icon
import kotlin.reflect.KFunction

class DeleteJesNodeActionTestSpec : AppInitShouldSpec("explorer/actions/DeleteJesNodeAction", {
  context("all functions") {
    var isShowYesNoDialogCalled = false
    var didChangeIsEnabledAndVisible = false
    var isEnabledAndVisibleNewValue: Boolean? = null

    val jesWsNodeMock = mockk<JesWsNode>()
    val jesExplorerViewMock = mockk<JesExplorerView>()
    val eventMock = mockk<AnActionEvent> {
      every { project } returns mockk()
      every { presentation } returns mockk {
        every {
          isEnabledAndVisible = any()
        } answers {
          didChangeIsEnabledAndVisible = true
          isEnabledAndVisibleNewValue = firstArg<Boolean>()
        }
      }
    }

    val showYesNoDialogMock: (String, String, Project?, String, String, Icon?) -> Boolean = ::showYesNoDialog
    mockkStatic(showYesNoDialogMock as KFunction<*>)
    every {
      showYesNoDialogMock(any<String>(), any<String>(), any(), any<String>(), any<String>(), any())
    } answers {
      isShowYesNoDialogCalled = true
      false
    }

    val deleteJesNodeAction = DeleteJesNodeAction()

    beforeEach {
      isShowYesNoDialogCalled = false
      didChangeIsEnabledAndVisible = false
      isEnabledAndVisibleNewValue = null

      every { eventMock.getData(EXPLORER_VIEW) } returns jesExplorerViewMock
    }

    context("actionPerformed") {
      beforeEach {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk {
            every { node } returns jesWsNodeMock
          },
          mockk {
            every { node } returns mockk<JesFilterNode> {
              every { parent } returns mockk()
            }
          },
          mockk {
            every { node } returns mockk<JesFilterNode> {
              every { parent } returns jesWsNodeMock
            }
          },
          mockk {
            every { node } returns mockk<DSMaskNode>()
          }
        )
      }

      should("perform Delete action on JES Working Set and Jobs Filter nodes") {
        deleteJesNodeAction.actionPerformed(eventMock)

        assertSoftly { isShowYesNoDialogCalled shouldBe true }
      }

      should("not perform Delete action on non-JES nodes") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<DSMaskNode>() }
        )

        deleteJesNodeAction.actionPerformed(eventMock)

        assertSoftly { isShowYesNoDialogCalled shouldBe false }
      }

      should("not perform Delete action without JES Explorer view initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        deleteJesNodeAction.actionPerformed(eventMock)

        assertSoftly { isShowYesNoDialogCalled shouldBe false }
      }
    }

    context("update") {
      beforeEach {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns jesWsNodeMock }
        )
      }

      should("show Delete action on JES node") {
        deleteJesNodeAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe true
        }
      }

      should("not show Delete action on non-JES node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk() }
        )

        deleteJesNodeAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show Delete action when there is no selected node") {
        every { jesExplorerViewMock.mySelectedNodesData } returns listOf()

        deleteJesNodeAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }

      should("not show Delete action without JES Explorer view initialized") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        deleteJesNodeAction.update(eventMock)

        assertSoftly {
          didChangeIsEnabledAndVisible shouldBe true
          isEnabledAndVisibleNewValue shouldBe false
        }
      }
    }
  }
})
