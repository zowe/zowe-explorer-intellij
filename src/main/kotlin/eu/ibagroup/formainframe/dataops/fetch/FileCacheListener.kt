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

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.Query

/**
 * Interface for describing listener for events with file cache.
 */
interface FileCacheListener {

  /** File cache update event. */
  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {}

  /** Fetch failure event. */
  fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {}

  /** Cancelled fetch event. */
  fun <R : Any, Q : Query<R, Unit>> onFetchCancelled(query: Q) {}

  /** File cache clean event. */
  fun <R : Any, Q : Query<R, Unit>> onCacheCleaned(query: Q) {}

}
//
//fun <R, Q : Query<R>, File : VirtualFile> fileCacheListener(): FileCacheListener
