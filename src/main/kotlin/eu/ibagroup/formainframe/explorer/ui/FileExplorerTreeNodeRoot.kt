package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FilesWorkingSet
import eu.ibagroup.formainframe.explorer.GlobalExplorer
import eu.ibagroup.formainframe.explorer.WorkingSet

class FileExplorerTreeNodeRoot(explorer: Explorer<FilesWorkingSet>, project: Project, treeStructure: ExplorerTreeStructureBase) :
  ExplorerTreeNode<Explorer<FilesWorkingSet>>(explorer, project, null, explorer, treeStructure) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<FilesWorkingSet>()
      .map { WorkingSetNode(it, notNullProject, this, treeStructure) }.toMutableList()
  }
}
