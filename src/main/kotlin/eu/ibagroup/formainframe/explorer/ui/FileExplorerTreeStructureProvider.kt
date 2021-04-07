package eu.ibagroup.formainframe.explorer.ui

import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

class FileExplorerTreeStructureProvider : ExplorerTreeStructureProvider() {
  override fun modifyOurs(
    parent: ExplorerTreeNode<*>,
    children: Collection<ExplorerTreeNode<*>>,
    settings: ExplorerViewSettings
  ): MutableCollection<ExplorerTreeNode<*>> {
    return children.toMutableList()
  }
}