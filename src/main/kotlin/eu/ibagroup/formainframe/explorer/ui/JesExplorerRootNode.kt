/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.GlobalJesWorkingSet

class JesExplorerRootNode(
  value: Explorer<*>, project: Project,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerTreeNode<Explorer<*>>(
  value, project,
  null,
  value, treeStructure
) {
  override fun update(presentation: PresentationData) {

  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return explorer.units.filterIsInstance<GlobalJesWorkingSet>().map {
      JobsWsNode(it, notNullProject, this, treeStructure)
    }.toMutableList()
  }
}
