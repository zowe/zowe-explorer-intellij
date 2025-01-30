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

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.PossiblyDumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.exceptions.CredentialsNotFoundForConnectionException
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.explorer.actions.rexx.ExecuteRexxAction
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.tso.ui.TSOConsoleView
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.subscribe
import org.zowe.kotlinsdk.TsoResponse
import java.net.ConnectException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
  fun close(project: Project, session: TSOConfigWrapper)
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

/**
 * Interface class which represents execute rexx from explorer topic handler
 */
interface TSOSessionExecuteRexxFromExplorerHandler {
  /**
   * Function which is called when execute rexx event is triggered
   * @param project
   * @param tsoConfig
   * @param rexxConfig
   */
  fun executeRexx(project: Project, tsoConfig: TSOConfigWrapper, rexxConfig: ExecuteRexxAction.RexxParams)
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

@JvmField
val SESSION_EXECUTE_REXX_TOPIC = Topic.create("tsoSessionExecuteRexx", TSOSessionExecuteRexxFromExplorerHandler::class.java)

const val SESSION_RECONNECT_ERROR_MESSAGE = "Unable to reconnect to the TSO session. Session is closed."

/**
 * Factory class for building an instance of TSO tool window when TSO session is created
 */
class TSOWindowFactory : ToolWindowFactory, PossiblyDumbAware, DumbAware {

  private var currentTsoSession: TSOConfigWrapper? = null
  private val tsoSessionToConfigMap = mutableMapOf<String, TSOSessionConfig>()

  /**
   * Method is called to initialize TSO session console view. It registers content and displays it
   * @param project - a root project
   * @param toolWindow - a tool window instance
   * @param tsoSession - TSO session wrapper instance
   * @return Void
   */
  fun addToolWindowContent(project: Project, toolWindow: ToolWindow, tsoSession: TSOConfigWrapper) {
    runInEdt {
      val contentManager = toolWindow.contentManager

      val tsoContent = TSOConsoleView(project, tsoSession)

      val content = contentManager.factory.createContent(
        tsoContent,
        tsoSession.getConnectionConfig().name + " TSO session. ServletKey: " + tsoSession.getTSOResponse().servletKey,
        false
      ).apply {
        preferredFocusableComponent = tsoContent.preferredFocusableComponent
      }

      contentManager.addContent(content)

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
        val contentManager = toolWindow.contentManager
        val component = event.content.component as TSOConsoleView
        val session = component.getTsoSession()
        wrapInlineCall { sendTopic(SESSION_CLOSED_TOPIC, project).close(project, session) }
        if (contentManager.contents.isEmpty()) toolWindow.isAvailable = false
      }

      override fun contentAdded(event: ContentManagerEvent) {
        val contentManager = toolWindow.contentManager
        contentManager.setSelectedContent(event.content, true)
        toolWindow.apply {
          activate(null, true)
          isAvailable = true
          show()
        }
      }
    })

