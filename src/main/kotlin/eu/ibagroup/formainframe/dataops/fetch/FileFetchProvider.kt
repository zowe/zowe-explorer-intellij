/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.Query

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
  fun loadMode(query: Q, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE)

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
