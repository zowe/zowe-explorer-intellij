package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.IdeBundle.message
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

private val singletonList = mutableListOf<AbstractTreeNode<*>>()
private val any = Any()

class LoadingNode(
  project: Project,
  parent: ExplorerTreeNodeBase<*>,
  explorer: Explorer,
  treeStructure: ExplorerTreeStructureBase
) :
  ExplorerTreeNodeBase<Any>(any, project, parent, explorer, treeStructure) {

  override fun isAlwaysLeaf(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    @Suppress("DialogTitleCapitalization")
    presentation.addText(message("treenode.loading"), SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return singletonList
  }

}