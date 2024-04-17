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
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.operations.MessageData
import org.zowe.explorer.dataops.operations.MessageType
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.ui.build.tso.config.TSOConfigWrapper
import org.zowe.explorer.ui.build.tso.ui.TSOConsoleView
import org.zowe.explorer.ui.build.tso.ui.TSOSessionParams
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.subscribe
import org.zowe.kotlinsdk.TsoResponse
import java.net.ConnectException

/**
 * Interface class which represents create topic handler
 */
interface TSOSessionCreateHandler {

  /**
   * Function which is called when TSO session is created
   * @param project - a root project
   * @param newSession - an instance of config wrapper for new TSO session created
   */
  fun create(project: Project, newSession: TSOConfigWrapper)
}

/**
 * Interface class which represents reconnect topic handler
 */
interface TSOSessionReconnectHandler {
  /**
   * Function which is called when we need to reconnect to the disconnected TSO session
   * @param project - a root project
   * @param console - an instance of TSO console view
   * @param oldSession - an instance of TSO session which we want to reconnect to
   */
  fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper)
}

/**
 * Interface class which represents process command topic handler
 */
interface TSOSessionProcessCommandHandler {

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
  fun processCommand(
    project: Project,
    console: TSOConsoleView,
    session: TSOConfigWrapper,
    command: String,
    messageType: MessageType,
    messageData: MessageData,
    processHandler: ProcessHandler
  )
}

/**
 * Interface class which represents close topic handler
 */
interface TSOSessionCloseHandler {

  /**
   * Function which is called when we want to close currently running TSO session
   * @param project - a root project
   * @param session - an instance of TSO session we want to close
   * @throws CallException if any z/OSMF error occurred
   */
  @Throws(CallException::class, ConnectException::class)
  fun close (project: Project, session: TSOConfigWrapper)
}

/**
 * Interface class which represents reopen topic handler
 */
interface TSOSessionReopenHandler {
  /**
   * Function which is called when reopen session event is triggered
   * @param project
   * @param console
   */
  fun reopen(project: Project, console: TSOConsoleView)
}

@JvmField
val SESSION_ADDED_TOPIC = Topic.create("tsoSessionAdded", TSOSessionCreateHandler::class.java)

@JvmField
val SESSION_RECONNECT_TOPIC = Topic.create("tsoSessionReconnect", TSOSessionReconnectHandler::class.java)

@JvmField
val SESSION_COMMAND_ENTERED = Topic.create("tsoSessionCommandTyped", TSOSessionProcessCommandHandler::class.java)

@JvmField
val SESSION_CLOSED_TOPIC = Topic.create("tsoSessionClosed", TSOSessionCloseHandler::class.java)

@JvmField
val SESSION_REOPEN_TOPIC = Topic.create("tsoSessionReopen", TSOSessionReopenHandler::class.java)

/**
 * Factory class for building an instance of TSO tool window when TSO session is created
 */
class TSOWindowFactory : ToolWindowFactory {

