package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.Query

interface FileCacheListener {

  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>) {}

  fun <R : Any, Q : Query<R, Unit>> onFetchFailure(query: Q, throwable: Throwable) {}

}
//
//fun <R, Q : Query<R>, File : VirtualFile> fileCacheListener(): FileCacheListener