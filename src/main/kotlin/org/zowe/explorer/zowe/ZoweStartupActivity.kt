/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.zowe

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.zowe.service.ZOWE_CONFIG_CHANGED
import org.zowe.explorer.zowe.service.ZoweConfigHandler
import org.zowe.explorer.zowe.service.ZoweConfigService
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.kotlinsdk.zowe.config.ZoweConfig

const val ZOWE_CONFIG_NAME = "zowe.config.json"

/**
 * Checks if zowe config needs to be synchronized with crudable configs and show notification with sync action.
 * @param project - project instance to check zoweConfig.
 * @return Nothing.
 */
fun showNotificationForAddUpdateZoweConfigIfNeeded(project: Project) {
  val zoweConfigService = project.service<ZoweConfigService>()
  val zoweConfigState = zoweConfigService.getZoweConfigState()

  if (zoweConfigState == ZoweConfigState.NEED_TO_ADD) {
    NotificationGroupManager.getInstance().getNotificationGroup(EXPLORER_NOTIFICATION_GROUP_ID)
      .createNotification("Zowe config file detected", NotificationType.INFORMATION).apply {
        subscribe(ZOWE_CONFIG_CHANGED, object : ZoweConfigHandler {
          override fun onConfigSaved(config: ZoweConfig, connectionConfig: ConnectionConfig) {
            hideBalloon()
          }
        })
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
 * Checks if zowe config has been deleted to be synchronized with crudable configs and show dialog for delete zowe config connection.
 * @param project - project instance to check zoweConfig.
 * @return Nothing.
 */
fun showDialogForDeleteZoweConfigIfNeeded(project: Project) {
  val zoweConfigService = project.service<ZoweConfigService>()
  val zoweConfigState = zoweConfigService.getZoweConfigState()
  if(zoweConfigState != ZoweConfigState.NEED_TO_ADD || zoweConfigState != ZoweConfigState.NOT_EXISTS) {
    val choice = Messages.showDialog(
      project,
      "Zowe config file has been deleted.\n" +
          "Would you like to delete the corresponding connection?\n" +
          "If you decide to leave the connection, it will be converted to a regular connection (username will be visible).",
      "Deleting Zowe Config connection",
      arrayOf(
        "Delete Connection", "Keep Connection"
      ),
      0,
      AllIcons.General.QuestionDialog
    )
    if (choice == 0) {
      zoweConfigService.deleteZoweConfig()
    }
  }
  zoweConfigService.zoweConfig = null
  zoweConfigService.checkAndRemoveOldZoweConnection()
}

/**
 * ZoweStartupActivity is needed to scan for the presence of a file at the project startup.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    showNotificationForAddUpdateZoweConfigIfNeeded(project)
  }
}
