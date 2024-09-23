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

package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.Query
import java.time.LocalDateTime

/** Interface that represents the file fetch provider and operations available for it */
interface FileFetchProvider<R : Any, Q : Query<R, Unit>, File : VirtualFile> {

  companion object {
    @JvmField
    val CACHE_CHANGES = Topic.create("cacheUpdated", FileCacheListener::class.java)

    @JvmField
    val EP = ExtensionPointName.create<FileFetchProviderFactory>("eu.ibagroup.formainframe.fileDataProvider")
  }

  fun getCached(query: Q): Collection<File>?

  fun isCacheValid(query: Q): Boolean

  fun getFetchedErrorMessage(query: Q): String? = "Error"

  fun cleanCache(query: Q, sendTopic: Boolean = true)

  fun reload(query: Q, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE)

  /** Function for "load more" nodes */
  fun loadMore(query: Q, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE)

  /**
   * Function to fetch attributes for single element. The main purpose - to update the element's attributes without
   * reloading the full elements list
   * @param elemQuery the query related exactly to the element to fetch attributes
   * @param fullListQuery the parent query for the element's parent to make further updates to the whole list
   * @param progressIndicator the progress indicator to make it possible to cancel the operation
   */
  fun fetchSingleElemAttributes(
    elemQuery: Q,
    fullListQuery: Q,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  )

  /**
   * Function adds (node,query) pair with @param lastRefresh into the corresponding fetch provider refreshCacheState map
   *
   * @param query
   * @param node
   * @param lastRefresh
   * @return Void
   */
  fun applyRefreshCacheDate(query: Q, node: AbstractTreeNode<*>, lastRefresh: LocalDateTime)

  /**
   * Function finds the lastRefresh date by query in refreshCacheSate map and returns it.
   * If date was not found then returns null
   *
   * @param query
   * @return LocalDateTime instance or null
   */
  fun findCacheRefreshDateIfPresent(query: Q): LocalDateTime?

  /**
   * File fetch provider contains all list of queries inside.
   * If the query was created with default parameters - it will find query with real parameters.
   * It was made firstly for BatchedRemoteQuery.
   * @param query supposed query.
   * @return real query instance or null if it was not found.
   */
  fun <Q : Query<R, Unit>> getRealQueryInstance(query: Q?): Q?

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*, *>>

  val vFileClass: Class<out File>

}
