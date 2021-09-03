/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.jobs.JobsWorkingSetConfig
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.JesFilterUnit
import eu.ibagroup.formainframe.explorer.JesWorkingSet
import eu.ibagroup.formainframe.explorer.JobsWorkingSetUnit
import java.util.stream.Collectors

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
//    return explorer.units.filterIsInstance<JesFilterUnit>().map {
//       JesFilterNode(it, notNullProject, this, explorer, treeStructure)
//    }.toMutableList()
    return explorer.units.filterIsInstance<JesWorkingSet>().map {
      JobsWsNode(it, notNullProject, this, treeStructure)
    }.toMutableList()
//    return mutableListOf()
  }
}
