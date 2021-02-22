package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import eu.ibagroup.formainframe.dataops.api.api
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.r2z.InfoAPI

class AddAndTestConnection(val state: ConnectionDialogState, val crudable: Crudable, val project: Project?) {

  private lateinit var dialog: ConnectionDialog

  fun showUntilTested() : ConnectionDialogState? {

    while (true) {
      dialog = ConnectionDialog(state = state, crudable = crudable)
      if (dialog.showAndGet())
        if (performTestRequest(dialog.state.connectionUrl, dialog.state.isAllowSsl, project)) {
          return dialog.state
        } else {
          Messages.showErrorDialog("Cannot establish connection with: ${dialog.state.connectionUrl}","Cannot Connect")
      } else {
        break
      }
    }
    return null
  }

  private fun performTestRequest(url: String, isAllowSelfSigned: Boolean, project: Project?): Boolean {
    var isConnectionOk = false

    ProgressManager.getInstance().run(object : Task.Modal(project, "Testing Connection to $url", false) {
      override fun run(indicator: ProgressIndicator) {
        try {
          isConnectionOk = api<InfoAPI>(url, isAllowSelfSigned).getSystemInfo().execute().isSuccessful
        } catch (t: Throwable) {

        }

      }
    })
    return isConnectionOk
  }

}