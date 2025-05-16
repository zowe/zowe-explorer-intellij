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
import com.intellij.openapi.util.Key
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.config.ws.ui.jes.JesWsDialog
import org.zowe.explorer.explorer.ui.FILE_EXPLORER_CONTEXT_MENU
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import java.util.Optional

class AddJesWorkingSetActionTestSpec : AppInitShouldSpec("explorer/actions/AddJesWorkingSetAction", {
  context("all functions") {
    val addJesWorkingSetAction = AddJesWorkingSetAction()

    val configServiceCrudable = mockk<Crudable> {
      every { nextUniqueValue<JesWorkingSetConfig, String>(JesWorkingSetConfig::class.java) } returns "test"
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    beforeEach {
      every {
        configServiceCrudable.getAll(ConnectionConfig::class.java)
      } answers {
        listOf(mockk<ConnectionConfig> { every { uuid } returns "test_uuid" }).stream()
      }
      every {
        configServiceCrudable.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.ofNullable(null)
    }

    context("update") {
      var didChangeIsEnabled = false
      var didChangeIsVisible = false
      var didAddTooltip = false
      var isEnabledNewValue: Boolean? = null
      var isVisibleNewValue: Boolean? = null

      val eventMock = mockk<AnActionEvent> {
        every { presentation } returns mockk {
          every {
            isEnabledAndVisible = any()
          } answers {
            callOriginal()
          }
          every {
            isEnabled = any<Boolean>()
          } answers {
            didChangeIsEnabled = true
            isEnabledNewValue = firstArg<Boolean>()
          }
          every {
            isVisible = any<Boolean>()
          } answers {
            didChangeIsVisible = true
            isVisibleNewValue = firstArg<Boolean>()
          }
          every { text = any() } returns Unit
          every {
            putClientProperty(any<Key<String>>(), any<String>())
          } answers {
            didAddTooltip = true
          }
        }
      }

      beforeEach {
        didChangeIsEnabled = false
        didChangeIsVisible = false
        didAddTooltip = false
        isEnabledNewValue = null
        isVisibleNewValue = null

        every { eventMock.place } returns "JES Explorer"
      }

      should("allow to add a JES working set when there is a connection config already added") {
        addJesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsVisible shouldBe false
          isVisibleNewValue shouldBe null
          didChangeIsEnabled shouldBe false
          isEnabledNewValue shouldBe null
          didAddTooltip shouldBe false
        }
      }

      should("not allow to add a JES working set when there is a Files Explorer view as the current view") {
        every { eventMock.place } returns FILE_EXPLORER_CONTEXT_MENU

        addJesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsVisible shouldBe true
          isVisibleNewValue shouldBe false
          didChangeIsEnabled shouldBe true
          isEnabledNewValue shouldBe false
          didAddTooltip shouldBe false
        }
      }

      should("not allow to add a JES working set when there is no connection configs added yet") {
        every {
          configServiceCrudable.getAll(ConnectionConfig::class.java)
        } answers {
          listOf<ConnectionConfig>().stream()
        }

        addJesWorkingSetAction.update(eventMock)

        assertSoftly {
          didChangeIsVisible shouldBe false
          isVisibleNewValue shouldBe null
          didChangeIsEnabled shouldBe true
          isEnabledNewValue shouldBe false
          didAddTooltip shouldBe true
        }
      }
    }

    context("createDialog") {
      should("create a JES Working Set creation dialog") {
        runInEdtAndWait {
          val result = addJesWorkingSetAction.createDialog(configServiceCrudable)

          assertSoftly { (result is JesWsDialog) shouldBe true }
        }
      }
    }
  }
})
