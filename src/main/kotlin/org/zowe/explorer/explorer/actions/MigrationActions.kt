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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.content.synchronizer.checkFileForSync
import org.zowe.explorer.dataops.operations.RemoteUnitOperation
import org.zowe.explorer.dataops.operations.migration.MigrateOperation
import org.zowe.explorer.dataops.operations.migration.MigrateOperationParams
import org.zowe.explorer.dataops.operations.migration.RecallOperation
import org.zowe.explorer.dataops.operations.migration.RecallOperationParams
import org.zowe.explorer.explorer.FilesWorkingSet
import org.zowe.explorer.explorer.ui.*
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.vfs.MFVirtualFile
import kotlin.collections.map

/**
 * Get data for explorer node
 * @return Pair of [MFVirtualFile] and [ConnectionConfig]
 */
fun getRequestDataForNode(node: ExplorerTreeNode<*, *>): Pair<VirtualFile, ConnectionConfig>? {
  return if (node is ExplorerUnitTreeNodeBase<*, *, *> && node.unit is FilesWorkingSet) {
    val file = node.virtualFile
    val config = node.unit.connectionConfig
    if (file != null && config != null) file to config
    else null
  } else {
    null
  }
}

/**
 * Action class for recall a migrated dataset
 * @see MigrateAction
 */
class RecallAction : AbstractRecallOrMigrateAction() {

  override val modalTaskTitle = "Recalling Datasets"

  override fun prepareOperationForNode(
    vFileToConnectionConfig: Pair<VirtualFile, ConnectionConfig>
  ): RemoteUnitOperation<*> {
    val (vFile, connectionConfig) = vFileToConnectionConfig
    return RecallOperation(request = RecallOperationParams(vFile), connectionConfig = connectionConfig)
  }

  /** Determines if recall operation is possible for chosen object */
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
class MigrateAction : AbstractRecallOrMigrateAction() {

  override val modalTaskTitle = "Migrating Datasets"

  override fun prepareOperationForNode(
    vFileToConnectionConfig: Pair<VirtualFile, ConnectionConfig>
  ): RemoteUnitOperation<*> {
    val (vFile, connectionConfig) = vFileToConnectionConfig
    return MigrateOperation(request = MigrateOperationParams(vFile), connectionConfig = connectionConfig)
  }

  /** Determines if migrate operation is possible for chosen object */
  override fun update(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val selected = view.mySelectedNodesData
    val hasWrongNode = selected.find {
      val attributes = it.attributes as? RemoteDatasetAttributes
      attributes !is RemoteDatasetAttributes || !attributes.hasDsOrg
    }
    e.presentation.isEnabledAndVisible = hasWrongNode == null
  }

}

/** Abstract class for Recall and Migrate actions */
abstract class AbstractRecallOrMigrateAction : DumbAwareAction() {

  abstract val modalTaskTitle: String

  /** Prepare the operation object for the selected node */
  abstract fun prepareOperationForNode(
    vFileToConnectionConfig: Pair<VirtualFile, ConnectionConfig>
  ): RemoteUnitOperation<*>

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  /** Run dataset Recall or Migrate operation */
  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getExplorerView<FileExplorerView>() ?: return
    val project = e.project
    val filteredNodesData = view.mySelectedNodesData.filter {
      it.file != null && !checkFileForSync(project, it.file, checkDependentFiles = true)
    }
    val pairs = filteredNodesData.mapNotNull { getRequestDataForNode(it.node) }
    val operations: List<RemoteUnitOperation<*>> = pairs.map(::prepareOperationForNode)

    runModalTask(modalTaskTitle) { progressIndicator ->
      runCatching {
        operations.forEach { operation ->
          DataOpsManager.getService()
            .performOperation(operation, progressIndicator)
        }
      }.onFailure {
        NotificationsService.errorNotification(it, project)
      }
    }

    filteredNodesData
      .map { it.node.parent }
      .distinct()
      .forEach {
        it?.cleanCacheIfPossible(cleanBatchedQuery = true)
      }
  }

}
