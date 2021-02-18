package eu.ibagroup.formainframe.utils

import javax.swing.tree.TreePath

class TreePathIterator(private val treePath: TreePath) : Iterator<Any> {

  @Volatile
  private var currentIndex = 0

  override fun hasNext(): Boolean {
    return currentIndex < treePath.pathCount
  }

  override fun next(): Any {
    return treePath.getPathComponent(currentIndex++)
  }

}