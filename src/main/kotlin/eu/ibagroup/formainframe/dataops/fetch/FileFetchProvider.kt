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

// TODO: doc
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

  fun cleanCache(query: Q)

  fun reload(query: Q, progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE)

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*, *>>

  val vFileClass: Class<out File>

}