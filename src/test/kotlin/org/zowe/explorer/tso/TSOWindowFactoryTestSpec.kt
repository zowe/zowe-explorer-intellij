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

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package org.zowe.explorer.tso

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.vfs.encoding.EncodingProjectManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.MessageData
import org.zowe.explorer.dataops.operations.MessageType
import org.zowe.explorer.explorer.actions.rexx.ExecuteRexxAction
import org.zowe.explorer.tso.config.TSOConfigWrapper
import org.zowe.explorer.tso.ui.TSOConsoleView
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.dataops.exceptions.CredentialsNotFoundForConnectionException
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.setPrivateFieldValue
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.lazyInjectable
import org.zowe.kotlinsdk.TsoResponse
import java.util.*
import java.util.concurrent.*
import kotlin.reflect.KFunction

class TSOWindowFactoryTestSpec : ShouldSpec({
  afterSpec {
    unmockkAll()
    clearAllMocks()
  }

  context("tso/TSOWindowFactory") {
    var didSetSelectedContent = false
    var didActivate = false
    var didChangeIsAvailable = false
    var didTriggerShow = false
    var didTriggerCreate = false
    var didTriggerClose = false
    var didTriggerProcessCommand = false
    var didTriggerNotifyError = false
    var didTriggerReconnect = false

    lateinit var closeSessionSub: TSOSessionCloseHandler
    lateinit var createSessionSub: TSOSessionCreateHandler
    lateinit var executeRexxSub: TSOSessionExecuteRexxFromExplorerHandler
    lateinit var commandEnteredSub: TSOSessionProcessCommandHandler
    lateinit var reconnectSessionSub: TSOSessionReconnectHandler
    lateinit var reopenSessionSub: TSOSessionReopenHandler

    lateinit var applicationMock: Application
    mockkStatic(ApplicationManager::getApplication)

    mockkStatic("org.zowe.explorer.utils.MiscUtilsKt")
    every {
      lazyInjectable(any<() -> Any>())
    } answers {
      val funArg = args[0]?.javaClass?.name ?: ""
      if (funArg.contains("${TSOConsoleView::class.java.name}\$consoleView")) {
        mockk<TerminalExecutionConsole>(relaxed = true) {
          every { component } returns mockk(relaxed = true)
        }
      } else if (funArg.contains("${TSOConsoleView::class.java.name}\$tsoPanel")) {
        mockk<DialogPanel>(relaxed = true)
      } else {
        fail("Unknown function to mock: $funArg")
      }
    }

    mockkStatic(::runInEdt)
    every {
      runInEdt(any(), any<() -> Unit>())
    } answers {
      secondArg<() -> Unit>()()
    }

    val messageBusMock = mockk<MessageBus> {
      every { connect() } returns mockk {
        every {
          subscribe(any<Topic<Any>>(), any<Any>())
        } answers {
          val topic = firstArg<Topic<Any>>()
          when (topic.displayName) {
            SESSION_CLOSED_TOPIC.displayName -> {
              closeSessionSub = secondArg<TSOSessionCloseHandler>()
            }
            SESSION_EXECUTE_REXX_TOPIC.displayName -> {
              executeRexxSub = secondArg<TSOSessionExecuteRexxFromExplorerHandler>()
            }
            SESSION_ADDED_TOPIC.displayName -> {
              createSessionSub = secondArg<TSOSessionCreateHandler>()
            }
            SESSION_COMMAND_ENTERED.displayName -> {
              commandEnteredSub = secondArg<TSOSessionProcessCommandHandler>()
            }
            SESSION_RECONNECT_TOPIC.displayName -> {
              reconnectSessionSub = secondArg<TSOSessionReconnectHandler>()
            }
            SESSION_REOPEN_TOPIC.displayName -> {
              reopenSessionSub = secondArg<TSOSessionReopenHandler>()
            }
          }
        }
      }
      every {
        syncPublisher(any<Topic<Any>>())
      } answers {
        val topic = value as Topic<*>
        when (topic.displayName) {
          SESSION_CLOSED_TOPIC.displayName -> {
            val closeSessionListener = value.castOrNull<Topic<TSOSessionCloseHandler>>()
            if (closeSessionListener != null) {
              mockk<TSOSessionCloseHandler> {
                every {
                  close(any<Project>(), any<TSOConfigWrapper>())
                } answers {
                  didTriggerClose = true
                  closeSessionSub.close(firstArg<Project>(), secondArg<TSOConfigWrapper>())
                }
              }
            } else {
              fail("Topic is impossible to cast to Topic<TSOSessionCloseHandler>")
            }
          }
          SESSION_ADDED_TOPIC.displayName -> {
            val createSessionListener = value.castOrNull<Topic<TSOSessionCreateHandler>>()
            if (createSessionListener != null) {
              mockk<TSOSessionCreateHandler> {
                every {
                  create(any<Project>(), any<TSOConfigWrapper>())
                } answers {
                  didTriggerCreate = true
                  createSessionSub.create(firstArg<Project>(), secondArg<TSOConfigWrapper>())
                }
              }
            } else {
              fail("Topic is impossible to cast to Topic<TSOSessionCreateHandler>")
            }
          }
          SESSION_COMMAND_ENTERED.displayName -> {
            val commandEnteredListener = value.castOrNull<Topic<TSOSessionProcessCommandHandler>>()
            if (commandEnteredListener != null) {
              mockk<TSOSessionProcessCommandHandler> {
                every {
                  processCommand(
                    any<Project>(),
                    any<TSOConsoleView>(),
                    any<TSOConfigWrapper>(),
                    any<String>(),
                    any<MessageType>(),
                    any<MessageData>(),
                    any<ProcessHandler>()
                  )
                } answers {
                  didTriggerProcessCommand = true
                  commandEnteredSub
                    .processCommand(
                      arg<Project>(0),
                      arg<TSOConsoleView>(1),
                      arg<TSOConfigWrapper>(2),
                      arg<String>(3),
                      arg<MessageType>(4),
                      arg<MessageData>(5),
                      arg<ProcessHandler>(6)
                    )
                }
              }
            } else {
              fail("Topic is impossible to cast to Topic<TSOSessionProcessCommandHandler>")
            }
          }
          SESSION_RECONNECT_TOPIC.displayName -> {
            val reconnectSessionListener = value.castOrNull<Topic<TSOSessionReconnectHandler>>()
            if (reconnectSessionListener != null) {
              mockk<TSOSessionReconnectHandler> {
                every {
                  reconnect(any<Project>(), any<TSOConsoleView>(), any<TSOConfigWrapper>())
                } answers {
                  didTriggerReconnect = true
                  reconnectSessionSub
                    .reconnect(firstArg<Project>(), secondArg<TSOConsoleView>(), thirdArg<TSOConfigWrapper>())
                }
              }
            } else {
              fail("Topic is impossible to cast to Topic<TSOSessionReconnectHandler>")
            }
          }
          else -> fail("Unrecognized event listener: $topic")
        }
      }
    }

    beforeEach {
      didSetSelectedContent = false
      didActivate = false
      didChangeIsAvailable = false
      didTriggerShow = false
      didTriggerCreate = false
      didTriggerClose = false
      didTriggerProcessCommand = false
      didTriggerNotifyError = false
      didTriggerReconnect = false

      applicationMock = mockk<Application> {
        every { getService(NotificationsService::class.java) } returns mockk {
          every {
            notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
          } answers {
            didTriggerNotifyError = true
          }
        }
        every { messageBus } returns messageBusMock
      }

      every { ApplicationManager.getApplication() } returns applicationMock
    }

    context("init") {
      var textAvailableTriggerCount = 0

      lateinit var contentManagerListener: ContentManagerListener
      lateinit var tsoConfigWrapperMock: TSOConfigWrapper

      val contentManagerMock = mockk<ContentManager> {
        every { setSelectedContent(any<Content>(), any<Boolean>()) } answers {
          didSetSelectedContent = true
        }
        every { selectedContent } returns mockk()
        every { factory } returns mockk {
          every { createContent(any(), any(), any()) } returns mockk {
            every { preferredFocusableComponent = any() } returns Unit
          }
        }
        every { addContent(any<Content>()) } returns Unit
      }
      val projectMock = mockk<Project> {
        every { messageBus } returns messageBusMock
        every { getService(EncodingProjectManager::class.java) } returns mockk {
          every { defaultCharset } returns mockk()
        }
      }
      val processHandlerMock = mockk<ProcessHandler>(relaxUnitFun = true) {
        every {
          notifyTextAvailable(any<String>(), ProcessOutputType.STDOUT)
        } answers {
          textAvailableTriggerCount++
        }
      }
      val toolWindow = mockk<ToolWindow> {
        every { project } returns projectMock
        every { addContentManagerListener(any<ContentManagerListener>()) } answers {
          contentManagerListener = firstArg<ContentManagerListener>()
        }
        every { contentManager } returns contentManagerMock
        every {
          activate(any<Runnable>(), any<Boolean>())
        } answers {
          didActivate = true
        }
        every {
          isAvailable = any<Boolean>()
        } answers {
          didChangeIsAvailable = true
        }
        every {
          show()
        } answers {
          didTriggerShow = true
        }
      }
      val tsoWindowFactory = TSOWindowFactory()
      tsoWindowFactory.init(toolWindow)

      beforeEach {
        textAvailableTriggerCount = 0

        every { contentManagerMock.contents } returns arrayOf()
        every { contentManagerMock.selectedContent } returns mockk()
        every { contentManagerMock.removeContent(any<Content>(), any<Boolean>()) } returns true

        tsoConfigWrapperMock = mockk<TSOConfigWrapper>(relaxed = true) {
          every { getTSOResponse() } returns mockk {
            every { servletKey } returns "test_servlet_key"
            every { tsoData } returns listOf(
              mockk {
                every { tsoMessage } returns mockk {
                  every { data } returns "test_tso_response"
                }
              }
            )
          }
          every { unresponsive } returns false
          every { getConnectionConfig() } returns mockk {
            every { name } returns "test_connection_config"
          }
          every { getTSOResponseMessageQueue() } returns listOf(
            mockk {
              every { tsoPrompt } returns mockk()
            }
          )
          every { getTSOSessionConfig() } returns mockk {
            every { timeout } returns 1L
            every { maxAttempts } returns 3
          }
          every { reconnectAttempts } returns 0
        }
      }

      context("ContentManagerListener") {
        should("trigger 'contentAdded' component event") {
          val mockEvent = mockk<ContentManagerEvent> {
            every { content } returns mockk()
          }

          contentManagerListener.contentAdded(mockEvent)

          assertSoftly {
            didSetSelectedContent shouldBe true
            didActivate shouldBe true
            didChangeIsAvailable shouldBe true
            didTriggerShow shouldBe true
          }
        }
        should("trigger 'contentRemoved' component event successfully closing TSO session") {
          val closeLatch = CountDownLatch(1)

          val mockEvent = mockk<ContentManagerEvent> {
            every { content } returns mockk {
              every { component } returns mockk<TSOConsoleView> {
                every { tsoSession } returns mockk {
                  every { getTSOResponse() } returns mockk {
                    every { servletKey } returns ""
                  }
                }
              }
            }
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  closeLatch.countDown()
                  mockk<TsoResponse>()
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }

          contentManagerListener.contentRemoved(mockEvent)

          val closeLathCompleted = closeLatch.await(10, TimeUnit.SECONDS)

          assertSoftly {
            closeLathCompleted shouldBe true
            didTriggerClose shouldBe true
            didSetSelectedContent shouldBe false
            didActivate shouldBe false
            didChangeIsAvailable shouldBe true
            didTriggerShow shouldBe false
          }
        }
        should("trigger 'contentRemoved' component event closing TSO session with failure") {
          val closeLatch = CountDownLatch(1)

          val mockEvent = mockk<ContentManagerEvent> {
            every { content } returns mockk {
              every { component } returns mockk<TSOConsoleView> {
                every { tsoSession } returns mockk {
                  every { getTSOResponse() } returns mockk {
                    every { servletKey } returns ""
                  }
                }
              }
            }
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  closeLatch.countDown()
                  throw Exception("Test timeout during the request")
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }
          every { contentManagerMock.contents } returns arrayOf(mockk())

          contentManagerListener.contentRemoved(mockEvent)

          val closeLathCompleted = closeLatch.await(10, TimeUnit.SECONDS)

          assertSoftly {
            closeLathCompleted shouldBe true
            didTriggerClose shouldBe true
            didSetSelectedContent shouldBe false
            didActivate shouldBe false
            didChangeIsAvailable shouldBe false
            didTriggerShow shouldBe false
          }
        }
      }

      context("executeRexx") {
        lateinit var rexxParamsMock: ExecuteRexxAction.RexxParams

        mockkConstructor(TSOConsoleView::class)
        every { anyConstructed<TSOConsoleView>().processHandler } returns processHandlerMock

        beforeEach {
          rexxParamsMock = mockk {
            every { rexxLibrary } returns "TEST.REXX.LIBRARY"
            every { execMember } returns "TESTMEM"
            every { rexxArguments } returns listOf()
          }

          var isFirstPrompt = true
          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> mockk<TsoResponse> {
                  every { tsoData } returns listOf(
                    mockk {
                      every { tsoMessage } returns mockk {
                        every { data } returns "test_tso_response"
                      }
                      every { tsoPrompt } answers {
                        if (isFirstPrompt) {
                          isFirstPrompt = false
                          null
                        } else {
                          mockk()
                        }
                      }
                    }
                  )
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }

          every { contentManagerMock.selectedContent } returns mockk {
            every { component } returns mockk<TSOConsoleView> {
              every { processHandler } returns processHandlerMock
            }
          }
        }

        should("execute Rexx from explorer without arguments") {
          executeRexxSub.executeRexx(projectMock, tsoConfigWrapperMock, rexxParamsMock)

          assertSoftly {
            didTriggerCreate shouldBe true
            didTriggerProcessCommand shouldBe true
            textAvailableTriggerCount shouldBe 6
          }
        }
        should("execute Rexx from explorer with arguments and a prompt once") {
          var performTsoOperationCount = 0

          every { rexxParamsMock.rexxArguments } returns listOf("test_arg")

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  performTsoOperationCount++
                  mockk<TsoResponse> {
                    every { tsoData } returns listOf(
                      mockk {
                        every { tsoMessage } returns mockk {
                          every { data } returns "test_tso_response_$performTsoOperationCount"
                        }
                        every { tsoPrompt } returns mockk()
                      }
                    )
                  }
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }
          var isFirstFetchForRexx = true
          every { tsoConfigWrapperMock.getTSOResponseMessageQueue() } returns listOf(
            mockk {
              every { tsoPrompt } answers {
                if (isFirstFetchForRexx) {
                  isFirstFetchForRexx = false
                  null
                } else {
                  mockk()
                }
              }
            }
          )

          executeRexxSub.executeRexx(projectMock, tsoConfigWrapperMock, rexxParamsMock)

          assertSoftly {
            didTriggerCreate shouldBe true
            didTriggerProcessCommand shouldBe true
            textAvailableTriggerCount shouldBe 6
            performTsoOperationCount shouldBe 2
          }
        }
        should("fail to execute Rexx from explorer cause the TSO session is not created for some reason") {
          every { contentManagerMock.selectedContent } returns null

          executeRexxSub.executeRexx(projectMock, tsoConfigWrapperMock, rexxParamsMock)

          assertSoftly {
            didTriggerNotifyError shouldBe true
            didTriggerCreate shouldBe true
            didTriggerProcessCommand shouldBe false
            textAvailableTriggerCount shouldBe 2
          }
        }
      }

      context("processCommand") {
        lateinit var scheduledReconnectTask: TimerTask

        var reconnectAttemptsCount = 0
        var didCompleteReconnectTask = false
        var performTsoOperationCount = 0

        val newSingleThreadExecutorMock: () -> ScheduledExecutorService = Executors::newSingleThreadScheduledExecutor
        mockkStatic(newSingleThreadExecutorMock as KFunction<*>)
        every { Executors.newSingleThreadScheduledExecutor() } returns mockk<ScheduledExecutorService> {
          every {
            scheduleAtFixedRate(any(), 0L, any<Long>(), TimeUnit.SECONDS)
          } answers {
            scheduledReconnectTask = firstArg<TimerTask>()
            mockk()
          }
          every {
            awaitTermination(1L, TimeUnit.MINUTES)
          } answers {
            scheduledReconnectTask.run()
            didCompleteReconnectTask
          }
          every {
            shutdown()
          } answers {
            didCompleteReconnectTask = true
          }
        }

        beforeEach {
          reconnectAttemptsCount = 0
          didCompleteReconnectTask = false
          performTsoOperationCount = 0

          every {
            tsoConfigWrapperMock.incrementReconnectAttempt()
          } answers {
            reconnectAttemptsCount++
          }
          every { tsoConfigWrapperMock.clearReconnectAttempts() } returns Unit
        }

        should("fail to process a command with one failed and one successful reconnection attempts") {
          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  performTsoOperationCount++
                  if (performTsoOperationCount == 1 || performTsoOperationCount == 2) {
                    throw Exception("Test exception", Exception("This is a test"))
                  } else {
                    mockk<TsoResponse> {
                      every { servletKey } returns "test_servlet_key_new"
                      every { tsoData } returns listOf(
                        mockk {
                          every { tsoMessage } returns mockk {
                            every { data } returns "test_tso_response_$performTsoOperationCount"
                          }
                          every { tsoPrompt } returns mockk()
                        }
                      )
                    }
                  }
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }

          setPrivateFieldValue(
            tsoWindowFactory,
            TSOWindowFactory::class.java,
            "tsoSessionToConfigMap",
            mutableMapOf("test_servlet_key" to tsoConfigWrapperMock)
          )

          commandEnteredSub
            .processCommand(
              projectMock,
              tsoConsoleViewMock,
              tsoConfigWrapperMock,
              "test_command",
              mockk(),
              mockk(),
              processHandlerMock
            )

          assertSoftly {
            reconnectAttemptsCount shouldBe 2
            textAvailableTriggerCount shouldBe 8
            performTsoOperationCount shouldBe 3
            didCompleteReconnectTask shouldBe true
            didTriggerReconnect shouldBe true
          }
        }

        should("fail to process a command and fail to reconnect after cause there is no associated session by servlet key") {
          var isNotificationErrorCorrect = false
          var didSessionFailed = false
          var didProcessDestroy = false

          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  performTsoOperationCount++
                  throw Exception("Test exception", Exception("This is a test"))
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }
          every { applicationMock.getService(NotificationsService::class.java) } returns mockk {
            every {
              notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
            } answers {
              val notificationException = firstArg<Throwable>()
              if (
                notificationException is NotificationCompatibleException
                && notificationException.detailsShort == "Could not find old TSO session ID"
              ) {
                isNotificationErrorCorrect = true
              }
              didTriggerNotifyError = true
            }
          }
          every {
            tsoConfigWrapperMock.onSessionFailure(any<Throwable>())
          } answers {
            didSessionFailed = true
          }
          every { tsoConfigWrapperMock.getTSOSessionConfig() } returns mockk {
            every { timeout } returns 1L
            every { maxAttempts } returns 0
          }
          every {
            processHandlerMock.destroyProcess()
          } answers {
            didProcessDestroy = true
          }

          commandEnteredSub
            .processCommand(
              projectMock,
              tsoConsoleViewMock,
              tsoConfigWrapperMock,
              "test_command",
              mockk(),
              mockk(),
              processHandlerMock
            )

          assertSoftly {
            reconnectAttemptsCount shouldBe 1
            textAvailableTriggerCount shouldBe 5
            performTsoOperationCount shouldBe 1
            didCompleteReconnectTask shouldBe true
            didTriggerReconnect shouldBe true
            didTriggerNotifyError shouldBe true
            isNotificationErrorCorrect shouldBe true
            didSessionFailed shouldBe true
            didProcessDestroy shouldBe true
          }
        }
        should("fail to process a command and fail to reconnect to a session again as a new servlet key is null") {
          var didSessionFailedWithCorrectMessage = false
          var didProcessDestroy = false

          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> {
                  performTsoOperationCount++
                  if (performTsoOperationCount == 1) {
                    throw Exception("Test exception", Exception("This is a test"))
                  } else {
                    mockk<TsoResponse> {
                      every { servletKey } returns null
                      every { tsoData } returns listOf(
                        mockk {
                          every { tsoMessage } returns mockk {
                            every { data } returns "test_tso_response_$performTsoOperationCount"
                          }
                          every { tsoPrompt } returns mockk()
                        }
                      )
                    }
                  }
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }
          every {
            tsoConfigWrapperMock.onSessionFailure(any<Throwable>())
          } answers {
            val throwable = firstArg<Exception>()
            if (throwable.message?.contains("TSO session servletKey is null, TSO response is not correct.") == true) {
              didSessionFailedWithCorrectMessage = true
            }
          }
          every { tsoConfigWrapperMock.getTSOSessionConfig() } returns mockk {
            every { timeout } returns 1L
            every { maxAttempts } returns 0
          }
          every {
            processHandlerMock.destroyProcess()
          } answers {
            didProcessDestroy = true
          }

          setPrivateFieldValue(
            tsoWindowFactory,
            TSOWindowFactory::class.java,
            "tsoSessionToConfigMap",
            mutableMapOf("test_servlet_key" to tsoConfigWrapperMock)
          )

          commandEnteredSub
            .processCommand(
              projectMock,
              tsoConsoleViewMock,
              tsoConfigWrapperMock,
              "test_command",
              mockk(),
              mockk(),
              processHandlerMock
            )

          assertSoftly {
            reconnectAttemptsCount shouldBe 1
            textAvailableTriggerCount shouldBe 5
            performTsoOperationCount shouldBe 2
            didCompleteReconnectTask shouldBe true
            didTriggerReconnect shouldBe true
            didSessionFailedWithCorrectMessage shouldBe true
            didProcessDestroy shouldBe true
          }
        }
        should("fail to process a command cause credentials are not correct") {
          var didSessionFailedWithCorrectMessage = false
          var didProcessDestroy = false

          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> throw CredentialsNotFoundForConnectionException(mockk())
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }
          every {
            tsoConfigWrapperMock.onSessionFailure(any<Throwable>())
          } answers {
            val throwable = firstArg<Throwable>()
            if (throwable is CredentialsNotFoundForConnectionException) {
              didSessionFailedWithCorrectMessage = true
            }
          }
          every {
            processHandlerMock.destroyProcess()
          } answers {
            didProcessDestroy = true
          }

          commandEnteredSub
            .processCommand(
              projectMock,
              tsoConsoleViewMock,
              tsoConfigWrapperMock,
              "test_command",
              mockk(),
              mockk(),
              processHandlerMock
            )

          assertSoftly {
            textAvailableTriggerCount shouldBe 1
            didTriggerReconnect shouldBe false
            didSessionFailedWithCorrectMessage shouldBe true
            didProcessDestroy shouldBe true
          }
        }
      }

      context("reopen") {
        should("reopen the session with content") {
          var didRemoveContent = false

          mockkConstructor(TSOConsoleView::class)
          every { anyConstructed<TSOConsoleView>().processHandler } returns processHandlerMock

          every {
            contentManagerMock.removeContent(any<Content>(), any<Boolean>())
          } answers {
            didRemoveContent = true
            true
          }

          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> mockk<TsoResponse> {
                  every { servletKey } returns "test_servlet_key"
                  every { tsoData } returns listOf(
                    mockk {
                      every { tsoMessage } returns mockk {
                        every { data } returns "test_tso_response"
                      }
                      every { tsoPrompt } returns mockk()
                    }
                  )
                }
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }

          reopenSessionSub.reopen(projectMock, tsoConsoleViewMock)

          assertSoftly {
            didRemoveContent shouldBe true
            didTriggerCreate shouldBe true
            didTriggerNotifyError shouldBe false
            textAvailableTriggerCount shouldBe 2
          }
        }
        should("fail to reopen the session cause a new servlet key is not acquired") {
          mockkConstructor(TSOConsoleView::class)
          every { anyConstructed<TSOConsoleView>().processHandler } returns processHandlerMock

          every { contentManagerMock.selectedContent } returns null

          val tsoConsoleViewMock = mockk<TSOConsoleView>(relaxUnitFun = true) {
            every { processHandler } returns processHandlerMock
            every { tsoSession } returns tsoConfigWrapperMock
          }

          every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
            every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
              when (val operation = firstArg<Operation<*>>()) {
                is TsoOperation -> throw Exception("Test exception")
                else -> fail("Operation handler is not defined: $operation")
              }
            }
          }

          reopenSessionSub.reopen(projectMock, tsoConsoleViewMock)

          assertSoftly {
            didTriggerCreate shouldBe false
            didTriggerNotifyError shouldBe true
            textAvailableTriggerCount shouldBe 0
          }
        }
      }

      context("create") {
        should("fail to create a new session as the TSO operation returns a response without a servlet key") {
          var didTriggerCorrectNotifyError = false

          every { applicationMock.getService(NotificationsService::class.java) } returns mockk {
            every {
              notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
            } answers {
              if (arg<String>(3) == "Cannot create a new session, because new session ID was not recognized") {
                didTriggerCorrectNotifyError = true
              }
            }
          }

          val tsoConfigWrapperForCreateMock = mockk<TSOConfigWrapper> {
            every { getTSOResponse() } returns mockk {
              every { servletKey } returns null
            }
          }

          createSessionSub.create(projectMock, tsoConfigWrapperForCreateMock)

          assertSoftly {
            didTriggerCorrectNotifyError shouldBe true
          }
        }
      }
    }

    context("getTsoMessageQueue") {
      should("show an error notification when the TSO operation is not performed successfully") {
        every { applicationMock.getService(DataOpsManager::class.java) } returns mockk {
          every { performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } answers {
            when (val operation = firstArg<Operation<*>>()) {
              is TsoOperation -> throw Exception("Test exception")
              else -> fail("Operation handler is not defined: $operation")
            }
          }
        }

        getTsoMessageQueue(mockk())

        assertSoftly {
          didTriggerNotifyError shouldBe true
        }
      }
    }
  }
})
