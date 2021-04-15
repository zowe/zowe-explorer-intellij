package eu.ibagroup.formainframe.common.ui

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.ui.tree.TreeUtil
import org.jetbrains.concurrency.Promise
import javax.swing.JTree
import javax.swing.tree.TreePath

fun <S : AbstractTreeStructure> StructureTreeModel<S>.promisePath(
  node: Any,
  tree: JTree,
): Promise<TreePath> {
  return promiseVisitor(node).thenAsync {
    TreeUtil.promiseVisit(tree, it)
  }
}