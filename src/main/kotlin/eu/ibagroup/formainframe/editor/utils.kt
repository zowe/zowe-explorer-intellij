package eu.ibagroup.formainframe.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder

fun showSyncOnCloseDialog(fileName: String, project: Project): Boolean {
  return MessageDialogBuilder
    .yesNo(
      title = "File $fileName Is Not Synced",
      message = "Do you want to sync the file with the Mainframe before it is closed?"
    )
    .asWarning()
    .ask(project = project)
}
