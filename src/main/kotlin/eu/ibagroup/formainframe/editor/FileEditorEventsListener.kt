package eu.ibagroup.formainframe.editor

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.synchronizer.DocumentedSyncProvider
import eu.ibagroup.formainframe.dataops.content.synchronizer.SaveStrategy
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import eu.ibagroup.formainframe.vfs.MFVirtualFile

private val log = log<FileEditorEventsListener>()

class FileEditorEventsListener : FileEditorManagerListener.Before {
  override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
    val configService = service<ConfigService>()
    if (file is MFVirtualFile && !configService.isAutoSyncEnabled.get()) {
      val document = FileDocumentManager.getInstance().getDocument(file) ?: let {
        log.info("Document cannot be used here")
        return
      }
      if (
        !(document.text.toByteArray() contentEquals file.contentsToByteArray()) &&
        showSyncOnCloseDialog(file.name, source.project)
      ) {
        runModalTask(
          title = "Syncing ${file.name}",
          project = source.project,
          cancellable = true
        ) {
          runInEdt {
            FileDocumentManager.getInstance().saveDocument(document)
            val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(source.project))
            service<DataOpsManager>().getContentSynchronizer(file)?.synchronizeWithRemote(syncProvider, it)
          }
        }
      } else {
        runWriteActionInEdt {
          document.setText(String(file.contentsToByteArray()))
        }
      }
    }
    super.beforeFileClosed(source, file)
  }
}
