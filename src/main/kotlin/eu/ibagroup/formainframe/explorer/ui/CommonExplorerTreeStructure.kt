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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.FileExplorerContentProvider
import eu.ibagroup.formainframe.telemetry.NotificationCompatibleException
import eu.ibagroup.formainframe.telemetry.NotificationsService
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

/**
 * Class that represents the common explorer tree structure. Provides basic implementation to work with the explorer tree structure
 * @param explorer the explorer for which the tree structure should be built. Is used to interact with various components, that could be fetched by the explorer
 * @param project the project where the tree structure should be visible
 * @param rootNodeProvider the root node provider that uses explorer
 */
class CommonExplorerTreeStructure<Expl : Explorer<*, *>>(
  explorer: Expl,
  project: Project,
  private val rootNodeProvider: (explorer: Expl, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<*, *>
) : ExplorerTreeStructureBase(explorer, project) {

  private val lock = ReentrantLock()

  private val valueToNodeMap = WeakHashMap<Any, MutableCollection<ExplorerTreeNode<*, *>>>()
  private val fileToNodeMap = WeakHashMap<VirtualFile, MutableCollection<ExplorerTreeNode<*, *>>>()

  /**
   * Put the node in the value to node map list
   * and the file to node map list if the virtual file is present for the node.
   * Cleans up the maps from an old node first
   * if the same node with old parameters is already registered for the same parent
   * @param node the node to be registered
   */
  override fun registerNode(node: ExplorerTreeNode<*, *>) {
    lock.withLock {
      val alreadyRegisteredNodes = valueToNodeMap
        .getOrPut(node.value) { LinkedList() }
        .filter { it.parent != null && it.parent == node.parent }
      if (alreadyRegisteredNodes.isNotEmpty()) {
        alreadyRegisteredNodes.forEach { alreadyRegisteredNode ->
          valueToNodeMap.getOrPut(node.value) { LinkedList() }
            .removeIf { nodeInMap -> nodeInMap.parent == alreadyRegisteredNode.parent }
          node.virtualFile?.let {
            fileToNodeMap.getOrPut(it) { LinkedList() }
              .removeIf { nodeInMap -> nodeInMap.parent == alreadyRegisteredNode.parent }
          }
        }
      }

      valueToNodeMap.getOrPut(node.value) { LinkedList() }.add(node)
      node.virtualFile?.let { fileToNodeMap.getOrPut(it) { LinkedList() }.add(node) }
    }
  }

  /**
   * Refresh the nodes that belong to the same virtual file as the node provided with the new presentation.
   * Will invalidate the parent of these nodes if the parent is a [DSMaskNode]
   * @param node the node to search the similar nodes in the maps by
   */
  override fun refreshSimilarNodes(node: ExplorerTreeNode<*, *>) {
    lock.withLock {
      if (node.virtualFile == null) {
        val exception = NotificationCompatibleException(
          "Virtual file is not found",
          "It is impossible to register new node. Virtual file is not found"
        )
        NotificationsService.getService().notifyError(exception, project)
        return
      }
      val nodesToRefresh = valueToNodeMap
        .getOrPut(node.virtualFile) { LinkedList() }
        .filter { it.parent != null && node.parent != it.parent }
      if (nodesToRefresh.isNotEmpty()) {
        nodesToRefresh.forEach {
          val virtualFile = it.virtualFile
          if (virtualFile == null) {
            val exception = NotificationCompatibleException(
              "Virtual file is not found",
              "It is impossible to register new node. Virtual file is not found"
            )
            NotificationsService.getService().notifyError(exception, project)
            return
          }
          if (it.parent is DSMaskNode) {
            val newChildNode = it.parent.prepareChildNodeFromMFVirtualFile(virtualFile)
            it.parent.refreshChildNode(it, newChildNode)
          }
        }
        nodesToRefresh
          .mapNotNull { it.parent }
          .filterIsInstance<DSMaskNode>()
          .distinct()
          .forEach {
            val query = it.query ?: return@forEach
            val view =
              FileExplorerContentProvider.getInstance().getExplorerView(project) ?: return@forEach
            val nodes = view.getNodesByQuery(query)
            view.invalidateNodes(nodes)
          }
      }
    }
  }

  /**
   * Get the nodes list from value to node map by the provided value. Empty list will be returned if the list is not found
   * @param value the value to get the list by
   */
  @Suppress("UNCHECKED_CAST")
  override fun <V : Any> findByValue(value: V): Collection<ExplorerTreeNode<*, V>> {
    return lock.withLock {
      valueToNodeMap[value] as Collection<ExplorerTreeNode<*, V>>? ?: emptyList()
    }
  }

  /**
   * Search for the values in all the value to node map lists by predicate
   * @param predicate the predicate to filter by
   */
  override fun findByPredicate(predicate: (ExplorerTreeNode<*, *>) -> Boolean): Collection<ExplorerTreeNode<*, *>> {
    return lock.withLock {
      valueToNodeMap.values.flatten().filter(predicate)
    }
  }

  /**
   * Get the nodes list from file to node map by the provided value. Empty list will be returned if the list is not found
   * @param file the virtual file to get the list by
   */
  override fun findByVirtualFile(file: VirtualFile): Collection<ExplorerTreeNode<*, *>> {
    return lock.withLock {
      fileToNodeMap[file] ?: emptyList()
    }
  }

  private val root by lazy { rootNodeProvider(explorer, project, this) }

  override fun getRootElement(): Any {
    return root
  }

  override fun commit() {
  }

  override fun hasSomethingToCommit() = false

  override fun getProviders() = PROVIDERS

  override val showVolser = true

  override val showMasksAndPathAsSeparateDirs = true

  override var showWorkingSetInfo = true

}
