/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.operations.migration.MigrateOperation
import org.zowe.explorer.dataops.operations.migration.MigrateOperationParams
import org.zowe.explorer.dataops.operations.migration.RecallOperation
import org.zowe.explorer.dataops.operations.migration.RecallOperationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.ExplorerUnitTreeNodeBase
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.cleanCacheIfPossible
import org.zowe.explorer.explorer.ui.getExplorerView
import org.zowe.explorer.vfs.MFVirtualFile

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
      val triples = view.mySelectedNodesData.mapNotNull { getRequestDataForNode(it.node) }
      val operations: List<RecallOperation> = triples.map {
        RecallOperation(
          request = RecallOperationParams(it.first),
          connectionConfig = it.second
        )
      }
      runModalTask("Recalling Datasets") { progressIndicator ->
        runCatching {
          operations.forEach { operation ->

            service<DataOpsManager>().performOperation(
              operation, progressIndicator
            )
          }
        }.onFailure {
          view.explorer.reportThrowable(it, e.project)
        }
      }
      makeUniqueCacheClean(view.mySelectedNodesData.map { it.node })
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
      val triples = view.mySelectedNodesData.mapNotNull { getRequestDataForNode(it.node) }
      val operations: List<MigrateOperation> = triples.map {
        MigrateOperation(
          request = MigrateOperationParams(it.first),
          connectionConfig = it.second
        )
      }
      runModalTask("Migrating Datasets") { progressIndicator ->
        runCatching {
          operations.forEach { operation ->

            service<DataOpsManager>().performOperation(
              operation, progressIndicator
            )
          }
        }.onFailure {
          view.explorer.reportThrowable(it, e.project)
        }
      }
      makeUniqueCacheClean(view.mySelectedNodesData.map { it.node })
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
