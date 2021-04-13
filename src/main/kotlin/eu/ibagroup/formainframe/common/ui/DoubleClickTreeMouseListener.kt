package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.util.EditSourceOnDoubleClickHandler
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.TreePath

class DoubleClickTreeMouseListener(
  private val tree: JTree,
  private val onDoubleClick: JTree.(TreePath) -> Unit
) : EditSourceOnDoubleClickHandler.TreeMouseListener(tree) {

  override fun processDoubleClick(e: MouseEvent, dataContext: DataContext, treePath: TreePath) {
    super.processDoubleClick(e, dataContext, treePath)
    onDoubleClick(tree, treePath)
  }

}