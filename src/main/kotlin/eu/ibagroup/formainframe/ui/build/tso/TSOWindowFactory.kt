/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.ui.build.tso

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOConsoleView
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionParams
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.r2z.TsoResponse
import java.net.ConnectException

/**
 * Interface class which represents callable methods for topics
 */
interface TSOSessionHandler {

  /**
   * Function which is called when TSO session is created
   * @param project - a root project
   * @param newSession - an instance of config wrapper for new TSO session created
   */
  fun create (project: Project, newSession: TSOConfigWrapper)

  /**
   * Function which is called when we need to reconnect to the disconnected TSO session
   * @param project - a root project
   * @param console - an instance of TSO console view
   * @param oldSession - an instance of TSO session which we want to reconnect to
   */
  fun reconnect (project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper)

  /**
   * Function which is called when we want to process an entered command.
   * It sends the request to z/OSMF to process this command and returns the result
   * @param project - a root project
   * @param console - an instance of TSO console view where we entered the command
   * @param session - an instance of TSO config wrapper for which we want to execute the command
   * @param command - a command to be executed
   * @param messageType - message type we want to execute
   * @param messageData - message data we want to execute
   * @param processHandler - process handler to display the result of response message in tool window
   */
  fun processCommand (
    project: Project,
    console: TSOConsoleView,
    session: TSOConfigWrapper,
    command: String,
    messageType: MessageType,
    messageData: MessageData,
    processHandler: ProcessHandler
  )

  /**
   * Function which is called when we want to close currently running TSO session
   * @param project - a root project
   * @param session - an instance of TSO session we want to close
   * @throws CallException if any z/OSMF error occurred
   */
  @Throws(CallException::class, ConnectException::class)
  fun close (project: Project, session: TSOConfigWrapper)
}

@JvmField
val SESSION_ADDED_TOPIC = Topic.create("tsoSessionAdded", TSOSessionHandler::class.java)

@JvmField
val SESSION_RECONNECT_TOPIC = Topic.create("tsoSessionReconnect", TSOSessionHandler::class.java)

@JvmField
val SESSION_COMMAND_ENTERED = Topic.create("tsoSessionCommandTyped", TSOSessionHandler::class.java)

@JvmField
val SESSION_CLOSED_TOPIC = Topic.create("tsoSessionClosed", TSOSessionHandler::class.java)

/**
 * Factory class for building an instance of TSO tool window when TSO session is created
 */
class TSOWindowFactory : ToolWindowFactory {

  /**
   * Static companion object for TSO tool window class
   */
  companion object {

    private var currentTsoSession : TSOConfigWrapper? = null
    private val tsoSessionToConfigMap = mutableMapOf<String, TSOSessionParams>()

    /**
     * Getter for TSO session wrapper class for each TSO session created
     */
    fun getTsoSession() : TSOConfigWrapper? {
      return currentTsoSession
    }

    /**
     * Getter for TSO config map which contains all the TSO session currently created and active
     */
    fun getTsoSessionConfigMap() : Map<String, TSOSessionParams> {
      return tsoSessionToConfigMap
    }

    /**
     * Method is used to parse response for every TSO session created
     * @param tsoResponse - response from TSO session
     * @return String message
     */
    fun parseTSODataResponse(tsoResponse: TsoResponse) : String {
      val tsoData = tsoResponse.tsoData
      var message = ""
      tsoData.forEach {
        if (it.tsoMessage?.data != null) {
          message += (it.tsoMessage?.data)
          message += "\n"
        }
      }
      return message
    }
  }

