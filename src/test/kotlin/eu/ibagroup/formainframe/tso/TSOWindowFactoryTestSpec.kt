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

package eu.ibagroup.formainframe.tso

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestNotificationsServiceImpl
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.tso.ui.TSOConsoleView
import eu.ibagroup.formainframe.utils.sendTopic
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.zowe.kotlinsdk.TsoResponse

class TSOWindowFactoryTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("TSOWindowFactory test spec") {

    val connectionConfig = mockk<ConnectionConfig>()
    val console = mockk<TSOConsoleView>()
    val project = ProjectManager.getInstance().defaultProject
    val toolWindow = spyk(MockToolWindow(project))
    val classUnderTest = spyk(TSOWindowFactory(), recordPrivateCalls = true)

    val tsoSessionConfig = mockk<TSOSessionConfig>()
    every { tsoSessionConfig.timeout } returns 10
    every { tsoSessionConfig.maxAttempts } returns 3

    // initialize topics to be able to call them
    classUnderTest.init(toolWindow)

    val notificationsService = NotificationsService.getService() as TestNotificationsServiceImpl

    context("Reconnect to the session") {

      should("should call notifyError if session was not found in map") {
        var isCorrectErrorNotificationTriggered = false
        var triggerCount = 0

        val oldSessionResponse = TsoResponse(servletKey = "test-servletKey-1")
        val oldSession = TSOConfigWrapper(tsoSessionConfig, connectionConfig, oldSessionResponse)

        notificationsService.testInstance = object : TestNotificationsServiceImpl() {
          override fun notifyError(
            t: Throwable,
            project: Project?,
            custTitle: String?,
            custDetailsShort: String?,
            custDetailsLong: String?
          ) {
            if (custTitle == "Error getting TSO session info" && custDetailsShort == "Could not find old TSO session ID") {
              isCorrectErrorNotificationTriggered = true
              triggerCount += 1
            }
          }
        }

        sendTopic(SESSION_RECONNECT_TOPIC).reconnect(project, console, oldSession)

        assertSoftly { isCorrectErrorNotificationTriggered shouldBe true }
        assertSoftly { triggerCount shouldBe 1 }
      }
    }

    context("process tso command") {

      val processHandler = spyk(NopProcessHandler())

      should("should reconnect to the tso session after unsuccessful execution of the command") {
        // given
        clearMocks(processHandler, verificationMarks = true, recordedCalls = true)
        val dataOpsManager = DataOpsManager.getService() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalArgumentException("Error processing tso command")
          }

        }

        val oldSessionResponse = TsoResponse(servletKey = "test-servletKey-1")
        val session = TSOConfigWrapper(tsoSessionConfig, connectionConfig, oldSessionResponse)
        val command = "TIME"
        val messageType = mockk<MessageType>()
        val messageData = mockk<MessageData>()

        every { console.getProcessHandler() } returns processHandler
        every { console.getTsoSession() } returns session
        every { processHandler.notifyTextAvailable(any(), any()) } just Runs

        val capturedFunc = slot<() -> Unit>()
        every { classUnderTest.wrapInlineCall(capture(capturedFunc)) } just Runs

        // when
        sendTopic(SESSION_COMMAND_ENTERED).processCommand(
          project,
          console,
          session,
          command,
          messageType,
          messageData,
          processHandler
        )

        // then
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Unsuccessful execution of the TSO request. Connection was broken.\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Attempting to reconnect 3 times with timeout 10(s) each respectively...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Trying to connect (attempt 1 of 3)...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Successfully reconnected to the TSO session.\n" + "READY\n",
            ProcessOutputType.STDOUT
          )
        }
      }

      should("should not reconnect 3 times to the tso session after unsuccessful execution of the command") {
        // given
        clearMocks(processHandler, verificationMarks = true, recordedCalls = true)
        val errorZOSMFMessage = "Error reconnecting to the tso session"
        val cause = IllegalStateException(errorZOSMFMessage)
        val capturedFunc = slot<() -> Unit>()
        every { classUnderTest.wrapInlineCall(capture(capturedFunc)) } answers {
          throw IllegalStateException(cause)
        }

        val oldSessionResponse = TsoResponse(servletKey = "test-servletKey-1")
        val session = TSOConfigWrapper(tsoSessionConfig, connectionConfig, oldSessionResponse)
        val consoleView = mockk<TerminalExecutionConsole>()
        val widget = mockk<JBTerminalWidget>()

        every { consoleView.terminalWidget } returns widget
        every { widget.stop() } just Runs
        every { console.getProcessHandler() } returns processHandler
        every { console.getTsoSession() } returns session
        every { console.getTerminalConsole() } returns consoleView
        val command = "TIME"
        val messageType = mockk<MessageType>()
        val messageData = mockk<MessageData>()

        // when
        sendTopic(SESSION_COMMAND_ENTERED).processCommand(
          project,
          console,
          session,
          command,
          messageType,
          messageData,
          processHandler
        )

        // then
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Unsuccessful execution of the TSO request. Connection was broken.\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Attempting to reconnect 3 times with timeout 10(s) each respectively...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Trying to connect (attempt 1 of 3)...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Trying to connect (attempt 2 of 3)...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            "Trying to connect (attempt 3 of 3)...\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 3) {
          processHandler.notifyTextAvailable(
            "Failed to reconnect. The error message is:\n $cause\n",
            ProcessOutputType.STDOUT
          )
        }
        verify(exactly = 1) {
          processHandler.notifyTextAvailable(
            SESSION_RECONNECT_ERROR_MESSAGE,
            ProcessOutputType.STDOUT
          )
        }

        assertSoftly {
          session.unresponsive shouldBe true
        }
      }
    }
  }

})