/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes.path

import org.zowe.explorer.v3.tree.nodes.Ephemeral
import org.zowe.explorer.v3.tree.nodes.Traversable
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Path tree representation. Provides the way of storing [org.zowe.explorer.v3.tree.nodes.Traversable] objects,
 * as well as manipulating them.
 *
 * The tree is organized as a trie where each path segment forms a level. Each node in the trie
 * holds a list of [Traversable] elements and a [PathState] that tracks whether the node's content
 * is being loaded, already loaded, or the path is just initialized.
 *
 * Example structure for a connection to `my.host.com` with two dataset masks (`SYS1.**`, `USER.**`)
 * and a USS path (`/u/user`):
 * ```
 * pathTreeNodes
 * └── "my.host.com"                          (basePath[0] — host)
 *     └── "files"                            (basePath[1] — system type)
 *         ├── "ds"                           (basePath[2] — system name)
 *         │   ├── "SYS1.**"                  [LOADED] elements: [DatasetMaskNodeDescriptor("SYS1.**")]
 *         │   │   ├── "SYS1.PARMLIB"         elements: [PartitionedDatasetNodeDescriptor("SYS1.PARMLIB")]
 *         │   │   │   ├── "IEASYS00"         elements: [MemberNodeDescriptor("IEASYS00")]
 *         │   │   │   └── "IEASYS01"         elements: [MemberNodeDescriptor("IEASYS01")]
 *         │   │   └── "SYS1.PROCLIB"         elements: [SequentialDatasetNodeDescriptor("SYS1.PROCLIB")]
 *         │   └── "USER.**"                  [BUSY]   elements: [DatasetMaskNodeDescriptor("USER.**")]
 *         └── "uss"
 *             └── "/u/user"                  [LOADED] elements: [UssFilterNodeDescriptor("/u/user")]
 *                 ├── "file.txt"             elements: [UssFileNodeDescriptor("file.txt")]
 *                 └── "subdir"               elements: [UssFolderNodeDescriptor("subdir")]
 * ```
 *
 * The base path for datasets is `["host", "files", "ds"]`, for USS — `["host", "files", "uss"]`,
 * for JES — `["host", "jes", "jobs"]`. Filter names and element names extend the path further.
 *
 * Thread safety: all public methods are synchronized via [ReentrantReadWriteLock].
 * Read operations acquire a read lock, write operations acquire a write lock.
 * Returned lists are defensive copies that are safe to iterate outside the lock.
 */
open class PathTree {
  /**
   * Represents a respective path state.
   * INIT - the path is initialized only.
   * BUSY - the path has some operation in progress.
   * LOADED - the path is already loaded by some operation
   */
  enum class PathState {
    INIT,
    BUSY,
    LOADED
  }

  private val lock = ReentrantReadWriteLock()

  /** Currently initialized and actual path tree to operate on */
  private val pathTreeNodes = mutableMapOf<String, PathTreeNode>()

  /**
   * A path tree node representation
   * @property parent the parent node (null if it is a root)
   * @property pathElements the elements that are stored under the path
   * @property innerNodes the inner nodes of the current path tree node
   * @property pathState the respective [PathState] of the path
   */
  private class PathTreeNode(
    val parent: PathTreeNode?,
    val pathElements: MutableList<Traversable> = mutableListOf(),
    var innerNodes: MutableMap<String, PathTreeNode> = mutableMapOf(),
    var pathState: PathState = PathState.INIT,
  )

  /**
   * Form a path tree from the provided path.
   * Returns the last element of the path strings list if the path is already initialized.
   * If the path is not initialized yet, will create all the elements down the path and return the last one in the path
   * @param pathStrings the path strings list to form the path from
   * @param pathTreeNodeParent the parent node for the next path string in the list (is used for recursion)
   * @return the final formed [PathTreeNode]
   */
  private fun formPathTree(
    pathStrings: List<String>,
    pathTreeNodeParent: PathTreeNode? = null
  ): PathTreeNode {
    if (pathStrings.isEmpty()) throw Exception("Path cannot be empty")

    val nextSegment = pathStrings.first()
    val pathTreeNodesToModify = pathTreeNodeParent?.innerNodes ?: pathTreeNodes
    val pathTreeNode = pathTreeNodesToModify.getOrPut(nextSegment) { PathTreeNode(pathTreeNodeParent) }
    return if (pathStrings.size > 1) {
      formPathTree(pathStrings.subList(1, pathStrings.size), pathTreeNode)
    } else {
      pathTreeNode
    }
  }

