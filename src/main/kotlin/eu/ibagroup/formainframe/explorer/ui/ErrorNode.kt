package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.explorer.Explorer

class ErrorNode(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer,
  treeStructure: ExplorerTreeStructureBase,
  override var text: String = message("title.error")
) : InfoNodeBase(project, parent, explorer, treeStructure) {


  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES

}