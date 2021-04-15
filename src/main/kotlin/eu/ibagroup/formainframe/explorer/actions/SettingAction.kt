package eu.ibagroup.formainframe.explorer.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import eu.ibagroup.formainframe.config.MainframeConfigurable

class SettingAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    ShowSettingsUtil.getInstance().showSettingsDialog(e.project, MainframeConfigurable::class.java)
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}