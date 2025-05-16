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
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.config.ws.ui.AbstractWsDialog
import org.zowe.explorer.config.ws.ui.AbstractWsDialogState
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.Optional

class AddWsActionBaseTestSpec : AppInitShouldSpec("explorer/actions/AddWsActionBase", {
  context("all functions") {
    var didTriggerCreateDialog = false
    var didAddWorkingSet = false

    val eventMock = mockk<AnActionEvent>()

    val dialogMock = mockk<AbstractWsDialog<*, *, *, *>> {
      every { state } returns mockk {
        every { workingSetConfig } returns mockk()
      }
    }

    val addWsAction = object : AddWsActionBase() {
      override val presentationTextInExplorer = "test"
      override val defaultPresentationText = "test"

      override fun createDialog(configCrudable: Crudable): AbstractWsDialog<*, *, *, out AbstractWsDialogState<out WorkingSetConfig, *>> {
        didTriggerCreateDialog = true
        return dialogMock
      }
    }

    val configServiceCrudable = mockk<Crudable> {
      every {
        add(any<WorkingSetConfig>())
      } answers {
        didAddWorkingSet = true
        Optional.ofNullable(null)
      }
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    beforeEach {
      didTriggerCreateDialog = false
      didAddWorkingSet = false

      every { dialogMock.showAndGet() } returns true
    }

    context("actionPerformed") {
      should("add a new working set when the dialog is fulfilled") {
        runInEdtAndWait {
          addWsAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerCreateDialog shouldBe true
          didAddWorkingSet shouldBe true
        }
      }

      should("not add a new working set when the dialog is cancelled") {
        every { dialogMock.showAndGet() } returns false

        runInEdtAndWait {
          addWsAction.actionPerformed(eventMock)
        }

        assertSoftly {
          didTriggerCreateDialog shouldBe true
          didAddWorkingSet shouldBe false
        }
      }
    }
  }
})
