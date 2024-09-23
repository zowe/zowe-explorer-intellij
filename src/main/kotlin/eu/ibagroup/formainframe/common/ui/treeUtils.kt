/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.common.ui

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.ui.tree.TreeUtil
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeNode
import eu.ibagroup.formainframe.explorer.ui.ExplorerTreeView
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.utils.castOrNull
import org.jetbrains.concurrency.Promise
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

/**
 * Promises to process nodes in the specified tree.
 * @param node node for processing.
 * @param tree working tree.
 * @return promise that will be succeeded when process is finished.
 */
fun <S : AbstractTreeStructure> StructureTreeModel<S>.promisePath(
  node: Any,
  tree: JTree,
): Promise<TreePath> {
  return promiseVisitor(node).thenAsync {
    TreeUtil.promiseVisit(tree, it)
  }
}

/**
 * Makes node data from a tree path.
 * @param explorer represents explorer object.
 * @param treePath path to a node in the tree.
 * @return node data [NodeData].
 */
fun <Con : ConnectionConfigBase> makeNodeDataFromTreePath(
  explorer: Explorer<Con, *>,
  treePath: TreePath?
): NodeData<Con>? {
  val descriptor = (treePath?.lastPathComponent as DefaultMutableTreeNode).userObject
    .castOrNull<ExplorerTreeNode<Con, *>>() ?: return null
  val file = descriptor.virtualFile
  val attributes = if (file != null) {
    DataOpsManager.getService().tryToGetAttributes(file)
  } else null
  return NodeData(descriptor, file, attributes)
}

/**
 * Returns virtual file from tree path object.
 */
fun TreePath.getVirtualFile(): VirtualFile? {
  val treeNode = (lastPathComponent as DefaultMutableTreeNode).userObject as ProjectViewNode<*>
  return if (treeNode is PsiFileNode) treeNode.virtualFile else if (treeNode is PsiDirectoryNode) treeNode.virtualFile else null
}

/**
 * Removes node from "invalidateOnExpand" collection of explorer view.
 * @param node node to remove.
 * @param view explorer view from which to return node.
 */
fun <Connection : ConnectionConfigBase> cleanInvalidateOnExpand(
  node: ExplorerTreeNode<*, *>,
  view: ExplorerTreeView<Connection, *, *>
) {
  view.myStructure.promisePath(node, view.myTree).onSuccess {
    val lastNode = it?.lastPathComponent
    if (view.myNodesToInvalidateOnExpand.contains(lastNode)) {
      synchronized(view.myNodesToInvalidateOnExpand) {
        view.myNodesToInvalidateOnExpand.remove(lastNode)
      }
    }
  }
}
