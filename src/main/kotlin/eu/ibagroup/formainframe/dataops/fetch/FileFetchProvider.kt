package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.FetchCallback
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.fetchAdapter
import eu.ibagroup.formainframe.utils.sendTopic

fun <File : VirtualFile> emptyCallback() = fetchAdapter<Collection<File>> {  }

interface FileFetchProvider<R : Any, Q : Query<R>, File : VirtualFile> {

  companion object {
    @JvmStatic
    val CACHE_UPDATED = Topic.create("cacheUpdated", FileCacheListener::class.java)

    @JvmStatic
    val EP = ExtensionPointName.create<FileFetchProviderFactory>("eu.ibagroup.formainframe.fileDataProvider")
  }

  fun getCached(query: Q): Collection<File>?

  fun forceReloadSynchronous(query: Q, project: Project?): Collection<File>

  fun forceReloadSynchronous(query: Q): Collection<File> = forceReloadSynchronous(query, null)

  fun cleanCache(query: Q)

  fun forceReloadAsync(query: Q, callback: FetchCallback<Collection<File>> = emptyCallback(), project: Project?)

  fun forceReloadAsync(query: Q, callback: FetchCallback<Collection<File>> = emptyCallback()) {
    forceReloadAsync(query, callback, null)
  }

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*>>

  val vFileClass: Class<out File>

}