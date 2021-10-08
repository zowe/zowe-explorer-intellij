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

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.Query

interface FileCacheListener {

  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {}

  fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {}

  fun <R : Any, Q : Query<R, Unit>> onFetchCancelled(query: Q) {}

  fun <R : Any, Q : Query<R, Unit>> onCacheCleaned(query: Q) {}

}
//
//fun <R, Q : Query<R>, File : VirtualFile> fileCacheListener(): FileCacheListener