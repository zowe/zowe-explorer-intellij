package eu.ibagroup.formainframe.zowe

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.zowe.service.ZoweConfigService
import eu.ibagroup.formainframe.zowe.service.ZoweConfigState

const val ZOWE_CONFIG_NAME = "zowe.config.json"

class ZoweStartupActivity: StartupActivity {

  lateinit var zoweConfigService: ZoweConfigService

  fun showNotificationForAddUpdateZoweConfig (project: Project, zoweConfigState: ZoweConfigState) {
    val notifyTitle = if (zoweConfigState == ZoweConfigState.NEED_TO_ADD) "Add Zowe Connection" else "Update Zowe Connection"

    NotificationGroupManager.getInstance().getNotificationGroup(EXPLORER_NOTIFICATION_GROUP_ID)
      .createNotification("Zowe config file detected", NotificationType.INFORMATION)
      .apply {
        addAction(object : DumbAwareAction(notifyTitle) {
          override fun actionPerformed(e: AnActionEvent) {
            zoweConfigService.addOrUpdateZoweConfig(false)
            hideBalloon()
          }
        }).notify(project)
      }
  }

  override fun runActivity(project: Project) {
    zoweConfigService = project.service()
    val zoweConfigState = zoweConfigService.getZoweConfigState()


    if (zoweConfigState == ZoweConfigState.NEED_TO_UPDATE || zoweConfigState == ZoweConfigState.NEED_TO_ADD) {
      showNotificationForAddUpdateZoweConfig(project, zoweConfigState)
    }

  }
}
