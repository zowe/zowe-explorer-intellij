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

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.tree.LeafState
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.common.message
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.dataops.BatchedRemoteQuery
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.formainframe.dataops.fetch.LibraryQuery
import eu.ibagroup.formainframe.explorer.ExplorerUnit
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.locked
import eu.ibagroup.formainframe.utils.toHumanReadableFormat
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Abstract class to represent a tree node in explorer
 */
abstract class FileFetchNode<Connection : ConnectionConfigBase, Value : Any, R : Any, Q : Query<R, Unit>, File : VirtualFile, U : ExplorerUnit<Connection>>(
  value: Value,
  project: Project,
  parent: ExplorerTreeNode<Connection, *>,
  unit: U,
  treeStructure: ExplorerTreeStructureBase
) : ExplorerUnitTreeNodeBase<Connection, Value, U>(value, project, parent, unit, treeStructure) {

  private val lock = ReentrantLock()
  private val condition = lock.newCondition()

  private val connectionError = "Error: Check connection"
  private val refreshLabel = "latest refresh:"
  private val outOfSync: String = "Out of sync"

  @Volatile
  private var needsToShowPlus = true

  private var cachedChildren: List<AbstractTreeNode<*>>? by locked(null, lock)

  private val fileFetchProvider
    get() = DataOpsManager.getService()
      .getFileFetchProvider(requestClass, queryClass, vFileClass)

  private val loadingNode by lazy { LoadingNode(notNullProject, this, explorer, treeStructure) }

  protected abstract val requestClass: Class<out R>

  protected abstract val queryClass: Class<out Query<*, *>>

  protected abstract val vFileClass: Class<out File>

  abstract val query: Q?

  /** Indicates whether the next children fetch should load more child elements to the children list */
  var needToLoadMore = false

  /** Indicates whether it is possible to fetch for the current node or is it currently being executed  */
  private var possibleToFetch = true

  protected abstract fun Collection<File>.toChildrenNodes(): List<AbstractTreeNode<*>>

  protected abstract fun makeFetchTaskTitle(query: Q): String

  /**
   * Refresh child node in the children list. Updates the actual reference in the list
   * @param oldChildNode the old child node reference to search through the list for
   * @param newChildNode the new child node instance to put to the list
   */
  fun refreshChildNode(oldChildNode: AbstractTreeNode<*>, newChildNode: AbstractTreeNode<*>) {
    lock.withLock {
      (cachedChildren as? MutableList)?.replaceAll {
        if (it.value == oldChildNode.value) newChildNode else it
      }
    }
  }

  /**
   * Method to build a list of error nodes with specified error text
   */
  private fun errorNode(text: String): List<ErrorNode<Connection>> {
    return listOf(ErrorNode(notNullProject, this, explorer, treeStructure, text = text))
  }

  /**
   * Method which is called when tree node is expanded or refresh is pressed on tree node.
   * It fetches the children nodes if no cached nodes are present / displays "loading..." during fetch / displays any Error if an error happened during fetch.
   * Normally, this function is called after nodes invalidation
   * @return collection of children tree nodes
   */
  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return lock.withLock {
      val childrenNodes = cachedChildren
      if (childrenNodes == null) {
        val q = fileFetchProvider.getRealQueryInstance(query) ?: query
        if (q != null && fileFetchProvider.isCacheValid(q)) {
          val fetched = fileFetchProvider.getCached(q)?.toMutableList()
          if (fetched != null && !needToLoadMore) {
            fetched
              .toChildrenNodes()
              .toMutableList()
              .apply {
                val batchedQ = q.castOrNull<BatchedRemoteQuery<*>>()
                if (batchedQ?.fetchNeeded == true) {
                  val itemsLeft = batchedQ.totalRows?.let { it - batchedQ.alreadyFetched }
                  add(LoadMoreNode(notNullProject, this@FileFetchNode, explorer, treeStructure, itemsLeft))
                }
              }
              .apply {
                if (isEmpty()) {
                  add(NoItemsFoundNode(notNullProject, this@FileFetchNode, explorer, treeStructure))
                }
              }
              .also {
                it.forEach { fetchedNode ->
                  if (fetchedNode is FileLikeDatasetNode || fetchedNode is LibraryNode) {
                    val explorerNode = fetchedNode as ExplorerTreeNode<*, *>
                    explorerNode.refreshSimilarNodes()
                  }
                }
                cachedChildren = it
              }
          } else {
            if (possibleToFetch) {
              possibleToFetch = false
              runBackgroundableTask(
                title = makeFetchTaskTitle(q),
                project = project,
                cancellable = true
              ) {
                var isMembersFetchOnInvalidDS = false
                // This functionality is going to skip the fetch of dataset members
                // in case the dataset became plain. This might happed when the dataset
                // got new mainframe attributes that changed it state but not the presentation
                if (q.request is LibraryQuery && this@FileFetchNode is LibraryNode) {
                  val dsNode = this@FileFetchNode as LibraryNode
                  val dataOpsManager = DataOpsManager.getService()
                  val attributes = dataOpsManager.tryToGetAttributes(dsNode.virtualFile)
                  if (attributes is RemoteDatasetAttributes && !attributes.hasDsOrg) {
                    isMembersFetchOnInvalidDS = true
                  }
                }
                if (!isMembersFetchOnInvalidDS) {
                  if (needToLoadMore) {
                    fileFetchProvider.loadMore(q, it)
                  } else {
                    fileFetchProvider.apply {
                      reload(q, it)
                      applyRefreshCacheDate(q, this@FileFetchNode, LocalDateTime.now())
                    }
                  }

                }
                needToLoadMore = false
                possibleToFetch = true
              }
            }
            (fetched?.toChildrenNodes()?.toMutableList() ?: mutableListOf()).apply { add(loadingNode) }
          }
        } else {
          errorNode(
            if (unit.connectionConfig == null) {
              connectionError
            } else {
              q?.let { it1 -> fileFetchProvider.getFetchedErrorMessage(it1) } ?: message("title.error")
            }
          ).also {
            cachedChildren = it
          }
        }
      } else {
        childrenNodes
      }
    }.toMutableSmartList()
  }

  /**
   * Displays the leaf state
   * @return leaf state object instance
   */
  override fun getLeafState(): LeafState {
    return if (needsToShowPlus) {
      LeafState.NEVER
    } else {
      LeafState.DEFAULT
    }
  }

  /**
   * Function gets the last updated date for query from fetchProvider and updates the node presentation accordingly
   * Important: In current implementation fetchProvider stores the last refresh timestamp for every node,
   * but presentation is only updated for mask nodes and filter nodes
   *
   * @param presentation
   * @return Void
   */
  fun updateRefreshDateAndTime(presentation: PresentationData) {
    val q = fileFetchProvider.getRealQueryInstance(query) ?: query
    if (q != null) {
      val lastKnownRefreshTime = fileFetchProvider.findCacheRefreshDateIfPresent(q)
      if (lastKnownRefreshTime != null) {
        presentation.addText(
          " $refreshLabel ${lastKnownRefreshTime.toHumanReadableFormat()}",
          SimpleTextAttributes.GRAYED_ATTRIBUTES
        )
      }
    } else presentation.addText(" $outOfSync", SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }

  /**
   * Method which is called to clean a cache during refresh or reload of the tree node
   * @param recursively - determines if all children nodes should clean the cache
   * @param cleanFetchProviderCache - also cleans cache of fetch provider for corresponding query.
   * @param cleanBatchedQuery - cleans current position of fetching items in batched query.
   * @param sendTopic - true if it is necessary to send message in CACHE_CHANGES topic and false otherwise.
   * @return Void
   */
  fun cleanCache(
    recursively: Boolean = true,
    cleanFetchProviderCache: Boolean = true,
    cleanBatchedQuery: Boolean = false,
    sendTopic: Boolean = true
  ) {
    val children = cachedChildren
    cachedChildren = null
    if (cleanFetchProviderCache) {
      query?.let {
        fileFetchProvider.cleanCache(it, sendTopic)
      }
    }
    if (cleanBatchedQuery) {
      fileFetchProvider.getRealQueryInstance(query)?.castOrNull<BatchedRemoteQuery<*>>()?.clear()
    }
    if (recursively) {
      children?.forEach {
        if (it is FileFetchNode<*, *, *, *, *, *>) {
          it.cleanCache(cleanBatchedQuery = cleanBatchedQuery)
        }
      }
    }
  }

}

// TODO: rework or remove as it is not customisable at the moment (recursively and cleanFetchProviderCache is omitted)
/**
 * Method to call cleanCache() of desired instance of the tree node. Tree node should be an instance of FileFetchNode
 * @param cleanBatchedQuery value to indicate whether it is needed to clean batched query
 */
fun ExplorerTreeNode<*, *>.cleanCacheIfPossible(cleanBatchedQuery: Boolean) {
  if (this is FileFetchNode<*, *, *, *, *, *>) {
    cleanCache(cleanBatchedQuery = cleanBatchedQuery)
  }
}
