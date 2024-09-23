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

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.impl.ContentImpl
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobFilterState
import eu.ibagroup.formainframe.config.ws.JobFilterStateWithMultipleWS
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.ui.JesWorkingSetDialogState
import eu.ibagroup.formainframe.config.ws.ui.initEmptyUuids
import eu.ibagroup.formainframe.config.ws.ui.jes.JesWsDialog
import eu.ibagroup.formainframe.explorer.JesExplorer
import eu.ibagroup.formainframe.explorer.JesExplorerContentProvider
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.UIComponentManager
import eu.ibagroup.formainframe.explorer.ui.AddJobsFilterDialog
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW

/**
 * Action which allows to create Jes working set + filters from Jobs Logs View
 */
class GoToJobAction : AnAction() {

  companion object {
    const val JOB_FILTER_CREATED_TITLE = "Job Filter(s) successfully created"
    const val JOB_FILTER_NOT_CREATED_TITLE = "Job Filter cannot be created"
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Overrides actionPerformed in super class. When Go-To-Job button is pressed below implementation is used
   * Based on the current state of Jes Explorer View, it creates new Jes WS,
   * or if Jes WS already exists on the connection,
   * it searches for existing Job Filters and enables Job Filter creation on specified Jes WS
   */
  override fun actionPerformed(e: AnActionEvent) {
    var jobFilterCreated = false
    var message = ""
    val jobsLogsView = e.getData(JOBS_LOG_VIEW) ?: return
    val jobId = jobsLogsView.jobLogInfo.jobId ?: return
    val jesContentProvider =
      UIComponentManager.getService().getExplorerContentProvider(JesExplorer::class.java) as JesExplorerContentProvider
    val view = e.project?.let { jesContentProvider.getExplorerView(it) } ?: return
    val connectionConfig = jobsLogsView.getConnectionConfig()

    val jesWSOnSameConnection = view.let { jesView -> jesView.myFsTreeStructure.findByPredicate { it is JesWsNode } }
      .map { it as JesWsNode }
      .mapNotNull { if (it.unit.connectionConfig == connectionConfig) it else null }

    if (jesWSOnSameConnection.isEmpty()) {
      val maskRow = mutableListOf(JobFilterState(jobId = jobId))
      val dialog = JesWsDialog(
        ConfigService.getService().crudable,
        JesWorkingSetDialogState(maskRow = maskRow).initEmptyUuids(ConfigService.getService().crudable),
        true,
        connectionConfig
      )
      if (dialog.showAndGet()) {
        val wsConfigToSave = dialog.state.workingSetConfig
        ConfigService.getService().crudable.add(wsConfigToSave)
        jobFilterCreated = true
        message = createNotificationSuccessMessage(
          jobFilters = wsConfigToSave.jobsFilters.toMutableList(),
          connection = connectionConfig
        )
      }
    } else {
      val filteredJesWSNodes = jesWSOnSameConnection.filter { it.name != null }.distinct()
      val possibleWorkingSetsToCreateNewFilter = filteredJesWSNodes
        .filter { node -> node.children.none { (it as JesFilterNode).value.jobId == jobId } }
        .map { it.unit as JesWorkingSet }
      if (possibleWorkingSetsToCreateNewFilter.isNotEmpty()) {
        val initialState = JobFilterStateWithMultipleWS(
          wsList = possibleWorkingSetsToCreateNewFilter,
          prefix = "",
          owner = "",
          jobId = jobId
        )
        val dialog = AddJobsFilterDialog(e.project, initialState)
        if (dialog.showAndGet()) {
          val jobFilterToSave = dialog.state.toJobsFilter()
          dialog.state.selectedWS.addMask(jobFilterToSave)
          jobFilterCreated = true
          filteredJesWSNodes.find { it.unit == dialog.state.selectedWS }?.let {
            message = createNotificationSuccessMessage(it, mutableListOf(jobFilterToSave), connectionConfig)
            view.myStructure.apply {
              select(it, view.myTree) { }
              expand(it, view.myTree) { }
            }
          }
        }
      } else {
        val wsNamesWithCreationFailure = filteredJesWSNodes.mapNotNull { it.name }
        message = "Cannot create job filter, because all working sets ($wsNamesWithCreationFailure) " +
          "on connection $connectionConfig already contain job filter with jobId = $jobId"
        view.explorer.showNotification(title = JOB_FILTER_NOT_CREATED_TITLE, content = message, project = e.project)
        e.project?.let { navigateToJesExplorer(view, it) }
      }
    }

    if (jobFilterCreated) {
      view.explorer.showNotification(title = JOB_FILTER_CREATED_TITLE, content = message, project = e.project)
      e.project?.let { navigateToJesExplorer(view, it) }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    e.getData(JOBS_LOG_VIEW)?.let {
      e.presentation.apply {
        isVisible = true
        isEnabled = it.getJobLogger().logFetcher.getCachedJobStatus()?.status.let { it != null }
      }
    }
  }

  /**
   * Function creates SUCCESS message content for Job Filter creation
   * @param wsNode
   * @param jobFilters
   * @param connection
   * @return String representation of the message
   */
  private fun createNotificationSuccessMessage(
    wsNode: JesWsNode? = null,
    jobFilters: List<JobsFilter>,
    connection: ConnectionConfig
  ): String {
    val messageBuilder = StringBuilder().append("Job Filter(s): ")
    jobFilters.forEach { messageBuilder.append(it.toString().plus(", ")) }
    messageBuilder.append("successfully created ")
    if (wsNode != null) messageBuilder.append("in the working set ${wsNode.name} ")
    messageBuilder.append("on connection: $connection")
    return messageBuilder.toString()
  }

  /**
   * Method gets the tool window content and navigates to File/Jes Explorer view and puts the focus on selected view
   * @param view
   * @param project
   */
  private fun navigateToJesExplorer(view: ExplorerTreeView<*, *, *>, project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("For Mainframe")
    val content = toolWindow?.contentManager?.getContent(view) as ContentImpl
    toolWindow.contentManager.setSelectedContent(content, true)
  }

}