  /**
   * Static companion object for TSO tool window class
   */
  companion object {

    private var currentTsoSession: TSOConfigWrapper? = null
    private val tsoSessionToConfigMap = mutableMapOf<String, TSOSessionParams>()

    /**
     * Method is used to parse response for every TSO session created
     * @param tsoResponse - response from TSO session
     * @return String message
     */
    fun parseTSODataResponse(tsoResponse: TsoResponse): String {
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

    /**
     * Method is used to get all messages in queue for particular TSO session
     * @param session - TSO session config wrapper instance
     * @return an instance of TSO response
     * @throws Exception if operation is not successful
     */
    fun getTsoMessageQueue(session: TSOConfigWrapper): TsoResponse {
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

      val content = contentManager.factory.createContent(
        tsoContent,
        tsoSession.getConnectionConfig().name + " TSO session. ServletKey: " + tsoSession.getTSOResponse().servletKey,
        false
      )
      contentManager.addContent(content)
      contentManager.setSelectedContent(content)

      fetchNewSessionResponseMessages(tsoContent, tsoSession)
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
   * Init method which is called first. Adds basic listeners and subscribe on topics
   */
  override fun init(toolWindow: ToolWindow) {
    val project = toolWindow.project

    toolWindow.addContentManagerListener(object : ContentManagerListener {
      override fun contentRemoved(event: ContentManagerEvent) {
        val component = event.content.component as TSOConsoleView
        val session = component.getTsoSession()
        sendTopic(SESSION_CLOSED_TOPIC, project).close(project, session)
      }
    })

    subscribe(
      project = project,
      topic = SESSION_ADDED_TOPIC,
      handler = object : TSOSessionCreateHandler {

        override fun create(project: Project, newSession: TSOConfigWrapper) {
          val servletKey = newSession.getTSOResponse().servletKey ?: throw Exception("Cannot create a new session, because new session ID was not recognized.")
          addToolWindowContent(project, toolWindow, newSession)
          tsoSessionToConfigMap[servletKey] = newSession.getTSOSessionParams()
        }
      }
    )

    subscribe(
      project = project,
      topic = SESSION_RECONNECT_TOPIC,
      handler = object : TSOSessionReconnectHandler {

        override fun reconnect(project: Project, console: TSOConsoleView, oldSession: TSOConfigWrapper) {
          val oldServletKey = oldSession.getTSOResponse().servletKey
          if (tsoSessionToConfigMap[oldServletKey] != null) {
            val newSessionConfig = createNewSessionFromOldConfig(oldSession)
            if (newSessionConfig != null) {
              val sessionResponse = newSessionConfig.getTSOResponse()
              val newServletKey = sessionResponse.servletKey ?: throw Exception("Cannot reconnect to the session, because new session ID was not recognized.")
              currentTsoSession = newSessionConfig
              console.setTsoSession(newSessionConfig)
              fetchNewSessionResponseMessages(console, newSessionConfig)
              tsoSessionToConfigMap.remove(oldServletKey)
              tsoSessionToConfigMap[newServletKey] = newSessionConfig.getTSOSessionParams()
            } else {
              throw Exception("Cannot reconnect to the session, because new session config is missing.")
            }
          } else {
            throw Exception("Cannot reconnect to the session, because session ID was not found.")
          }
        }
      }
    )
    subscribe(
      project = project,
      topic = SESSION_COMMAND_ENTERED,
      handler = object : TSOSessionProcessCommandHandler {

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
            processHandler.notifyTextAvailable(
              "Unsuccessful execution of the TSO request. Session already closed.\n",
              ProcessOutputType.STDOUT
            )
            processHandler.notifyTextAvailable("Attempting to reconnect...\n", ProcessOutputType.STDOUT)
            try {
              sendTopic(SESSION_RECONNECT_TOPIC, project).reconnect(project, console, session)
              processHandler.notifyTextAvailable(
                "Successfully reconnected to the TSO session.\nREADY\n",
                ProcessOutputType.STDOUT
              )
            } catch (e: Exception) {
              throw e
            }
          }
        }
      }
    )
    subscribe(
      project = project,
      topic = SESSION_CLOSED_TOPIC,
      handler = object : TSOSessionCloseHandler {

        override fun close(project: Project, session: TSOConfigWrapper) {
          /**
           * z/OSMF can be down due to VPN issues (if any) which causes the IDE to be frozen for ~20-30 seconds while closing the toolWindow content.
           * We don't want this behavior, so let's run the request in the separate thread.
           * If it fails, do not report the failure as IDE error and just remove the failed session from the session map
           */
          val closeThread = Thread(object : Runnable {
            override fun run() {
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
          })
          closeThread.start()
        }
      }
    )

    subscribe(
      project = project,
      topic = SESSION_REOPEN_TOPIC,
      handler = object : TSOSessionReopenHandler {
        override fun reopen(project: Project, console: TSOConsoleView) {
          val oldConfig = console.getTsoSession()
          runInEdt {
            toolWindow.contentManager.apply {
              selectedContent?.let { removeContent(it, true) }
            }
          }
          val newConfig = createNewSessionFromOldConfig(oldConfig) ?: throw Exception("Unable to establish a new TSO session with parameters: ${oldConfig.getTSOSessionParams()}")
          sendTopic(SESSION_ADDED_TOPIC).create(project, newConfig)
        }
      }
    )
  }

  /**
   * Method is used to create a new session config from the parameters of the old session config
   * @param oldConfig
   * @return a new instance of TSOConfigWrapper if a new session was successfully created, null otherwise
   */
  private fun createNewSessionFromOldConfig(oldConfig: TSOConfigWrapper) : TSOConfigWrapper? {
    val params = oldConfig.getTSOSessionParams()
    val tsoResponse = service<DataOpsManager>().performOperation(
      TsoOperation(
        params,
        TsoOperationMode.START
      )
    )
    val servletKey = tsoResponse.servletKey
    return if (servletKey != null) TSOConfigWrapper(params, tsoResponse) else null
  }

  /**
   * Method is used to fetch and display the welcome messages when a new session is created and a tool window content is added
   * @param console
   * @param newConfig
   */
  private fun fetchNewSessionResponseMessages(console: TSOConsoleView, newConfig: TSOConfigWrapper) {
    val sessionResponse = newConfig.getTSOResponse()
    val processHandler = console.getProcessHandler()
    processHandler
      .notifyTextAvailable(parseTSODataResponse(sessionResponse), ProcessOutputType.STDOUT)
    while (newConfig.getTSOResponseMessageQueue().last().tsoPrompt == null) {
      val response = getTsoMessageQueue(newConfig)
      processHandler
        .notifyTextAvailable(parseTSODataResponse(response), ProcessOutputType.STDOUT)
      newConfig.setTSOResponseMessageQueue(response.tsoData)
    }
    processHandler.notifyTextAvailable("> ", ProcessOutputType.STDOUT)
  }

}
