/*
 * Copyright (c) 2024 IBA Group.
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

package eu.ibagroup.formainframe.explorer.actions.rexx

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.*
import com.intellij.openapi.progress.Task.WithResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.telemetry.NotificationsService
import eu.ibagroup.formainframe.tso.SESSION_EXECUTE_REXX_TOPIC
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.sendTopic
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.TsoResponse
import javax.swing.JComponent

/**
 * Class represents an Execute REXX action
 */
class ExecuteRexxAction : AnAction() {

  val logger: Logger = log<ExecuteRexxAction>()

  override fun actionPerformed(e: AnActionEvent) {
    var isRexx = false
    var throwable: Throwable? = null
    val project = e.project ?: return
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val crudable = ConfigService.getService().crudable
    val selectedNode = view.mySelectedNodesData[0].node as FileLikeDatasetNode
    val vFile = view.mySelectedNodesData[0].file ?: return
    val connectionConfig = selectedNode.unit.connectionConfig ?: return
    val rexxConfig = RexxParams(mutableListOf())

    if (checkFileForSync(e.project, vFile)) return
    val rexxCompatibilityTask = buildExecutionTask(project, "Checking REXX Compatibility...") { checkRexxCompatibility(selectedNode, rexxConfig, it) }
    runCatching {
      executeTask(rexxCompatibilityTask)
    }
      .onSuccess { isRexx = it }
      .onFailure {
        throwable = it
        NotificationsService.errorNotification(it, project)
      }

    if (isRexx) {
      val dialog = ExecuteRexxDialog.create(project, crudable, connectionConfig, ExecuteRexxDialogState(null))
      if (dialog.showAndGet()) {
        dialog.state.tsoSessionConfig?.also { rexxConfig.tsoSessionConfig = it }
        dialog.state.pgmArgumentsList.filter { it.isNotEmpty() }.also { rexxConfig.rexxArguments = it }
        val connectionConfigToRunUnder = crudable.getByUniqueKey<ConnectionConfig>(rexxConfig.tsoSessionConfig.connectionConfigUuid)
        if (connectionConfigToRunUnder != null) {

          val establishTsoSessionTask = buildExecutionTask(
            project,
            "Testing TSO Connection to ${connectionConfigToRunUnder.url}"
          ) { establishRuntimeEnvironment(rexxConfig, connectionConfigToRunUnder, it) }
          runCatching {
            executeTask(establishTsoSessionTask)
          }
            .onSuccess {
              logMessage("About to send $SESSION_EXECUTE_REXX_TOPIC topic to sync publisher")
              val sessionConfig = TSOConfigWrapper(rexxConfig.tsoSessionConfig, connectionConfigToRunUnder, it)
              sendTopic(SESSION_EXECUTE_REXX_TOPIC).executeRexx(project, sessionConfig, rexxConfig)
            }
            .onFailure { NotificationsService.errorNotification(it, project) }
        } else {
          NotificationsService.errorNotification(
            Exception(),
            project,
            "User Exception",
            "Connection config was not found",
            "Connection config for ${rexxConfig.tsoSessionConfig.name} was not found.\n " +
                "Please make sure the connection config is defined for the selected tso session config"
          )
        }
      }
    } else if (throwable == null) {
      NotificationsService.errorNotification(
        Exception(),
        project,
        "User Exception",
        "Member is not REXX",
        "Member you're trying to execute is not REXX program"
      )
    }
  }

  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    if (selected.size != 1) {
      e.presentation.isEnabledAndVisible = false
    } else {
      val attr = selected[0].attributes
      val tsoConfigsList = ConfigService.getService().crudable.getAll<TSOSessionConfig>().toList()
      e.presentation.isVisible = attr is RemoteMemberAttributes
      e.presentation.isEnabled = tsoConfigsList.isNotEmpty()
      if (tsoConfigsList.isEmpty())
        e.presentation.putClientProperty(Key(JComponent.TOOL_TIP_TEXT_KEY), "Create TSO session first")
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  /**
   * Logs any message
   * @param message - message to log
   */
  fun logMessage(message: String) {
    logger.info(message)
  }

  /**
   * Function builds an execution task to call afterward.
   * @param project
   * @param title
   * @param block block to execute under progress
   * @return WithResult object
   */
  private fun <R> buildExecutionTask(project: Project?, title: String, block: (ProgressIndicator) -> R): WithResult<R, Exception> {
    return object : WithResult<R, Exception>(project, title, true) {
      override fun compute(indicator: ProgressIndicator): R {
        return block(indicator)
      }
    }
  }

  /**
   * Executes the given task as Intellij modal task and waits the result.
   * Returns the execution result stored in passed WithResult object
   * or throws Exception if any exception is raised during processing.
   * @param taskToExecute
   * @return the result of execution
   * @throws any kind of Exception
   */
  private fun <R> executeTask(taskToExecute: WithResult<R, *>) : R {
    return ProgressManager.getInstance().run(taskToExecute)
  }

  /**
   * Function is used to check if the actual member(node) represents the REXX program
   * @param node
   * @param rexxConfig
   * @param indicator
   * @return true if the node represents executable REXX program. false otherwise
   * @throws CallException if API call failed
   */
  private fun checkRexxCompatibility(node: FileLikeDatasetNode, rexxConfig: RexxParams, indicator: ProgressIndicator) : Boolean {
    val connectionConfig = node.unit.connectionConfig ?: return false
    val libraryName = node.parent?.virtualFile?.filenameInternal?.also { rexxConfig.rexxLibrary = it } ?: return false
    val execMember = node.virtualFile.name.also { rexxConfig.execMember = it }
    val response = api<DataAPI>(connectionConfig).retrieveMemberContent(
      authorizationToken = connectionConfig.authToken,
      datasetName = libraryName,
      memberName = execMember,
    ).cancelByIndicator(indicator).execute()
    if (response.isSuccessful) {
      val firstLine = response.body()?.substringBefore("\n") ?: return false
      // Member is REXX program if first line of the content is comment (can be multirow comment),
      // but must contain 'REXX' word in the first line
      /*
          Example of the correct REXX pgm:
          SAY 'REXX'    /*  exampleREXX1
            continuation of the comment
            another line comment
          */
          SAY "HELLO"
          parse arg arg1 ',' arg2
       */
      return firstLine.contains("/*") && firstLine.substringAfter("/*").contains("REXX", ignoreCase = true)
    } else throw CallException(response, "Cannot retrieve member content")
  }

  /**
   * Function is used to establish TSO session
   * @param rexxConfig
   * @param connectionConfig
   * @return TsoResponse
   * @throws CallException if API call failed
   */
  private fun establishRuntimeEnvironment(rexxConfig: RexxParams, connectionConfig: ConnectionConfig, indicator: ProgressIndicator) : TsoResponse {
    val tsoStartOp =
      TsoOperation(TSOConfigWrapper(rexxConfig.tsoSessionConfig, connectionConfig), TsoOperationMode.START)
    return DataOpsManager.getService().performOperation(tsoStartOp, indicator)
  }

  /**
   * Data class represents REXX info to be executed
   */
  data class RexxParams(var rexxArguments: List<String>) {
    lateinit var tsoSessionConfig: TSOSessionConfig
    lateinit var rexxLibrary: String
    lateinit var execMember: String
  }
}