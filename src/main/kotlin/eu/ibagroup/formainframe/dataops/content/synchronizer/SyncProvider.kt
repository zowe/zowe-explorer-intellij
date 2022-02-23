package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import kotlin.jvm.Throws

const val SYNC_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.SyncNotificationGroupId"

// TODO: doc
interface SyncProvider {

  val file: VirtualFile

  val saveStrategy: SaveStrategy

  val isReadOnly: Boolean

  val vFileClass: Class<out VirtualFile>

  fun getDocument(): Document

  @Throws(IOException::class)
  fun putInitialContent(content: ByteArray)

  @Throws(IOException::class)
  fun loadNewContent(content: ByteArray)

  @Throws(IOException::class)
  fun retrieveCurrentContent(): ByteArray

  fun onThrowable(t: Throwable)

}
