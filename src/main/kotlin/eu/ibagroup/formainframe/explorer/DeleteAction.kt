package eu.ibagroup.formainframe.explorer

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.showYesNoDialog
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.dataops.operations.DeleteOperation
import eu.ibagroup.formainframe.explorer.ui.*
import eu.ibagroup.formainframe.utils.service
import eu.ibagroup.formainframe.vfs.MFVirtualFile

class DeleteAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES) ?: return
    selected.map { it.node }.filterIsInstance<WorkingSetNode>()
      .forEach {
        if (showYesNoDialog(
            title = "Deletion of Working Set ${it.unit.name}",
            message = "Do you want to delete this Working Set from configs? Note: all data under it will be untouched",
            project = e.project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          it.explorer.disposeUnit(it.unit)
        }
      }
    selected.map { it.node }.filterIsInstance<DSMaskNode>()
      .filter { it.explorer.isUnitPresented(it.unit) }
      .forEach {
        if (showYesNoDialog(
            title = "Deletion of DS Mask ${it.value.mask}",
            message = "Do you want to delete this mask from configs? Note: all data sets under it will be untouched",
            project = e.project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          it.unit.removeMask(it.value)
        }
      }
    selected.map { it.node }.filter { it is UssDirNode && it.isConfigUssPath }
      .filter { it.explorer.isUnitPresented((it as UssDirNode).unit) }
      .forEach {
        val node = it as UssDirNode
        if (showYesNoDialog(
            title = "Deletion of Uss Path Root ${node.value.path}",
            message = "Do you want to delete this USS path root from configs? Note: all files under it will be untouched",
            project = e.project,
            icon = AllIcons.General.QuestionDialog
          )
        ) {
          node.unit.removeUssPath(node.value)
        }
      }
    val nodeDataAndPaths = selected
      .filterNot {
        it.node is WorkingSetNode || it.node is DSMaskNode || (it.node is UssDirNode && it.node.isConfigUssPath)
      }.mapNotNull {
        val file = it.file ?: return@mapNotNull null
        val pathFiles = mutableListOf<MFVirtualFile>()
        var current: MFVirtualFile? = file
        while (current != null) {
          pathFiles.add(current)
          current = current.parent
        }
        Pair(it, pathFiles)
      }
    val nodeDataAndPathFiltered = nodeDataAndPaths.filter { orig ->
      nodeDataAndPaths.filter { orig.second.size > it.second.size }
        .none { orig.second.containsAll(it.second) }
    }
    val nodeAndFilePairs = nodeDataAndPathFiltered.map { it.first }.filter {
        service<DataOpsManager>(it.node.explorer.componentManager).isOperationSupported(
          DeleteOperation(
            listOf(it.file ?: return@filter false)
          )
        )
      }.mapNotNull { Pair(it, it.file ?: return@mapNotNull null) }
    if (nodeAndFilePairs.isNotEmpty()) {
      val nodes = nodeAndFilePairs.map { it.first.node }.toSet()
      val files = nodeAndFilePairs.map { it.second }.toSet().toList()
      if (showYesNoDialog(
          title = "Confirm Files Deletion",
          message = "Are you sure want to delete ${files.size} file(s)?",
          project = e.project,
          icon = AllIcons.General.QuestionDialog
        )
      ) {
        service<DataOpsManager>(
          componentManager = nodes.first().explorer.componentManager
        ).performOperation(
          operation = DeleteOperation(files),
          callback = fetchAdapter {
            onSuccess {
              nodes.forEach { it.parent?.cleanCacheIfPossible() }
            }
          },
          project = e.project
        )
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val selected = e.getData(SELECTED_NODES) ?: run {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isEnabledAndVisible = selected.any {
      it.node is WorkingSetNode
        || it.node is DSMaskNode
        || (it.node is UssDirNode && it.node.isConfigUssPath)
        || service<DataOpsManager>(it.node.explorer.componentManager).isOperationSupported(
        DeleteOperation(
          listOf(it.file ?: return@any false)
        )
      )
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }

}