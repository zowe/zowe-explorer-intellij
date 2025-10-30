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

/** Path tree representation. Provides the way of storing [org.zowe.explorer.v3.tree.nodes.Traversable] objects, as well as manipulating them */
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
    @Volatile
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
  fun getOrInitPathState(pathStrings: List<String>): PathState {
    val pathNode = formPathTree(pathStrings)
    return pathNode.pathState
  }

  // TODO: doc
  private fun setPathStateCascadelly(pathNode: PathTreeNode, newPathState: PathState) {
    pathNode.pathState = newPathState
    pathNode.innerNodes
      .values
      .forEach { childPathNode ->
        setPathStateCascadelly(childPathNode, newPathState)
      }
  }

  /**
   * TODO: doc
   * Set the path state
   * @param pathStrings the path strings list to set the path state by
   * @param newPathState the path state to set for the path
   */
  fun setPathState(pathStrings: List<String>, newPathState: PathState, childrenShouldReflect: Boolean = false) {
    val pathNode = formPathTree(pathStrings)
    if (childrenShouldReflect) {
      setPathStateCascadelly(pathNode, newPathState)
    } else {
      pathNode.pathState = newPathState
    }
  }

  /**
   * Check is any child node busy under the specified path
   * @param pathStrings the path strings list to check children nodes for
   * @return true if there is a child in the [PathState.BUSY] state, false otherwise
   */
  fun isAnyChildBusy(pathStrings: List<String>): Boolean {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    return hasAnyChildInState(pathNode, PathState.BUSY)
  }

  /**
   * Check is any parent node busy under the specified path
   * @param pathStrings the path strings list to check parent nodes for
   * @return true if there is a parent in the [PathState.BUSY] state, false otherwise
   */
  fun isAnyParentBusy(pathStrings: List<String>): Boolean {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    return hasAnyParentInState(pathNode, PathState.BUSY)
  }

  /**
   * Get stored elements under the specified path
   * @param pathStrings the path strings list to get a path tree node with the path elements
   * @return list of the stored path elements (or empty list if there is no elements under the path)
   */
  fun getPathElements(pathStrings: List<String>): List<Traversable> {
    return findPathTreeNodeByPath(pathStrings)?.pathElements ?: listOf()
  }

  /**
   * Get the element under the specified [placingPath] by the [elemName]
   * @return the found element or null if it does not exist
   */
  fun getPathElement(placingPath: List<String>, elemName: String): Traversable? {
    return getPathElements(placingPath).find { it.elemName == elemName }
  }

  /**
   * TODO: doc update
   * Update the specified path with the new path elements.
   * Will remove all [org.zowe.explorer.v3.tree.nodes.Ephemeral] elements from the existing path,
   * refresh the stored elements info with the data from [newPathElements],
   * and store any non-found elements from the [newPathElements] list
   * @param pathStrings the path strings list to update the respective path
   * @param newPathElements the new path elements to refresh the existing ones or insert new ones
   * @return the list of the path elements stored under the path after the update
   */
  fun updatePath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> {
    val pathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    pathNode.pathElements.removeIf { oldPathElement -> oldPathElement is Ephemeral }
    pathNode.pathElements.map { oldPathElement ->
      newPathElements.find { newPathElement -> newPathElement.getExactPath() == oldPathElement.getExactPath() }
        ?: oldPathElement
    }
    val newPathElementsToAdd = newPathElements
      .filter { newPathElement ->
        pathNode.pathElements
          .find { oldPathElement -> newPathElement.getExactPath() == oldPathElement.getExactPath() } == null
      }
    pathNode.pathElements.addAll(newPathElementsToAdd)
    return pathNode.pathElements
  }

  // TODO: doc
  fun rewritePath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> {
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
    return pathNode.pathElements
  }

  /**
   * Update the specified path with its children recursively
   * @param pathStrings the path to update
   * @param updateFn the update function to apply on the elements
   */
  fun updatePathWithChildren(pathStrings: List<String>, updateFn: (Traversable) -> Unit) {
    val parentPathNode = findPathTreeNodeByPath(pathStrings)
      ?: throw Exception("Path $pathStrings is not yet initialized")
    parentPathNode.pathElements.forEach(updateFn)
    parentPathNode.innerNodes
      .keys
      .forEach { nextPathStr ->
        updatePathWithChildren(pathStrings + listOf(nextPathStr), updateFn)
      }
  }

  /**
   * Reset the path with the [newPathElements] entirely
   * @param pathStrings the path strings list to reset the respective path
   * @param newPathElements the new path elements to insert
   * @return the list of the path elements stored under the path after the reset
   */
  fun resetPath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> {
    val pathNode = formPathTree(pathStrings)
    pathNode.pathElements.clear()
    pathNode.pathElements.addAll(newPathElements)
    return pathNode.pathElements
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