package eu.ibagroup.formainframe.explorer.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings
import eu.ibagroup.formainframe.explorer.WorkingSet

class WorkingSetNode(
  workingSet: WorkingSet,
  project: Project,
  viewSettings: ExplorerViewSettings
) : ExplorerUnitTreeNodeBase<WorkingSet, WorkingSet>(workingSet, project, workingSet, viewSettings) {

  override fun isAlwaysExpand(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.setIcon(AllIcons.Nodes.Project)
    presentation.presentableText = value.name
    when {
      value.connectionConfig == null || value.urlConnection == null -> connectionIsNotSet(presentation)
      value.dsMasks.isEmpty() && value.ussPaths.isEmpty() -> destinationsAreEmpty(presentation)
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
    return value.dsMasks.map { DSMaskNode(it, notNullProject, value, viewSettings) }.plus(
      value.ussPaths.map { UssDirNode(it, notNullProject, value, viewSettings, isRootNode = true) }
    ).toMutableList()
  }

}