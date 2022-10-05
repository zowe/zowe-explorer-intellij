/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.zowe.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.password
import org.zowe.explorer.config.connect.username
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.runReadActionInEdtAndWait
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList

val ZOWE_PROJECT_PREFIX = "zowe-"

/**
 * ZoweConfigService base implementation.
 * @see ZoweConfigService
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweConfigServiceImpl(override val myProject: Project) : ZoweConfigService {

  private val configCrudable = ConfigService.instance.crudable

  override var zoweConfig: ZoweConfig? = null

  private val zoweConnectionName: String
    get() = "$ZOWE_PROJECT_PREFIX${myProject.name}"

  /**
   * Checks project contains zowe.config.json. If zowe config presented
   * it will parse it and save to object model inside zoweConfig field.
   * @return ZoweConfig instance if zowe.config.json is presented or null otherwise.
   */
  private fun scanForZoweConfig (): ZoweConfig? {
    val zoweConfigLocation = "${myProject.basePath}/$ZOWE_CONFIG_NAME"
    val zoweFile = runReadActionInEdtAndWait {
      VirtualFileManager.getInstance().findFileByNioPath(Path.of(zoweConfigLocation))
    } ?: return null
    return try {
      parseConfigJson(zoweFile.inputStream).also {
        it.extractSecureProperties(zoweFile.path.split("/").toTypedArray())
        zoweConfig = it
      }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Finds existing connection config related to zowe.config.json.
   * @return ConnectionConfig instance related to zowe config if it exists or null otherwise.
   */
  private fun findExistingConnection(): ConnectionConfig? {
    val zoweConnectionList = configCrudable.find<ConnectionConfig> {
      it.name == zoweConnectionName
    }.toList()
    return if (zoweConnectionList.isEmpty()) null else zoweConnectionList[0]
  }

  /**
   * Check if connection config related to zowe config is presented
   * then extracts existing uuid and generates a new one otherwise.
   * @return created or existing uuid.
   */
  private fun getOrCreateUuid(): String {
    return findExistingConnection()?.uuid ?: UUID.randomUUID().toString()
  }

  /**
   * Added notification about connection failure with action of force connection adding.
   * @param message - notification message.
   * @return Nothing.
   */
  private fun notifyUiOnConnectionFailure(message: String) {
    NotificationGroupManager.getInstance().getNotificationGroup(EXPLORER_NOTIFICATION_GROUP_ID)
      .createNotification(message, NotificationType.ERROR)
      .apply {
        addAction(object : DumbAwareAction("Add Anyway") {
          override fun actionPerformed(e: AnActionEvent) {
            addOrUpdateZoweConfig(checkConnection = false)
            hideBalloon()
          }
        })
        notify(myProject)
      }
  }

  /**
   * @see ZoweConfigService.addOrUpdateZoweConfig
   */
  override fun addOrUpdateZoweConfig(scanProject: Boolean, checkConnection: Boolean): ConnectionConfig? {
    val zoweConfig = if (scanProject) {
      scanForZoweConfig()
    } else this.zoweConfig
    zoweConfig ?: return null
    val username = zoweConfig.user ?: return null
    val password = zoweConfig.password ?: return null
    val zoweConnection = zoweConfig.toConnectionConfig()

    if (checkConnection) {
      val res = runTask("Testing Connection to ${zoweConnection.url}", myProject) {
        runCatching {
          service<DataOpsManager>().performOperation(InfoOperation(zoweConfig.toConnectionConfig()), it)
        }
      }
      if (res.isFailure) {
        notifyUiOnConnectionFailure("Connection to ${zoweConnection.url} failed.")
        return null
      }
    }

    val connectionOpt = configCrudable.addOrUpdate(zoweConnection)
    return if (connectionOpt.isEmpty) null else connectionOpt.get().also {
      CredentialService.instance.setCredentials(it.uuid, username, password)
      sendTopic(ZOWE_CONFIG_CHANGED).onConfigSaved(zoweConfig, zoweConnection)
    }
  }

  /**
   * Converts ZoweConfig to ConnectionConfig.
   * @param uuid - uuid returned connection.
   * @return converted ConnectionConfig.
   */
  fun ZoweConfig.toConnectionConfig(uuid: String): ConnectionConfig {
    val basePath = if (basePath.last() == '/') basePath.dropLast(1) else basePath
    val domain = if (port == null) host else "${host}:${port}"
    val zoweUrl = "${protocol}://${domain}${basePath}"
    val isAllowSelfSigned = !(rejectUnauthorized ?: false)
    val codePage = codePage
    val user = this.user
    val password = this.password

    if (user != null && password != null) {
      CredentialService.instance.setCredentials(uuid, user, password)
    }

    return ConnectionConfig(
      uuid,
      zoweConnectionName,
      zoweUrl,
      isAllowSelfSigned,
      codePage,
      ZVersion.ZOS_2_1,
      "${myProject.basePath}/${ZOWE_CONFIG_NAME}"
    )
  }

  /**
   * Converts to connection config with existing connection config uuid.
   * related to zowe config or generates a new one.
   * @return converted ConnectionConfig.
   */
  fun ZoweConfig.toConnectionConfig(): ConnectionConfig = toConnectionConfig(getOrCreateUuid())

  /**
   * @see ZoweConfigService.getZoweConfigState
   */
  override fun getZoweConfigState(scanProject: Boolean): ZoweConfigState {
    if (scanProject) {
      scanForZoweConfig()
    }
    val zoweConfig = zoweConfig ?: return ZoweConfigState.NOT_EXISTS
    val existingConnection = findExistingConnection() ?: return ZoweConfigState.NEED_TO_ADD
    val newConnection = zoweConfig.toConnectionConfig(existingConnection.uuid)

    val zoweUsername = zoweConfig.user ?: return ZoweConfigState.ERROR
    val zowePassword = zoweConfig.password ?: return ZoweConfigState.ERROR

    return if (existingConnection == newConnection &&
      username(newConnection) == zoweUsername &&
      password(newConnection) == zowePassword) {
      ZoweConfigState.SYNCHRONIZED
    } else {
      ZoweConfigState.NEED_TO_UPDATE
    }
  }

}