  /**
   * Method is called to initialize TSO session console view. It registers content and displays it
   * @param project - a root project
   * @param toolWindow - a tool window instance
   * @param tsoSession - TSO session wrapper instance
   * @return Void
   */
  fun addToolWindowContent(project: Project, toolWindow: ToolWindow, tsoSession: TSOConfigWrapper) {
    runInEdt {
      toolWindow.setAvailable(true, null)
      toolWindow.activate(null)
      toolWindow.show(null)
      val contentManager = toolWindow.contentManager

      val tsoContent = TSOConsoleView(project, tsoSession)

      val content = contentManager.factory.createContent(tsoContent, tsoSession.getConnectionConfig().name + " TSO session. ServletKey: "
          + tsoSession.getTSOResponse().servletKey, false)
      contentManager.addContent(content)
      contentManager.setSelectedContent(content)

      val component = toolWindow.contentManager.selectedContent?.component as TSOConsoleView
      val processHandler = component.getProcessHandler()
      processHandler.notifyTextAvailable(parseTSODataResponse(tsoSession.getTSOResponse()), ProcessOutputType.STDOUT)

      while (tsoSession.getTSOResponseMessageQueue().last().tsoPrompt == null) {
        val response = getTsoMessageQueue(tsoSession)
        processHandler.notifyTextAvailable(parseTSODataResponse(response), ProcessOutputType.STDOUT)
        tsoSession.setTSOResponseMessageQueue(response.tsoData)
      }
      processHandler.notifyTextAvailable("> ", ProcessOutputType.STDOUT)
    }
  }

