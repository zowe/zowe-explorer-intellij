package eu.ibagroup.formainframe.common.ui

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ui.tree.AsyncTreeModel
import com.intellij.ui.tree.StructureTreeModel
import java.util.concurrent.locks.ReentrantLock
import javax.swing.tree.TreePath
import kotlin.concurrent.withLock


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

fun AsyncTreeModel.getPath(node: Any): TreePath? {
  val lock = ReentrantLock()
  val condition = lock.newCondition()
  var path: TreePath? = null
  lock.withLock {
    onValidThread {
      getTreePath(node).then {
        path = it
        condition.signalAll()
      }
    }
  }
  lock.withLock {
    condition.await()
  }
  return path
}

fun TreePath.findCommonParentPath(other: TreePath): TreePath? {
  val minPathLength = pathCount.coerceAtMost(other.pathCount)
  val common = ArrayList<Any>(minPathLength)
  for (i in 0 until minPathLength) {
    val component = getPathComponent(i)
    val otherComponent = other.getPathComponent(i)
    if (component == otherComponent) {
      common.add(component)
    } else {
      break
    }
  }
  return if (common.isNotEmpty()) {
    TreePath(common.toTypedArray())
  } else null
}