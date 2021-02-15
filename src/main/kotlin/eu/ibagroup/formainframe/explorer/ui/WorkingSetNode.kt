package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet

class WorkingSetNode(
  private val workingSet: WorkingSet,
  viewSettings: ExplorerViewSettings
) : ExplorerUnitTreeNodeBase<WorkingSet, WorkingSet>(workingSet, workingSet, viewSettings) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.setIcon(AllIcons.Nodes.Project)
    presentation.presentableText = workingSet.name
    when {
      workingSet.connectionConfig == null || workingSet.urlConnection == null -> connectionIsNotSet(presentation)
      workingSet.dsMasks.isEmpty() && workingSet.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
      viewSettings.showWorkingSetInfo -> addInfo(presentation)
    }
  }

  private fun connectionIsNotSet(presentation: PresentationData) {
  }

  private fun destinationsAreEmpty(presentation: PresentationData) {
  }

  private fun addInfo(presentation: PresentationData) {
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return workingSet.dsMasks.map { DSMaskNode(it, workingSet, viewSettings) }.plus(
      workingSet.ussPaths.map { UssDirNode(it, workingSet, viewSettings, isRootNode = true) }
    ).toMutableList()
  }

}