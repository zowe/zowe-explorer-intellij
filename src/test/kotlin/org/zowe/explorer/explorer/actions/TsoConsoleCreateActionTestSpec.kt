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
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.explorer.ui.SelectTSOSessionDialog
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.tso.SESSION_ADDED_TOPIC
import org.zowe.explorer.tso.TSOSessionCreateHandler
import org.zowe.explorer.tso.config.TSOConfigWrapper
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.TsoResponse
import java.util.Optional
import java.util.UUID

class TsoConsoleCreateActionTestSpec : AppInitShouldSpec("explorer/actions/TsoConsoleCreateAction", {
  lateinit var currentTestUuid: UUID

  beforeSpec {
    currentTestUuid = AppInitShouldSpec.currentTestUuid ?: throw Exception("Test UUID must be defined before the spec run")
  }

  context("all functions") {
    var showAndGetTriggerCount = 0
    var didPerformOperation = false
    var didCreateSession = false
    var didShowErrorNotification = false
    var isEnabledNewValue: Boolean? = null
    var didAddTooltip = false

    val projectMock = ProjectManager.getInstance().defaultProject
    val eventMock = mockk<AnActionEvent> {
      every { project } returns projectMock
      every { presentation } returns mockk {
        every {
          isEnabled = any()
        } answers {
          isEnabledNewValue = firstArg<Boolean>()
        }
        every { text = any() } returns Unit
        every {
          putClientProperty(any<Key<Any>>(), any<String>())
        } answers {
          didAddTooltip = true
        }
      }
    }

    val crudableMock = mockk<Crudable>()
    val configService = ConfigService.Companion.getService()
    every { configService.crudable } returns crudableMock

    mockkConstructor(SelectTSOSessionDialog::class)
    every {
      anyConstructed<SelectTSOSessionDialog>().showAndGet()
    } answers {
      showAndGetTriggerCount++
      true
    }

    val dataOpsManager = DataOpsManager.getService()

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService
        .notifyError(any(), any(), any(), any(), any())
    } answers {
      didShowErrorNotification = true
    }

    subscribe(
      project = projectMock,
      topic = SESSION_ADDED_TOPIC,
      handler = object: TSOSessionCreateHandler {
        override fun create(project: Project, newSession: TSOConfigWrapper) {
          if (currentTestUuid == AppInitShouldSpec.currentTestUuid) {
            didCreateSession = true
          }
        }
      }
    )

    val tsoConsoleCreateAction = TsoConsoleCreateAction()

    beforeEach {
      showAndGetTriggerCount = 0
      didPerformOperation = false
      didCreateSession = false
      didShowErrorNotification = false
      isEnabledNewValue = null

      every {
        crudableMock.getAll(TSOSessionConfig::class.java)
      } answers {
        listOf<TSOSessionConfig>(
          mockk {
            every { connectionConfigUuid } returns "test_uuid"
            every { name } returns "test"
          }
        )
          .stream()
      }
      every {
        crudableMock.getAll(ConnectionConfig::class.java)
      } answers {
        listOf<ConnectionConfig>(mockk()).stream()
      }
      every {
        crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
      } returns Optional.of(
        mockk {
          every { url } returns "https://test.com"
        }
      )

      every {
        dataOpsManager.performOperation(any<Operation<Any>>(), any())
      } answers {
        when (val operation = firstArg<Operation<*>>()) {
          is TsoOperation -> {
            didPerformOperation = true
            mockk<TsoResponse> {
              every { servletKey } returns "test_servlet_key"
            }
          }

          else -> fail("Unexpected operation: $operation")
        }
      }
    }

    context("actionPerformed") {
      should("create a TSO console after a user fulfills the dialog") {
        runInEdtAndWait {
          tsoConsoleCreateAction.actionPerformed(eventMock)
        }

        assertSoftly {
          showAndGetTriggerCount shouldBe 1
          didPerformOperation shouldBe true
          didCreateSession shouldBe true
          didShowErrorNotification shouldBe false
        }
      }

      should("not create a TSO console after a user fulfills the dialog cause the servlet key is null") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is TsoOperation -> {
              didPerformOperation = true
              mockk<TsoResponse> {
                every { servletKey } returns null
              }
            }

            else -> fail("Unexpected operation: $operation")
          }
        }

        runInEdtAndWait {
          tsoConsoleCreateAction.actionPerformed(eventMock)
        }

        assertSoftly {
          showAndGetTriggerCount shouldBe 1
          didPerformOperation shouldBe true
          didCreateSession shouldBe false
          didShowErrorNotification shouldBe false
        }
      }

      should("not create a TSO console after a user fulfills the dialog cause there is an exception during perform operation happen") {
        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any())
        } answers {
          didPerformOperation = true
          throw Throwable("Test exception")
        }

        runInEdtAndWait {
          tsoConsoleCreateAction.actionPerformed(eventMock)
        }

        assertSoftly {
          showAndGetTriggerCount shouldBe 1
          didPerformOperation shouldBe true
          didCreateSession shouldBe false
          didShowErrorNotification shouldBe true
        }
      }

      should("not create a TSO console after a user fulfills the dialog cause there is no connection config for a TSO session") {
        every {
          crudableMock.getByUniqueKey(ConnectionConfig::class.java, any<String>())
        } returns Optional.ofNullable(null)

        runInEdtAndWait {
          tsoConsoleCreateAction.actionPerformed(eventMock)
        }

        assertSoftly {
          showAndGetTriggerCount shouldBe 1
          didPerformOperation shouldBe false
          didCreateSession shouldBe false
          didShowErrorNotification shouldBe true
        }
      }

      should("not create a TSO console after a user fulfills the dialog cause there is no TSO session config specified") {
        every {
          crudableMock.getAll(TSOSessionConfig::class.java)
        } answers { listOf<TSOSessionConfig>().stream() }

        runInEdtAndWait {
          tsoConsoleCreateAction.actionPerformed(eventMock)
        }

        assertSoftly {
          showAndGetTriggerCount shouldBe 1
          didPerformOperation shouldBe false
          didCreateSession shouldBe false
          didShowErrorNotification shouldBe true
        }
      }
    }

    context("update") {
      should("show the action as enabled") {
        tsoConsoleCreateAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe null
          didAddTooltip shouldBe false
        }
      }

      should("show the action as disabled cause there is no TSO session config") {
        every {
          crudableMock.getAll(TSOSessionConfig::class.java)
        } answers { listOf<TSOSessionConfig>().stream() }

        tsoConsoleCreateAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          didAddTooltip shouldBe true
        }
      }

      should("show the action as disabled cause there is no connection config") {
        every {
          crudableMock.getAll(ConnectionConfig::class.java)
        } answers { listOf<ConnectionConfig>().stream() }

        tsoConsoleCreateAction.update(eventMock)

        assertSoftly {
          isEnabledNewValue shouldBe false
          didAddTooltip shouldBe true
        }
      }
    }
  }
})