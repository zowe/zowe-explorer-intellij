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