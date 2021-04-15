package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import eu.ibagroup.formainframe.explorer.Explorer

private val singletonList = mutableListOf<AbstractTreeNode<*>>()
private val any = Any()

abstract class InfoNodeBase(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer,
  treeStructure: ExplorerTreeStructureBase
) :
  ExplorerTreeNode<Any>(any, project, parent, explorer, treeStructure) {

  override fun isAlwaysLeaf(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.addText(text, textAttributes)
  }

  protected abstract val text: String

  protected abstract val textAttributes: SimpleTextAttributes

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return singletonList
  }

}