  /**
   * Method is used to create the content of tool window. Defined in superclass
   */
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
  }

  /**
   * Superclass method determines if tool window should be available in UI
   */
  override fun shouldBeAvailable(project: Project): Boolean {
    return false
  }

  /**
   * Method is used to get all messages in queue for particular TSO session
   * @param session - TSO session config wrapper instance
   * @return an instance of TSO response
   * @throws Exception if operation is not successful
   */
  private fun getTsoMessageQueue(session: TSOConfigWrapper) : TsoResponse {
    runCatching {
      service<DataOpsManager>().performOperation(
        TsoOperation(
          state = session,
          mode = TsoOperationMode.GET_MESSAGES
        )
      )
    }.onSuccess { return it }.onFailure { throw it }
    return TsoResponse()
  }

  /**
   * Init method which is called first. Adds basic listeners and subscribe on topics
   */
  override fun init(toolWindow: ToolWindow) {

    toolWindow.addContentManagerListener(object: ContentManagerListener {
      override fun contentRemoved(event: ContentManagerEvent) {
        val component = event.content.component as TSOConsoleView
        val session = component.getTsoSession()
        sendTopic(SESSION_CLOSED_TOPIC).close(toolWindow.project, session)
      }
    })

    subscribe(SESSION_ADDED_TOPIC,
      object: TSOSessionHandler {

        override fun create(project: Project, newSession: TSOConfigWrapper) {
          val servletKey = newSession.getTSOResponse().servletKey
          if(servletKey != null) {
            tsoSessionToConfigMap[servletKey] = newSession.getTSOSessionParams()
            addToolWindowContent(project, toolWindow, newSession)
          }
        }

        override fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper) { }

        override fun processCommand(
          project: Project,
          console: TSOConsoleView,
          session: TSOConfigWrapper,
          command: String,
          messageType: MessageType,
          messageData: MessageData,
          processHandler: ProcessHandler
        ) { }

        override fun close(project: Project, session: TSOConfigWrapper) { }
      }
    )
    subscribe(SESSION_RECONNECT_TOPIC,
      object : TSOSessionHandler {

        override fun create(project: Project, newSession: TSOConfigWrapper) { }

        override fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper) {
          val oldServletKey = oldSession.getTSOResponse().servletKey
          val params = oldSession.getTSOSessionParams()
          if (tsoSessionToConfigMap[oldServletKey] != null) {
            tsoSessionToConfigMap.remove(oldServletKey)
            val tsoResponse = service<DataOpsManager>().performOperation(
              TsoOperation(
                params,
                TsoOperationMode.START
              )
            )
            val servletKey = tsoResponse.servletKey
            if (servletKey != null) {
              val config = TSOConfigWrapper(params, tsoResponse)
              currentTsoSession = config
              console.setTsoSession(config)
              tsoSessionToConfigMap[servletKey] = config.getTSOSessionParams()
              console.getProcessHandler()
                .notifyTextAvailable(parseTSODataResponse(config.getTSOResponse()), ProcessOutputType.STDOUT)
              while (config.getTSOResponseMessageQueue().last().tsoPrompt == null) {
                val response = getTsoMessageQueue(config)
                console.getProcessHandler()
                  .notifyTextAvailable(parseTSODataResponse(response), ProcessOutputType.STDOUT)
                config.setTSOResponseMessageQueue(response.tsoData)
              }
            } else {
              throw Exception("Cannot reconnect to the session, because new session ID was not recognized.")
            }
          } else {
            throw Exception("Cannot reconnect to the session, because session ID was not found.")
          }
        }

        override fun processCommand(
          project: Project,
          console: TSOConsoleView,
          session: TSOConfigWrapper,
          command: String,
          messageType: MessageType,
          messageData: MessageData,
          processHandler: ProcessHandler
        ) { }

        override fun close(project: Project, session: TSOConfigWrapper) { }
      }
    )
    subscribe(SESSION_COMMAND_ENTERED,
      object: TSOSessionHandler {

        override fun create(project: Project, newSession: TSOConfigWrapper) { }

        override fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper) { }

        override fun processCommand(
          project: Project,
          console: TSOConsoleView,
          session: TSOConfigWrapper,
          command: String,
          messageType: MessageType,
          messageData: MessageData,
          processHandler: ProcessHandler
        ) {
          runCatching {
            service<DataOpsManager>().performOperation(
              TsoOperation(
                state = session,
                mode = TsoOperationMode.SEND_MESSAGE,
                messageType = messageType,
                messageData = messageData,
                message = command
              )
            )

          }.onSuccess {
            processHandler.notifyTextAvailable(parseTSODataResponse(it), ProcessOutputType.STDOUT)
            var response = it
            while (response.tsoData.last().tsoPrompt == null) {
              response = getTsoMessageQueue(session)
              processHandler.notifyTextAvailable(parseTSODataResponse(response), ProcessOutputType.STDOUT)
            }
          }.onFailure {
            processHandler.notifyTextAvailable("Unsuccessful execution of the TSO request. Session already closed.\n", ProcessOutputType.STDOUT)
            processHandler.notifyTextAvailable("Attempting to reconnect...\n", ProcessOutputType.STDOUT)
            try {
              sendTopic(SESSION_RECONNECT_TOPIC).reconnect(project, console, session)
              processHandler.notifyTextAvailable("Successfully reconnected to the TSO session.\n", ProcessOutputType.STDOUT)
            } catch (e : Exception) {
              throw e
            }
          }
        }

        override fun close(project: Project, session: TSOConfigWrapper) { }
      }
    )
    subscribe(SESSION_CLOSED_TOPIC,
      object : TSOSessionHandler {
        override fun create(project: Project, newSession: TSOConfigWrapper) { }

        override fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper) { }

        override fun processCommand(
          project: Project,
          console: TSOConsoleView,
          session: TSOConfigWrapper,
          command: String,
          messageType: MessageType,
          messageData: MessageData,
          processHandler: ProcessHandler
        ) { }

        override fun close(project: Project, session: TSOConfigWrapper) {
          runCatching {
            service<DataOpsManager>().performOperation(
              TsoOperation(
                state = session,
                mode = TsoOperationMode.STOP
              )
            )
          }.onSuccess {
            tsoSessionToConfigMap.remove(session.getTSOResponse().servletKey)
          }.onFailure {
            // We don't want exception to be thrown in this case which will be reported as IDE error. Just remove the session from config map
            tsoSessionToConfigMap.remove(session.getTSOResponse().servletKey)
          }
        }
      }
    )
  }
}