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

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.progress.runModalTask
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import eu.ibagroup.formainframe.dataops.operations.jobs.BasicPurgeJobParams
import eu.ibagroup.formainframe.dataops.operations.jobs.PurgeJobOperation
import eu.ibagroup.formainframe.explorer.ui.JesExplorerView
import eu.ibagroup.formainframe.explorer.ui.JesFilterNode
import eu.ibagroup.formainframe.explorer.ui.JesWsNode
import eu.ibagroup.formainframe.explorer.ui.JobNode
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.getExplorerView
import eu.ibagroup.formainframe.ui.build.jobs.JOBS_LOG_VIEW
import eu.ibagroup.formainframe.ui.build.jobs.JobBuildTreeView
import org.zowe.kotlinsdk.ExecData
import org.zowe.kotlinsdk.JESApi
import retrofit2.Response
import java.util.concurrent.ConcurrentHashMap

/** An action to purge a job */
class PurgeJobAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  override fun isDumbAware(): Boolean {
    return true
  }

  /**
   * Purge a job on button click
   * After completion shows a notification
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    if (view is JesExplorerView) {
      val jobNodesToPurge = view.mySelectedNodesData
      val filtersToNodesMap = selectJobNodesByWSFilters(jobNodesToPurge)
      runMassivePurgeAndRefreshByFilter(filtersToNodesMap, jobNodesToPurge, e, view)

    } else if (view is JobBuildTreeView) {
      val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()
      val connectionConfig: ConnectionConfig = view.getConnectionConfig()
      val dataOpsManager = DataOpsManager.getService()
      if (jobStatus != null) {
        runBackgroundableTask(
          title = "Purging ${jobStatus.jobName}: ${jobStatus.jobId}",
          project = e.project,
          cancellable = true
        ) {
          runCatching {
            dataOpsManager.performOperation(
              operation = PurgeJobOperation(
                request = BasicPurgeJobParams(jobStatus.jobName, jobStatus.jobId),
                connectionConfig = connectionConfig
              ),
              progressIndicator = it
            )
          }.onFailure {
            view.showNotification(
              "Error purging ${jobStatus.jobName}: ${jobStatus.jobId}",
              "${it.message}",
              e.project,
              NotificationType.ERROR
            )
          }.onSuccess {
            view.showNotification(
              "${jobStatus.jobName}: ${jobStatus.jobId} has been purged",
              "$it",
              e.project,
              NotificationType.INFORMATION
            )
          }
        }
      }
    }
  }

  /**
   * After performing the massive purge of the jobs scope (several filters) z/OSMF returns immediate success in the response,
   * but actual purge was not performed yet on the mainframe which causes the problems during fetching the new job list during refresh.
   * The purpose of this function is to perform the refresh on each filter only when actual purge was performed
   * Note: This function performs refresh only on those filters from which purge action is requested
   * @param jobsByFilterWaitingPurgeMap - concurrent map of the filter-to-jobs relationship waiting to purge
   * @return Void
   */
  private fun waitJobsReleasedAndRefresh(jobsByFilterWaitingPurgeMap: ConcurrentHashMap<JesFilterNode, List<NodeData<ConnectionConfig>>>) {
    val foundJobsWaitingInPurgeQueue: MutableList<JobNode> = mutableListOf()
    val filtersWithRefreshErrors: MutableMap<JesFilterNode, Response<*>> = mutableMapOf()
    jobsByFilterWaitingPurgeMap.keys.forEach { filterNode ->
      val query = filterNode.query
      if (query != null) {
        val response = api<JESApi>(query.connectionConfig).getFilteredJobs(
          basicCredentials = query.connectionConfig.authToken,
          owner = query.request.owner,
          prefix = query.request.prefix,
          userCorrelator = query.request.userCorrelatorFilter,
          execData = ExecData.YES
        ).execute()
        val result = response.body()
        if (response.isSuccessful && result != null) {
          jobsByFilterWaitingPurgeMap[filterNode]?.forEach { data ->
            val nodeToFind = data.node as JobNode
            val jobAttributes = data.attributes as RemoteJobAttributes
            val jobInfo = jobAttributes.jobInfo
            val foundJob = result.find { it.jobId == jobInfo.jobId }
            if (foundJob != null) foundJobsWaitingInPurgeQueue.add(nodeToFind)
          }
          if (foundJobsWaitingInPurgeQueue.isNotEmpty()) {
            foundJobsWaitingInPurgeQueue.clear()
            val filterRefreshSize =
              jobsByFilterWaitingPurgeMap[filterNode]?.size ?: throw Exception("Filter node is not found")
            runRefreshByFilter(
              filterNode,
              if (filterRefreshSize == 1) (filterRefreshSize.toLong() * 1000) else (filterRefreshSize / 2).toLong() * 1000
            )
          } else {
            filterNode.cleanCache()
          }
        } else {
          filtersWithRefreshErrors[filterNode] = response
        }
      }
    }
    if (filtersWithRefreshErrors.isNotEmpty()) {
      var errorFilterNames = ""
      for (entry in filtersWithRefreshErrors) {
        errorFilterNames += entry.key.unit.name + "\n"
      }
      throw RuntimeException("Refresh error. Failed filters are: $errorFilterNames")
    }
  }

  /**
   * Function triggers refresh by filter
   * @param filter - jes filter
   * @param timeWait - time to wait before refresh
   */
  private fun runRefreshByFilter(filter: JesFilterNode, timeWait: Long) {
    runInEdt {
      Thread.sleep(timeWait)
      filter.cleanCache()
    }
  }

  /**
   * Function needs to determine when to show "purge job/jobs" action. Currently, purge is only possible within 1 Working Set
   * @param jobs - the list of selected job nodes in JES view
   * @return true if jobs were selected within 1 WS, false otherwise
   */
  private fun isSelectedJobNodesFromSameWS(jobs: List<NodeData<ConnectionConfig>>): Boolean {
    val firstJob = jobs[0].node as JobNode
    val firstConnection = firstJob.query?.connectionConfig

    // Could be different connections
    val diffConnectionJobNode = jobs.filter {
      (it.node is JobNode && it.node.unit.connectionConfig != firstConnection)
    }
    if (diffConnectionJobNode.isNotEmpty()) return false

    // Could be the same connections but different Working Sets
    val firstJobWS = if (firstJob.parent is JesFilterNode) firstJob.parent.parent as? JesWsNode else null
    if (firstJobWS != null) {
      val diffWS = jobs.filter {
        val jobWS = it.node.parent?.parent as JesWsNode
        firstJobWS.unit.name != jobWS.unit.name
      }
      if (diffWS.isNotEmpty()) return false
    }
    return true
  }

  /**
   * Function builds the filter-to-jobs dependency and puts it to the concurrent map
   * @param nodes - list of the selected job nodes in JES view
   * @return concurrent map of the filter-to-jobs dependency
   */
  private fun selectJobNodesByWSFilters(nodes: List<NodeData<ConnectionConfig>>): ConcurrentHashMap<JesFilterNode, List<NodeData<ConnectionConfig>>> {
    val jobFilters: MutableList<JesFilterNode> = mutableListOf()
    val filtersToJobNodesMap: ConcurrentHashMap<JesFilterNode, List<NodeData<ConnectionConfig>>> = ConcurrentHashMap()
    nodes.forEach {
      val jobNode = it.node as JobNode
      val filterNode = findFilter(jobNode)
      if (filterNode != null && !jobFilters.contains(filterNode)) {
        jobFilters.add(filterNode)
        filtersToJobNodesMap[filterNode] = findJobsByFilter(filterNode, nodes)
      }
    }
    return filtersToJobNodesMap
  }

  /**
   * Function finds the corresponding filter or null if filter was not found
   * @param node - job node to find its filter
   * @return the JesFilterNode object corresponding to this job node or null if it was not found
   */
  private fun findFilter(node: JobNode): JesFilterNode? {
    return node.parent as? JesFilterNode
  }

  /**
   * Function finds all selected job nodes which correspond to the specified filter
   * @param filter - filter to find the corresponding job nodes
   * @param nodes - the list of all selected job nodes in JES view
   * @return the list of job nodes which correspond to the specified filter
   */
  private fun findJobsByFilter(
    filter: JesFilterNode,
    nodes: List<NodeData<ConnectionConfig>>
  ): List<NodeData<ConnectionConfig>> {
    val jobsByFilter: MutableList<NodeData<ConnectionConfig>> = mutableListOf()
    nodes.forEach {
      val jobNode = it.node as JobNode
      val jobFilter = findFilter(jobNode)
      if (filter == jobFilter) {
        jobsByFilter.add(it)
      }
    }
    return jobsByFilter
  }

  /**
   * The main function which triggers the jobs purge on the mainframe and refreshes each affected filter in the JES view tree
   * @param jobsByFilterMap - the concurrent map of the filter-to-jobs dependency
   * @param nodes - the list of all selected job nodes to purge
   * @param e - purge action event
   * @param view - an instance of current JES view
   */
  private fun runMassivePurgeAndRefreshByFilter(
    jobsByFilterMap: ConcurrentHashMap<JesFilterNode, List<NodeData<ConnectionConfig>>>,
    nodes: List<NodeData<ConnectionConfig>>,
    e: AnActionEvent,
    view: JesExplorerView
  ) {
    val dataOpsManager = DataOpsManager.getService()
    nodes.forEach { nodeData ->
      val jobAttributes = nodeData.attributes as RemoteJobAttributes
      val jobStatus = jobAttributes.jobInfo
      val connectionConfig = jobAttributes.requesters[0].connectionConfig
      runModalTask(
        title = "Purging ${jobStatus.jobName}: ${jobStatus.jobId}",
        project = e.project,
        cancellable = true
      ) {
        runCatching {
          dataOpsManager.performOperation(
            operation = PurgeJobOperation(
              request = BasicPurgeJobParams(jobStatus.jobName, jobStatus.jobId),
              connectionConfig = connectionConfig
            ),
            progressIndicator = it
          )
        }.onFailure { throwable ->
          (jobsByFilterMap.values.find { it.contains(nodeData) } as MutableList).remove(nodeData)
          view.explorer.showNotification(
            "Error purging ${jobStatus.jobName}: ${jobStatus.jobId}",
            "${throwable.message}",
            NotificationType.ERROR,
            e.project
          )
        }.onSuccess {
          view.explorer.showNotification(
            "${jobStatus.jobName}: ${jobStatus.jobId} has been purged",
            "$it",
            NotificationType.INFORMATION,
            e.project
          )
        }
      }
    }
    waitJobsReleasedAndRefresh(jobsByFilterMap)
  }

  /**
   * A job can be purged from the Jobs Tool Window
   * or from the JES Explorer by clicking on the corresponding job
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<JesExplorerView>() ?: e.getData(JOBS_LOG_VIEW) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    if (view is JesExplorerView) {
      val selected = view.mySelectedNodesData
      val wrongNode = selected.find { it.node !is JobNode }
      e.presentation.apply {
        isEnabledAndVisible = wrongNode == null && isSelectedJobNodesFromSameWS(selected)
        text = if (isEnabledAndVisible && selected.size > 1) "Purge Jobs" else "Purge Job"
      }
    } else if (view is JobBuildTreeView) {
      val jobStatus = view.getJobLogger().logFetcher.getCachedJobStatus()?.status
      if (jobStatus == null) {
        e.presentation.isEnabled = false
      }
    }
  }
}
