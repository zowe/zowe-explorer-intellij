package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.explorer.JesWorkingSet

class JobsWsNode(
  workingSet: JesWorkingSet,
  project: Project,
  parent: ExplorerTreeNode<*>,
  treeStructure: ExplorerTreeStructureBase
) : WorkingSetNode<JobsFilter>(
  workingSet, project, parent, treeStructure
), MFNode, RefreshableNode {

  override fun update(presentation: PresentationData) {
    presentation.addText(value.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
    when {
      value.connectionConfig == null -> connectionIsNotSet(presentation)
      value.masks.isEmpty() -> destinationsAreEmpty(presentation)
      else -> regular(presentation)
    }
    if (treeStructure.showWorkingSetInfo) {
      addInfo(presentation)
    }
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return value.masks.map { JesFilterNode(it, notNullProject, this, value as JesWorkingSet, treeStructure) }
      .toMutableSmartList().also { cachedChildrenInternal = it }
  }
}
