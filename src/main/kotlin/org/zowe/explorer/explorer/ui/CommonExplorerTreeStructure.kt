/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import org.zowe.explorer.explorer.Explorer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

private val PROVIDERS = SmartList(FileExplorerTreeStructureProvider())

/**
 * Class that represents the common explorer tree structure. Provides basic implementation to work with the explorer tree structure
 * @param explorer the explorer for which the tree structure should be built. Is used to interact with various components, that could be fetched by the explorer
 * @param project the project where the tree structure should be visible
 * @param rootNodeProvider the root node provider that uses explorer
 */
class CommonExplorerTreeStructure<Expl : Explorer<*>>(
  explorer: Expl,
  project: Project,
  private val rootNodeProvider: (explorer: Expl, project: Project, treeStructure: ExplorerTreeStructureBase) -> ExplorerTreeNode<*>
) : ExplorerTreeStructureBase(explorer, project) {

  private val valueToNodeMap = Collections.synchronizedMap(
    WeakHashMap<Any, ConcurrentLinkedQueue<ExplorerTreeNode<*>>>()
  )
  private val fileToNodeMap = Collections.synchronizedMap(
    WeakHashMap<VirtualFile, ConcurrentLinkedQueue<ExplorerTreeNode<*>>>()
  )

  /**
   * Put the node in the value to node map queue and the file to node map queue if the virtual file is present for the node
   * @param node the node to be registered
   */
  override fun registerNode(node: ExplorerTreeNode<*>) {
    valueToNodeMap.getOrPut(node.value) { ConcurrentLinkedQueue() }.add(node)
    val file = node.virtualFile ?: return
    fileToNodeMap.getOrPut(file) { ConcurrentLinkedQueue() }.add(node)
  }

  /**
   * Get the nodes queue from value to node map by the provided value. Empty set will be returned if the queue is not found
   * @param value the value to get the queue by
   */
  @Suppress("UNCHECKED_CAST")
  override fun <V : Any> findByValue(value: V): Collection<ExplorerTreeNode<V>> {
    return valueToNodeMap[value] as Collection<ExplorerTreeNode<V>>? ?: emptySet()
  }

  /**
   * Search for the values in all the value to node map queues by predicate
   * @param predicate the predicate to filter by
   */
  override fun findByPredicate(predicate: (ExplorerTreeNode<*>) -> Boolean): Collection<ExplorerTreeNode<*>> {
    return valueToNodeMap.values.flatten().filter(predicate)
  }

  /**
   * Get the nodes queue from file to node map by the provided value. Empty set will be returned if the queue is not found
   * @param file the virtual file to get the queue by
   */
  override fun findByVirtualFile(file: VirtualFile): Collection<ExplorerTreeNode<*>> {
    return fileToNodeMap[file] ?: emptySet()
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
