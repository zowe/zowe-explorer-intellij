package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet

class FileExplorerTreeNodeRoot(explorer: Explorer, viewSettings: ExplorerViewSettings) :
  ExplorerTreeNodeBase<Explorer>(explorer, explorer, viewSettings) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<WorkingSet>()
      .map { WorkingSetNode(it, viewSettings) }.toMutableList()
  }
}