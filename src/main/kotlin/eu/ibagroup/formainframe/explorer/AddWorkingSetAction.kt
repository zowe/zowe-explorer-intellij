package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.config.MainframeConfigurable
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.connect.ui.AddAndTestConnection
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialog
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.initEmptyUuids
import eu.ibagroup.formainframe.config.sandboxCrudable
import eu.ibagroup.formainframe.config.ws.ui.WSConfigurable
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialog
import eu.ibagroup.formainframe.config.ws.ui.WorkingSetDialogState
import eu.ibagroup.formainframe.config.ws.ui.initEmptyUuids
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.nextUniqueValue

class AddWorkingSetAction : AnAction("Create Working Set") {

  override fun actionPerformed(e: AnActionEvent) {
    if (configCrudable.getAll<ConnectionConfig>().isEmpty()) {
      val state = AddAndTestConnection(ConnectionDialogState().initEmptyUuids(configCrudable), configCrudable, e.project).showUntilTested()
      if (state != null) {
        val urlConnection = state.urlConnection
        val connectionConfig = state.connectionConfig
        CredentialService.instance.setCredentials(connectionConfig.uuid, state.username, state.password)
        configCrudable.add(urlConnection)
        configCrudable.add(connectionConfig)
      } else {
        return
      }
    }
    val dialog = WorkingSetDialog(configCrudable, WorkingSetDialogState().initEmptyUuids(configCrudable))
    if (dialog.showAndGet()) {
      val state = dialog.state
      val workingSetConfig = state.workingSetConfig
      configCrudable.add(workingSetConfig)
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
