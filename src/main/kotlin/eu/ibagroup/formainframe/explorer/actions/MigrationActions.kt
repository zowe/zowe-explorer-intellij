package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.analytics.AnalyticsService
import eu.ibagroup.formainframe.analytics.events.MigrateActionType
import eu.ibagroup.formainframe.analytics.events.MigrateEvent
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.operations.migration.MigrateOperation
import eu.ibagroup.formainframe.dataops.operations.migration.MigrateOperationParams
import eu.ibagroup.formainframe.dataops.operations.migration.RecallOperation
import eu.ibagroup.formainframe.dataops.operations.migration.RecallOperationParams
import eu.ibagroup.formainframe.explorer.WorkingSet
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerUnitTreeNodeBase
import eu.ibagroup.formainframe.explorer.ui.FILE_EXPLORER_VIEW
import eu.ibagroup.formainframe.explorer.ui.cleanCacheIfPossible


fun getRequestDataForNode(node: ExplorerTreeNode<*>): Pair<VirtualFile, ConnectionConfig>? {
  return if (node is ExplorerUnitTreeNodeBase<*, *> && node.unit is WorkingSet) {
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

private fun makeUniqueCacheClean(nodes: List<ExplorerTreeNode<*>>) {
  val uniqueParentNodes = nodes.map { it.parent }.distinct()
  uniqueParentNodes.forEach { it?.cleanCacheIfPossible() }
}

class RecallAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW)
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

            service<AnalyticsService>().trackAnalyticsEvent(MigrateEvent(MigrateActionType.RECALL))

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

  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
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

class MigrateAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW)
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

            service<AnalyticsService>().trackAnalyticsEvent(MigrateEvent(MigrateActionType.MIGRATE))

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


  override fun update(e: AnActionEvent) {
    val view = e.getData(FILE_EXPLORER_VIEW) ?: let {
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