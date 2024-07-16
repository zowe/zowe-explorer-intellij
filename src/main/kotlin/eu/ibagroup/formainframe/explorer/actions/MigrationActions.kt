/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.MigrateActionType
import eu.ibagroup.formainframe.analytics.events.MigrateEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.content.synchronizer.checkFileForSync
import eu.ibagroup.formainframe.dataops.operations.migration.MigrateOperation
import eu.ibagroup.formainframe.dataops.operations.migration.MigrateOperationParams
import eu.ibagroup.formainframe.dataops.operations.migration.RecallOperation
import eu.ibagroup.formainframe.dataops.operations.migration.RecallOperationParams
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Get data for explorer node
 * @return Pair of [MFVirtualFile] and [ConnectionConfig]
 */
fun getRequestDataForNode(node: ExplorerTreeNode<*, *>): Pair<VirtualFile, ConnectionConfig>? {
  return if (node is ExplorerUnitTreeNodeBase<*, *, *> && node.unit is FilesWorkingSet) {
    val file = node.virtualFile
    val config = node.unit.connectionConfig
    if (file != null && config != null) {
      return Pair(file, config)
    }
    null
  } else {
    null
  }
}

/**
 * Clean cache for explorer nodes
 * @see ExplorerTreeNode
 */
private fun makeUniqueCacheClean(nodes: List<ExplorerTreeNode<*, *>>) {
  val uniqueParentNodes = nodes.map { it.parent }.distinct()
  uniqueParentNodes.forEach { it?.cleanCacheIfPossible(cleanBatchedQuery = true) }
}

/**
 * Filter out nodes data that cannot be migrated due to synchronization
 */
private fun filterNodesData(project: Project?, nodesData: List<NodeData<*>>): List<NodeData<*>> {
  return nodesData.filter {
    it.file != null && !checkFileForSync(project, it.file, checkDependentFiles = true)
  }
}

/**
 * Action class for recall a migrated dataset
 * @see MigrateAction
 */
class RecallAction : DumbAwareAction() {

  /**
   * Runs recall operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>()
    if (view != null) {
      val filteredNodesData = filterNodesData(e.project, view.mySelectedNodesData)
      val pairs = filteredNodesData.mapNotNull { getRequestDataForNode(it.node) }
      val operations: List<RecallOperation> = pairs.map {
        RecallOperation(
          request = RecallOperationParams(it.first),
          connectionConfig = it.second
        )
      }
      runModalTask("Recalling Datasets") { progressIndicator ->
        runCatching {
          operations.forEach { operation ->

            service<AnalyticsService>().trackAnalyticsEvent(MigrateEvent(MigrateActionType.RECALL))

            service<DataOpsManager>().performOperation(
              operation, progressIndicator
            )
          }
        }.onFailure {
          view.explorer.reportThrowable(it, e.project)
        }
      }
      makeUniqueCacheClean(filteredNodesData.map { it.node })
    }

  }

  /**
   * Determines if recall operation is possible for chosen object
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val wrongNode = selected.find {
      val attributes = it.attributes as? RemoteDatasetAttributes
      val isMigrated = attributes?.isMigrated ?: false
      !isMigrated
    }
    e.presentation.isEnabledAndVisible = wrongNode == null
  }

}

/**
 * Action class for dataset migration
 */
class MigrateAction : DumbAwareAction() {

  /**
   * Runs migrate operation
   */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>()
    if (view != null) {
      val filteredNodesData = filterNodesData(e.project, view.mySelectedNodesData)
      val pairs = filteredNodesData.mapNotNull { getRequestDataForNode(it.node) }
      val operations: List<MigrateOperation> = pairs.map {
        MigrateOperation(
          request = MigrateOperationParams(it.first),
          connectionConfig = it.second
        )
      }
      runModalTask("Migrating Datasets") { progressIndicator ->
        runCatching {
          operations.forEach { operation ->

            service<AnalyticsService>().trackAnalyticsEvent(MigrateEvent(MigrateActionType.MIGRATE))

            service<DataOpsManager>().performOperation(
              operation, progressIndicator
            )
          }
        }.onFailure {
          view.explorer.reportThrowable(it, e.project)
        }
      }
      makeUniqueCacheClean(filteredNodesData.map { it.node })
    }
  }

  /**
   * Determines if migrate operation is possible for chosen object
   */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val wrongNode = selected.find {
      val attributes = it.attributes as? RemoteDatasetAttributes
      val isMigrated = attributes?.isMigrated ?: false
      isMigrated || attributes !is RemoteDatasetAttributes
    }
    e.presentation.isEnabledAndVisible = wrongNode == null
  }

}
