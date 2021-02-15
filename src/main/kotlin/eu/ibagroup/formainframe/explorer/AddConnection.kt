package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import eu.ibagroup.formainframe.config.MainframeConfigurable
import eu.ibagroup.formainframe.config.connect.ui.ConnectionConfigurable

class AddConnection : AnAction("Add Connection") {

  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().showSettingsDialog(e.project, MainframeConfigurable::class.java) { configurable ->
        val connectionConfigurable = configurable.configurables.filterIsInstance<ConnectionConfigurable>().firstOrNull()
        connectionConfigurable?.openAddDialog = true
      }
  }

}
