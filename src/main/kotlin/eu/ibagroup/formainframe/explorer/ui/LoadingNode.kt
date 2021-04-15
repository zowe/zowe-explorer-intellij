package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.IdeBundle.message
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.explorer.Explorer

class LoadingNode(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer,
  treeStructure: ExplorerTreeStructureBase
) : InfoNodeBase(project, parent, explorer, treeStructure) {

  override val text: String = message("treenode.loading")

  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES

}