  /**
   * Find a path tree node by the provided path.
   * In contrast to the [formPathTree] will return null if the path is not yet initialized
   * @param pathStrings the path strings list to get the [PathTreeNode] by
   * @param pathTreeNodeParent the parent element of the next path tree node (is used for recursion)
   */
  private fun findPathTreeNodeByPath(
    pathStrings: List<String>,
    pathTreeNodeParent: PathTreeNode? = null
  ): PathTreeNode? {
    if (pathStrings.isEmpty()) return pathTreeNodeParent

    val nextSegment = pathStrings.first()
    val nextInnerNodes = pathTreeNodeParent?.innerNodes ?: pathTreeNodes
    val pathTreeNode = nextInnerNodes.getOrDefault(nextSegment, null)
    return if (pathTreeNode == null) {
      null
    } else if (pathStrings.size > 1) {
      findPathTreeNodeByPath(pathStrings.subList(1, pathStrings.size), pathTreeNode)
    } else {
      pathTreeNode
    }
  }

  /**
   * Check if there is any child node in the provided [state]
   * @param node the path tree node to check children nodes of
   * @param state the state to check the children nodes for
   * @return true if there is a child in the provided [state], false otherwise
   */
  private fun hasAnyChildInState(node: PathTreeNode, state: PathState): Boolean {
    for (childNode in node.innerNodes.values) {
      if (childNode.pathState == state) {
        return true
      }
      if (hasAnyChildInState(childNode, state)) {
        return true
      }
    }
    return false
  }

  /**
   * Check if there is any parent node in the provided [state]
   * @param node the path tree node to check parent nodes of
   * @param state the state to check the parent nodes for
   * @return true if there is a parent node in the provided [state], false otherwise
   */
  private fun hasAnyParentInState(node: PathTreeNode, state: PathState): Boolean {
    val parentNode = node.parent ?: return false
    if (parentNode.pathState == state) return true
    return hasAnyParentInState(parentNode, state)
  }

  /**
   * Get the path state if initialized, initialize the path with [PathState.INIT] otherwise
   * @param pathStrings the path strings list to get or initialize the path by
   * @return the path state of the path (stored or initialized)
   */
  fun getOrInitPathState(pathStrings: List<String>): PathState = lock.write {
    val pathNode = formPathTree(pathStrings)
    pathNode.pathState
  }

  /**
   * Set the path state
   * @param pathStrings the path strings list to set the path state by
   * @param newPathState the path state to set for the path
   */
  fun setPathState(pathStrings: List<String>, newPathState: PathState): Unit = lock.write {
    val pathNode = formPathTree(pathStrings)
    pathNode.pathState = newPathState
  }

  /**
   * Check is any child node busy under the specified path
   * @param pathStrings the path strings list to check children nodes for
   * @return true if there is a child in the [PathState.BUSY] state, false otherwise
   */
  fun isAnyChildBusy(pathStrings: List<String>): Boolean = lock.read {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    hasAnyChildInState(pathNode, PathState.BUSY)
  }

  /**
   * Check is any parent node busy under the specified path
   * @param pathStrings the path strings list to check parent nodes for
   * @return true if there is a parent in the [PathState.BUSY] state, false otherwise
   */
  fun isAnyParentBusy(pathStrings: List<String>): Boolean = lock.read {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    hasAnyParentInState(pathNode, PathState.BUSY)
  }

  /**
   * Get stored elements under the specified path
   * @param pathStrings the path strings list to get a path tree node with the path elements
   * @return list of the stored path elements (or empty list if there is no elements under the path)
   */
  fun getPathElements(pathStrings: List<String>): List<Traversable> = lock.read {
    findPathTreeNodeByPath(pathStrings)?.pathElements?.toList() ?: listOf()
  }

  /**
   * Get the element under the specified [placingPath] by the [elemName]
   * @return the found element or null if it does not exist
   */
  fun getPathElement(placingPath: List<String>, elemName: String): Traversable? = lock.read {
    findPathTreeNodeByPath(placingPath)?.pathElements?.find { it.elemName == elemName }
  }

