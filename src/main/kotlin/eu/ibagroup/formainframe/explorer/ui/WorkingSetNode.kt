package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet

class WorkingSetNode(
  workingSet: WorkingSet,
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<WorkingSet, WorkingSet>(workingSet, project, parent, workingSet, treeStructure) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.setIcon(AllIcons.Nodes.Project)
    val wsName = value.name
    presentation.presentableText = wsName
    when {
      value.connectionConfig == null || value.urlConnection == null -> connectionIsNotSet(presentation)
      value.dsMasks.isEmpty() && value.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
      treeStructure.showWorkingSetInfo -> addInfo(presentation)
    }
  }

  private fun connectionIsNotSet(presentation: PresentationData) {
  }

  private fun destinationsAreEmpty(presentation: PresentationData) {
  }

  private fun addInfo(presentation: PresentationData) {
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.dsMasks.map { DSMaskNode(it, notNullProject, this, value, treeStructure) }.plus(
      value.ussPaths.map { UssDirNode(it, notNullProject, this, value, treeStructure, isRootNode = true) }
    ).toMutableList()
  }

}