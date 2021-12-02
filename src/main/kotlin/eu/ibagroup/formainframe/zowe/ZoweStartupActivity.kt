/*
 * This is a property of IBA Group
 */
package eu.ibagroup.formainframe.zowe

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialog
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.toDialogState
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.zowe.service.ZoweConfigService
import eu.ibagroup.formainframe.zowe.service.ZoweConfigState

const val ZOWE_CONFIG_NAME = "zowe.config.json"

/**
 * Checks if zowe config needs to be synchronized for crudable configs and show notification with sync action.
 * @param project - project instance to check zoweConfig.
 * @return Nothing
 */
fun showNotificationForAddUpdateZoweConfigIfNeeded (project: Project) {
  val zoweConfigService = project.service<ZoweConfigService>()
  val zoweConfigState = zoweConfigService.getZoweConfigState()

  if (zoweConfigState == ZoweConfigState.NEED_TO_ADD) {
    NotificationGroupManager.getInstance().getNotificationGroup(EXPLORER_NOTIFICATION_GROUP_ID)
      .createNotification("Zowe config file detected", NotificationType.INFORMATION)
      .apply {
        addAction(object : DumbAwareAction("Add Zowe Connection") {
          override fun actionPerformed(e: AnActionEvent) {
            project.service<ZoweConfigService>().addOrUpdateZoweConfig(false, true)
            hideBalloon()
          }
        }).notify(project)
      }
  }
}

/**
 * ZoweStartupActivity is needed to scan for the presence of a file at the project startup.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweStartupActivity: StartupActivity {

  override fun runActivity(project: Project) {
    showNotificationForAddUpdateZoweConfigIfNeeded(project)
  }
}
