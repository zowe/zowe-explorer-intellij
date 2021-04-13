package eu.ibagroup.formainframe.dataops.synchronizer

import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.utils.sendTopic

fun interface AfterSaveVfsListenerCallback : (List<VFileContentChangeEvent>) -> Unit

@JvmField
val FILES_SAVED = Topic.create("filesSaved", AfterSaveVfsListenerCallback::class.java)

class AfterSaveVfsListener : AsyncFileListener {

  override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier {
    return object : AsyncFileListener.ChangeApplier {
      override fun afterVfsChange() {
        events.filterIsInstance<VFileContentChangeEvent>()
          .also {
            if (it.isNotEmpty()) {
              sendTopic(FILES_SAVED)(it)
            }
          }
      }
    }
  }
}