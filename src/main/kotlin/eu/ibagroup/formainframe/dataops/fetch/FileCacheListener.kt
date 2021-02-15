package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.vfs.VirtualFile

interface FileCacheListener {

  fun <R : Any, Q : Query<R>, File : VirtualFile> onCacheUpdated(query: Q, files: Collection<File>)

}
//
//fun <R, Q : Query<R>, File : VirtualFile> fileCacheListener(): FileCacheListener