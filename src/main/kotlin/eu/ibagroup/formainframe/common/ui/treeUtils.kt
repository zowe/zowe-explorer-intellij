package eu.ibagroup.formainframe.common.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ui.tree.StructureTreeModel


class TreeInvalidator(
  private val rootProperty: () -> AbstractTreeNode<*>?,
  private val structureProperty: () -> StructureTreeModel<*>?
) {
  inline operator fun <reified T : Any> invoke(
    structure: Boolean = true,
    stopIfFound: Boolean = true,
    noinline predicate: (T) -> Boolean
  ) {
    invalidate(T::class.java, structure, stopIfFound, predicate)
  }

  operator fun <T : Any> invoke(
    value: T,
    structure: Boolean = true,
    stopIfFound: Boolean = true
  ) {
    invalidate(Any::class.java, structure, stopIfFound) { it == value }
  }

  fun <T : Any> invalidate(
    clazz: Class<out T>,
    structure: Boolean = true,
    stopIfFound: Boolean = true,
    predicate: (T) -> Boolean
  ) {
    val root = rootProperty()
    val treeModel = structureProperty()
    if (root != null && treeModel != null) {
      treeModel.invalidateByPredicate(clazz, root, structure, stopIfFound, predicate)
    }
  }

  private fun <T : Any> StructureTreeModel<*>.invalidateByPredicate(
    clazz: Class<out T>,
    startNode: AbstractTreeNode<*>,
    structure: Boolean = true,
    stopIfFound: Boolean = true,
    predicate: (T) -> Boolean
  ): Boolean {
    @Suppress("UNCHECKED_CAST")
    return if (clazz.isAssignableFrom(startNode.value::class.java)
      && predicate(startNode.value as T)
    ) {
      invalidate(startNode, structure)
      true
    } else {
      var foundAny = false
      for (child in startNode.children) {
        if (invalidateByPredicate(clazz, child, structure, stopIfFound, predicate).also {
            foundAny = it || foundAny
          } && stopIfFound) {
          return true
        }
      }
      foundAny
    }
  }

}

private fun <T : Any> StructureTreeModel<*>.invalidateByPredicate(
  clazz: Class<out T>,
  rootNode: AbstractTreeNode<*>,
  predicate: (T) -> Boolean,
  structure: Boolean = true,
  stopIfFound: Boolean = true
) {
  fun StructureTreeModel<*>.invalidateByPredicate(
    startNode: AbstractTreeNode<*>,
    value: Any,
    structure: Boolean = true
  ): Boolean {
    @Suppress("UNCHECKED_CAST")
    return if (clazz.isAssignableFrom(startNode.value::class.java)
      && predicate(startNode.value as T)
    ) {
      invalidate(startNode, structure)
      true
    } else {
      var foundAny = false
      for (child in startNode.children) {
        if (invalidateByPredicate(child, structure).also { foundAny = it || foundAny } && stopIfFound) {
          return true
        }
      }
      foundAny
    }
  }
  invalidateByPredicate(rootNode, predicate, structure)
}