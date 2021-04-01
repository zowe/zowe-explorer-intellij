package eu.ibagroup.formainframe.dataops.fetch

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.dataops.Query
import org.jetbrains.concurrency.Promise

interface FileFetchProvider<R : Any, Q : Query<R, Unit>, File : VirtualFile> {

  companion object {
    @JvmStatic
    val CACHE_UPDATED = Topic.create("cacheUpdated", FileCacheListener::class.java)

    @JvmStatic
    val EP = ExtensionPointName.create<FileFetchProviderFactory>("eu.ibagroup.formainframe.fileDataProvider")
  }

  fun getCached(query: Q): Collection<File>?

  fun isCacheValid(query: Q): Boolean

  fun cleanCache(query: Q)

  fun reload(query: Q, progressIndicator: ProgressIndicator? = null)

  val requestClass: Class<out R>

  val queryClass: Class<out Query<*, *>>

  val vFileClass: Class<out File>

}