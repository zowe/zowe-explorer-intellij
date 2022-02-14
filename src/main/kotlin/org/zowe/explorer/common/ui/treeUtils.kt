/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.common.ui

import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ui.tree.StructureTreeModel
import com.intellij.util.ui.tree.TreeUtil
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.explorer.Explorer
import org.zowe.explorer.explorer.ui.ExplorerTreeNode
import org.zowe.explorer.explorer.ui.NodeData
import org.zowe.explorer.utils.service
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
