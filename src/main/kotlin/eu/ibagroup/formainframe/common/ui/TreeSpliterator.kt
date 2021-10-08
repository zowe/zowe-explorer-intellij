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

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.util.ui.tree.AbstractTreeModel
import java.util.*
import java.util.function.Consumer

class TreeSpliterator(
  private val treeModel: AbstractTreeModel,
  private val startNode: AbstractTreeNode<*>
) : Spliterator<AbstractTreeNode<*>> {

  override fun tryAdvance(action: Consumer<in AbstractTreeNode<*>>?): Boolean {
    TODO("Not yet implemented")
  }

  override fun trySplit(): Spliterator<AbstractTreeNode<*>> {
    TODO("Not yet implemented")
  }

  override fun estimateSize() = if (treeModel.isLeaf(startNode)) 1 else Long.MAX_VALUE

  override fun characteristics(): Int {
    return Spliterator.DISTINCT or Spliterator.NONNULL or Spliterator.CONCURRENT
  }

}