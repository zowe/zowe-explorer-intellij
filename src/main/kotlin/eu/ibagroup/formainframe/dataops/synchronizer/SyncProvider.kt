package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import java.io.IOException
import kotlin.jvm.Throws

//data class SyncHandler(
//  val getContent: (VirtualFile) -> ByteArray,
//  val updateContent: (VirtualFile) -> ByteArray,
//  val beforeSaveDecision: () -> Unit,
//  val afterSaveDecision: () -> Unit,
//  val progressIndicator: ProgressIndicator,
//  val saveStrategy: SaveStrategy,
//  val onThrowable: (file: VirtualFile, t: Throwable) -> Unit,
//  val onSyncEstablished: () -> Unit,
//)

interface SyncProvider {

  val file: VirtualFile

  val isReadOnly: Boolean

  fun beforeSaveDecision()

  fun afterSaveDecision()

  val progressIndicator: ProgressIndicator

  val saveStrategy: SaveStrategy

  @Throws(IOException::class)
  fun putInitialContent(content: ByteArray)

  @Throws(IOException::class)
  fun loadNewContent(content: ByteArray)

  @Throws(IOException::class)
  fun retrieveCurrentContent(): ByteArray

  fun onThrowable(t: Throwable)

  fun notifySyncStarted()

  fun waitForSyncStarted()

}