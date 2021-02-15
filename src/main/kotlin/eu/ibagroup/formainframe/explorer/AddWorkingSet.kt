package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.ui.WSConfigurable

class AddWorkingSet : AnAction("Create Working Set") {

  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().showSettingsDialog(null, WSConfigurable::class.java)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = configCrudable.getAll(ConnectionConfig::class.java).findAny().isPresent
  }
}