    subscribe(
      project = project,
      topic = SESSION_ADDED_TOPIC,
      handler = object : TSOSessionCreateHandler {

        override fun create(project: Project, newSession: TSOConfigWrapper) {
          val servletKey = newSession.getTSOResponse().servletKey?.let {
            addToolWindowContent(project, toolWindow, newSession)
            tsoSessionToConfigMap[it] = newSession.getTSOSessionConfig()
            it
          }
          if (servletKey.isNullOrBlank()) {
            NotificationsService
              .errorNotification(
                Exception(),
                project = project,
                custTitle = "Error getting TSO session servletKey",
                custDetailsShort = "Cannot create a new session, because new session ID was not recognized"
              )
          }
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
            tryToStartNewSessionFromOldConfig(oldSession)?.let {
              val sessionResponse = it.getTSOResponse()
              val newServletKey =
                sessionResponse.servletKey ?: throw Exception("TSO session servletKey must not be null.")
              currentTsoSession = it
              console.setTsoSession(it)
              fetchNewSessionResponseMessages(console, it)
              tsoSessionToConfigMap.remove(oldServletKey)
              tsoSessionToConfigMap[newServletKey] = it.getTSOSessionConfig()
            }
          } else {
            NotificationsService
              .errorNotification(
                Exception(),
                project = project,
                custTitle = "Error getting TSO session info",
                custDetailsShort = "Could not find old TSO session ID"
              )
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
            DataOpsManager.getService().performOperation(
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
            if (it is CredentialsNotFoundForConnectionException) {
              processHandler.notifyTextAvailable(
                "Unable to obtain the connection information for connection=${session.getConnectionConfig()}.\n Session will be closed.",
                ProcessOutputType.STDOUT
              )
              session.onSessionFailure(it)
              processHandler.destroyProcess()
            } else {
              processHandler.notifyTextAvailable(
                "Unsuccessful execution of the TSO request. Connection was broken.\n",
                ProcessOutputType.STDOUT
              )
              executeTsoReconnectWithTimeout(
                timeout = session.getTSOSessionConfig().timeout,
                maxAttempts = session.getTSOSessionConfig().maxAttempts,
                tsoConsole = console
              ) {
                wrapInlineCall { sendTopic(SESSION_RECONNECT_TOPIC, project).reconnect(project, console, session) }
              }
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
                DataOpsManager.getService().performOperation(
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
          runCatching {
            tryToStartNewSessionFromOldConfig(oldConfig)?.let {
              wrapInlineCall { sendTopic(SESSION_ADDED_TOPIC).create(project, it) }
            }
          }.onFailure {
            NotificationsService.errorNotification(
              it,
              project = project,
              custTitle = "Error starting a new TSO session"
            )
          }
        }
      }
    )

    subscribe(
      project = project,
      topic = SESSION_EXECUTE_REXX_TOPIC,
      handler = object : TSOSessionExecuteRexxFromExplorerHandler {
        override fun executeRexx(
          project: Project,
          tsoConfig: TSOConfigWrapper,
          rexxConfig: ExecuteRexxAction.RexxParams
        ) {
          sendTopic(SESSION_ADDED_TOPIC, project).create(project, tsoConfig)
          val selectedSessionContent = toolWindow.contentManager.selectedContent
          if (selectedSessionContent != null) {
            var commandToExecute = "EXEC '${rexxConfig.rexxLibrary}(${rexxConfig.execMember})'"
            var passedArguments = ""
            val consoleView = selectedSessionContent.component as TSOConsoleView
            val processHandler = consoleView.getProcessHandler()

            if (rexxConfig.rexxArguments.isNotEmpty()) {
              rexxConfig.rexxArguments.forEach { passedArguments += "$it," }
                .also { passedArguments = passedArguments.removeSuffix(",") }
              commandToExecute += " '$passedArguments'"
            }

            processHandler.notifyTextAvailable(commandToExecute + "\n", ProcessOutputType.STDOUT)
            sendTopic(SESSION_COMMAND_ENTERED).processCommand(
              project,
              consoleView,
              tsoConfig,
              commandToExecute,
              MessageType.TSO_RESPONSE,
              MessageData.DATA_DATA,
              processHandler
            )
            processHandler.notifyTextAvailable("> ", ProcessOutputType.STDOUT)
          } else {
            NotificationsService
              .errorNotification(
                Exception(),
                project = project,
                custTitle = "Error getting TSO runtime session",
                custDetailsShort = "Cannot execute REXX program, because we cannot find TSO session. Please retry"
              )
          }
        }
      }
    )
  }

  /**
   * Wrapper function to call a block of inline code
   */
  fun wrapInlineCall(inlineBlock: () -> Unit) {
    inlineBlock()
  }

  /**
   * Method is used to start a new session from the parameters of the old session config
   * @param oldConfig
   * @return a new instance of TSOConfigWrapper if a new session was successfully created, null otherwise
   */
  private fun tryToStartNewSessionFromOldConfig(oldConfig: TSOConfigWrapper): TSOConfigWrapper? {
    var newConfig: TSOConfigWrapper? = null
    val tsoSessionConfig = oldConfig.getTSOSessionConfig()
    runCatching {
      DataOpsManager.getService().performOperation(
        TsoOperation(
          TSOConfigWrapper(tsoSessionConfig, oldConfig.getConnectionConfig()),
          TsoOperationMode.START
        )
      )
    }.onSuccess {
      if (it.servletKey != null) newConfig = TSOConfigWrapper(tsoSessionConfig, oldConfig.getConnectionConfig(), it)
    }.onFailure {
      throw it
    }
    return newConfig
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

  /**
   * Function tries to reconnect to the TSO session with specified timeout and attempts if the connection was broken for some reason.
   * @param timeout - the time interval between executing each reconnect attempt
   * @param maxAttempts - the number of attempts to make
   * @param tsoConsole
   * @param block - the function to call
   */
  private fun executeTsoReconnectWithTimeout(
    timeout: Long,
    maxAttempts: Int,
    tsoConsole: TSOConsoleView,
    block: () -> Unit
  ) {
    val processHandler = tsoConsole.getProcessHandler()
    processHandler.notifyTextAvailable(
      "Attempting to reconnect $maxAttempts times with timeout $timeout(s) each respectively...\n",
      ProcessOutputType.STDOUT
    )
    var isTaskComplete = false
    val scheduledService = Executors.newSingleThreadScheduledExecutor()
    val tsoReconnectTask = createExecutableTask(scheduledService, tsoConsole, block, maxAttempts)
    scheduledService.scheduleAtFixedRate(tsoReconnectTask, 0L, timeout, TimeUnit.SECONDS)
    while (!isTaskComplete) {
      isTaskComplete = scheduledService.awaitTermination(1L, TimeUnit.MINUTES)
    }
  }

  /**
   * Method creates an executable reconnect task with the specified block to execute
   * @param service
   * @param tsoConsole
   * @param block
   * @param maxAttempts
   * @return an instance of @see TsoReconnectTask
   */
  private fun createExecutableTask(
    service: ScheduledExecutorService,
    tsoConsole: TSOConsoleView,
    block: () -> Unit,
    maxAttempts: Int
  ): TimerTask {
    return object : TsoReconnectTask(service) {
      override fun run() {
        val tsoSession = tsoConsole.getTsoSession()
        val processHandler = tsoConsole.getProcessHandler()
        runCatching {
          tsoSession.incrementReconnectAttempt()
          processHandler.notifyTextAvailable(
            "Trying to connect (attempt ${tsoSession.reconnectAttempts} of $maxAttempts)...\n",
            ProcessOutputType.STDOUT
          )
          block()
        }.onSuccess {
          processHandler.notifyTextAvailable(
            "Successfully reconnected to the TSO session.\nREADY\n",
            ProcessOutputType.STDOUT
          )
          tsoSession.clearReconnectAttempts()
          cancel()
          service.shutdown()
        }.onFailure { throwable ->
          val cause = throwable.cause.toString()
          processHandler.notifyTextAvailable(
            "Failed to reconnect. The error message is:\n $cause\n",
            ProcessOutputType.STDOUT
          )
          if (tsoSession.reconnectAttempts == maxAttempts) {
            processHandler.notifyTextAvailable(
              SESSION_RECONNECT_ERROR_MESSAGE,
              ProcessOutputType.STDOUT
            )
            tsoSession.onSessionFailure(throwable)
            processHandler.destroyProcess()
            cancel()
            service.shutdown()
          }
        }
      }
    }
  }

  /**
   * Identifies that tool window is dumb aware
   */
  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Class represents reconnect runnable task to be submitted in case of any connection problems.
   * It requires an overriden @see TimerTask.run() method
   */
  abstract class TsoReconnectTask(val service: ScheduledExecutorService) : TimerTask()

}

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
    DataOpsManager.getService().performOperation(
      TsoOperation(
        state = session,
        mode = TsoOperationMode.GET_MESSAGES
      )
    )
  }.onSuccess { return it }
    .onFailure {
      NotificationsService.getService().notifyError(it, custTitle = "Error getting TSO response messages")
    }
  return TsoResponse()
}