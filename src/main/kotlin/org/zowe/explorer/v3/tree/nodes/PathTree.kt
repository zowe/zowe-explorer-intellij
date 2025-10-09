/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

// TODO: doc
class PathTree {
  enum class PathState {
    INIT,
    BUSY,
    LOADED
  }

  private class PathTreeNode(
    val parent: PathTreeNode?,
    val pathElements: MutableList<Traversable> = mutableListOf(),
    var innerNodes: MutableMap<String, PathTreeNode> = mutableMapOf(),
    @Volatile
    var pathState: PathState = PathState.INIT
  )

  private val pathTreeNodes = mutableMapOf<String, PathTreeNode>()

  private fun formPathTree(
    pathStrings: List<String>,
    pathElement: Traversable? = null,
    pathTreeNodeParent: PathTreeNode? = null
  ): PathTreeNode {
    if (pathStrings.isEmpty()) throw Exception("Path cannot be empty")

    val nextSegment = pathStrings.first()
    val pathTreeNodesToModify = pathTreeNodeParent?.innerNodes ?: pathTreeNodes
    val pathTreeNode = pathTreeNodesToModify.getOrPut(nextSegment) { PathTreeNode(pathTreeNodeParent) }
    return if (pathStrings.size > 1) {
      formPathTree(pathStrings.subList(1, pathStrings.size), pathElement, pathTreeNode)
    } else {
      if (pathElement != null) pathTreeNode.pathElements.add(pathElement)
      pathTreeNode
    }
  }

  private fun findPathTreeNodeByPath(pathStrings: List<String>, pathTreeNodeParent: PathTreeNode? = null): PathTreeNode? {
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

  private fun hasAnyParentInState(node: PathTreeNode, state: PathState): Boolean {
    val parentNode = node.parent ?: return false
    if (parentNode.pathState == state) return true
    return hasAnyParentInState(parentNode, state)
  }

  fun getPathState(pathStrings: List<String>): PathState {
    val pathNode = formPathTree(pathStrings)
    return pathNode.pathState
  }

  fun isAnyChildBusy(pathStrings: List<String>): Boolean {
    val pathNode = formPathTree(pathStrings)
    return hasAnyChildInState(pathNode, PathState.BUSY)
  }

  fun isAnyParentBusy(pathStrings: List<String>): Boolean {
    val pathNode = formPathTree(pathStrings)
    return hasAnyParentInState(pathNode, PathState.BUSY)
  }

  fun setPathState(pathStrings: List<String>, newPathState: PathState) {
    val pathNode = findPathTreeNodeByPath(pathStrings) ?: throw Exception("Path $pathStrings is not registered")
    pathNode.pathState = newPathState
  }

  fun getPathElements(pathStrings: List<String>): List<Traversable> {
    return findPathTreeNodeByPath(pathStrings)?.pathElements ?: listOf()
  }

  fun updatePath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> {
    val pathNode = findPathTreeNodeByPath(pathStrings) ?: throw Exception("Path $pathStrings is not registered")
    val newPathElementsToAdd = newPathElements
      .filter { newPathElement ->
        pathNode.pathElements
          .filter { it !is Ephemeral }
          .find { oldPathElement -> newPathElement.path == oldPathElement.path } == null
      }
    pathNode.pathElements
      .removeIf { oldPathElement ->
        oldPathElement is Ephemeral
          || newPathElements.find { newPathElement -> newPathElement.path == oldPathElement.path } == null
      }
    pathNode.pathElements.addAll(newPathElementsToAdd)
    return pathNode.pathElements
  }

  fun resetPath(pathStrings: List<String>, newPathElements: List<Traversable>): List<Traversable> {
    val pathNode = findPathTreeNodeByPath(pathStrings) ?: throw Exception("Path $pathStrings is not registered")
    pathNode.pathElements.clear()
    pathNode.pathElements.addAll(newPathElements)
    return pathNode.pathElements
  }

  // TODO: check if it is needed
  fun removePathElement(pathElement: Traversable) {
    val pathTreeNode = findPathTreeNodeByPath(pathElement.path)
    pathTreeNode?.pathElements?.remove(pathElement)
    if ((pathTreeNode?.pathElements?.isEmpty() ?: false) && (pathTreeNode?.innerNodes?.isEmpty() ?: false)) {
      val (pathSegmentToRemove, _) = pathTreeNode?.parent
        ?.innerNodes
        ?.entries
        ?.find { (_, innerNode) -> innerNode == pathTreeNode }
        ?: return
      pathTreeNode?.parent?.innerNodes?.remove(pathSegmentToRemove)
    }
  }
}