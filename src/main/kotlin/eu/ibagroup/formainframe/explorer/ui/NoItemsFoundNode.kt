package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.explorer.Explorer

class NoItemsFoundNode<Connection: ConnectionConfigBase>(
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  explorer: Explorer<Connection, *>,
  treeStructure: ExplorerTreeStructureBase
) : InfoNodeBase<Connection>(project, parent, explorer, treeStructure) {
  override val text: String = "No items found"
  override val textAttributes: SimpleTextAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
}