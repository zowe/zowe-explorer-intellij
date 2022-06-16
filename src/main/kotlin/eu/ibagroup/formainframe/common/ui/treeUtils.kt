/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.common.ui

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.ui.tree.TreeUtil
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.utils.service
import org.jetbrains.concurrency.Promise
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun <S : AbstractTreeStructure> StructureTreeModel<S>.promisePath(
  node: Any,
  tree: JTree,
): Promise<TreePath> {
  return promiseVisitor(node).thenAsync {
    TreeUtil.promiseVisit(tree, it)
  }
}

fun makeNodeDataFromTreePath (explorer: Explorer<*>, treePath: TreePath?): NodeData {
  val descriptor = (treePath?.lastPathComponent as DefaultMutableTreeNode).userObject as ExplorerTreeNode<*>
  val file = descriptor.virtualFile
  val attributes = if (file != null) {
    explorer.componentManager.service<DataOpsManager>().tryToGetAttributes(file)
  } else null
  return NodeData(descriptor, file, attributes)
}

fun TreePath.getVirtualFile(): VirtualFile? {
  val treeNode = (lastPathComponent as DefaultMutableTreeNode).userObject as ProjectViewNode<*>
  return if (treeNode is PsiFileNode) treeNode.virtualFile else if (treeNode is PsiDirectoryNode) treeNode.virtualFile else null
}