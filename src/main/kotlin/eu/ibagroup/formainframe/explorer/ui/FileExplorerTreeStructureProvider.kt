package eu.ibagroup.formainframe.explorer.ui

import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

class FileExplorerTreeStructureProvider : ExplorerTreeStructureProvider() {
  override fun modifyOurs(
    parent: ExplorerTreeNodeBase<*>,
    children: Collection<ExplorerTreeNodeBase<*>>,
    settings: ExplorerViewSettings
  ): MutableCollection<ExplorerTreeNodeBase<*>> {
    return children.toMutableList()
  }
}