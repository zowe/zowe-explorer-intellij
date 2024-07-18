/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.ui.build.tso

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.ProjectManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl.MockToolWindow
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.MessageData
import org.zowe.explorer.dataops.operations.MessageType
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestDataOpsManagerImpl
import org.zowe.explorer.ui.build.tso.config.TSOConfigWrapper
import org.zowe.explorer.ui.build.tso.ui.TSOConsoleView
import org.zowe.explorer.ui.build.tso.ui.TSOSessionParams
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.service
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
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

    // initialize topics to be able to call them
    classUnderTest.init(toolWindow)

    mockkObject(TSOWindowFactory.Companion)
    every { TSOWindowFactory.showSessionFailureNotification(any(), any(), any()) } just Runs

    context("Reconnect to the session") {

      should("should call showSessionFailureNotification if session was not found in map") {
        // given
        val oldSessionResponse = TsoResponse(servletKey = "test-servletKey-1")
        val oldSessionParams = TSOSessionParams(connectionConfig = connectionConfig)
        val oldSession = TSOConfigWrapper(oldSessionParams, oldSessionResponse)

        // when
        sendTopic(SESSION_RECONNECT_TOPIC).reconnect(project, console, oldSession)

        // then
        verify(exactly = 1) {
          TSOWindowFactory.showSessionFailureNotification(
            "Error getting TSO session info",
            "Could not find old TSO session ID",
            project
          )
        }
      }
    }

    context("process tso command") {

      val processHandler = spyk(NopProcessHandler())

      should("should reconnect to the tso session after unsuccessful execution of the command") {
        // given
        clearMocks(processHandler, verificationMarks = true, recordedCalls = true)
        val dataOpsManager = ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
        dataOpsManager.testInstance = object : TestDataOpsManagerImpl() {
          override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
            throw IllegalArgumentException("Error processing tso command")
          }

        }

        val oldSessionResponse = TsoResponse(servletKey = "test-servletKey-1")
        val oldSessionParams = TSOSessionParams(connectionConfig = connectionConfig)
        val session = TSOConfigWrapper(oldSessionParams, oldSessionResponse)
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
        val oldSessionParams = TSOSessionParams(connectionConfig = connectionConfig)
        val session = TSOConfigWrapper(oldSessionParams, oldSessionResponse)
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
