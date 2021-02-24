package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet

class FileExplorerTreeNodeRoot(explorer: Explorer, project: Project, viewSettings: ExplorerViewSettings) :
  ExplorerTreeNodeBase<Explorer>(explorer, project, explorer, viewSettings) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<WorkingSet>()
      .map { WorkingSetNode(it, notNullProject, viewSettings) }.toMutableList()
  }
}