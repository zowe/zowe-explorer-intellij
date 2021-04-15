package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

abstract class ExplorerTreeStructureProvider : TreeStructureProvider {

  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings?
  ): MutableCollection<AbstractTreeNode<*>> {
    if (parent is ExplorerTreeNode && settings is ExplorerViewSettings) {
      val castedChildren = children.filterIsInstance<ExplorerTreeNode<*>>()
      if (castedChildren.size == children.size) {
        @Suppress("UNCHECKED_CAST")
        return modifyOurs(parent, castedChildren, settings) as MutableCollection<AbstractTreeNode<*>>
      }
    }
    return children
  }

  protected abstract fun modifyOurs(
    parent: ExplorerTreeNode<*>,
    children: Collection<ExplorerTreeNode<*>>,
    settings: ExplorerViewSettings
  ): MutableCollection<ExplorerTreeNode<*>>

}