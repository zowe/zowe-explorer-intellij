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

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
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
      .createNotification("Zowe config file detected", NotificationType.INFORMATION)
      .apply {
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
 * ZoweStartupActivity is needed to scan for the presence of a file at the project startup.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    showNotificationForAddUpdateZoweConfigIfNeeded(project)
  }
}