  /**
   * Merge-update the specified path with the new path elements.
   * Removes all [Ephemeral] elements, then adds elements from [newPathElements] that are not yet present.
   * Existing non-ephemeral elements are preserved (important for keeping references of real fetched elements).
   * @param pathStrings the path strings list to update the respective path
   * @param newPathElements the new path elements to merge into the existing ones
   * @return the list of the path elements stored under the path after the update
   */
  fun updatePath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> = lock.write {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    pathNode.pathElements.removeIf { it is Ephemeral }
    val existingPaths = pathNode.pathElements.map { it.getExactPath() }.toSet()
    val newPathElementsToAdd = newPathElements.filter { it.getExactPath() !in existingPaths }
    pathNode.pathElements.addAll(newPathElementsToAdd)
    pathNode.pathElements.toList()
  }

  /**
   * Fully synchronize the specified path with [newPathElements].
   * Removes all [Ephemeral] elements and any existing elements whose [Traversable.getExactPath]
   * is not present in [newPathElements]. New elements that don't yet exist are added.
   * Existing elements that match by path are kept as-is (preserving object references).
   *
   * In contrast to [updatePath], this method **removes** stale elements that are no longer
   * reported by the server, making it suitable for full reload scenarios.
   * @param pathStrings the path strings list identifying the path to rewrite
   * @param newPathElements the authoritative list of elements from the server
   * @return the list of the path elements stored under the path after the rewrite
   */
  fun rewritePath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> = lock.write {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    val newPathElementsToAdd = newPathElements
      .filter { newPathElement ->
        pathNode.pathElements
          .filter { it !is Ephemeral }
          .find { oldPathElement -> newPathElement.getExactPath() == oldPathElement.getExactPath() } == null
      }
    pathNode.pathElements
      .removeIf { oldPathElement ->
        oldPathElement is Ephemeral
          || newPathElements.find { newPathElement -> newPathElement.getExactPath() == oldPathElement.getExactPath() } == null
      }
    pathNode.pathElements.addAll(newPathElementsToAdd)
    pathNode.pathElements.toList()
  }

  /**
   * Apply updates of the [applyFn] to elements by the specified path
   * @param pathStrings the path to apply the function on elements of
   * @param shouldUpdateChildren if "true", will update children elements recursively ("true" is a default)
   * @param applyFn the function to apply on the elements
   */
  fun applyToPathElements(
    pathStrings: List<String>,
    shouldUpdateChildren: Boolean = true,
    applyFn: (Traversable) -> Unit
  ): Unit = lock.write {
    applyToPathElementsInternal(pathStrings, shouldUpdateChildren, applyFn)
  }

  private fun applyToPathElementsInternal(
    pathStrings: List<String>,
    shouldUpdateChildren: Boolean,
    applyFn: (Traversable) -> Unit
  ) {
    val parentPathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    parentPathNode.pathElements.forEach(applyFn)
    if (shouldUpdateChildren) {
      parentPathNode.innerNodes
        .keys
        .forEach { nextPathStr ->
          applyToPathElementsInternal(pathStrings + listOf(nextPathStr), true, applyFn)
        }
    }
  }

  /**
   * Reset the path with the [newPathElements] entirely
   * @param pathStrings the path strings list to reset the respective path
   * @param newPathElements the new path elements to insert
   * @return the list of the path elements stored under the path after the reset
   */
  fun resetPath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> = lock.write {
    val pathNode = formPathTree(pathStrings)
    pathNode.pathElements.clear()
    pathNode.pathElements.addAll(newPathElements)
    pathNode.pathElements.toList()
  }

//  // TODO: check if it is needed
//  fun removePathElement(pathElement: Traversable) {
//    val pathTreeNode = findPathTreeNodeByPath(pathElement.path)
//      ?: throw Exception("Path ${pathElement.path} is not yet initialized")
//    pathTreeNode.pathElements.remove(pathElement)
//    if (pathTreeNode.pathElements.isEmpty() && pathTreeNode.innerNodes.isEmpty()) {
//      val (pathSegmentToRemove, _) = pathTreeNode.parent
//        ?.innerNodes
//        ?.entries
//        ?.find { (_, innerNode) -> innerNode == pathTreeNode }
//        ?: return
//      pathTreeNode.parent.innerNodes.remove(pathSegmentToRemove)
//    }
//  }
}