/*
 * This is a property of IBA Group
 */
package eu.ibagroup.formainframe.zowe.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.password
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.InfoOperation
import eu.ibagroup.formainframe.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.runReadActionInEdtAndWait
import eu.ibagroup.formainframe.utils.runTask
import eu.ibagroup.formainframe.zowe.ZOWE_CONFIG_NAME
import eu.ibagroup.r2z.annotations.ZVersion
import eu.ibagroup.r2z.zowe.config.ZoweConfig
import eu.ibagroup.r2z.zowe.config.parseConfigJson
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
      it.zoweConfigPath != null && it.name == zoweConnectionName
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
          service<DataOpsManager>().performOperation(InfoOperation(zoweConnection.url, zoweConnection.isAllowSelfSigned), it)
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
    val isAllowSelfSigned = protocol == "https"
    val codePage = this.codePage

    return ConnectionConfig(
      uuid,
      zoweConnectionName,
      zoweUrl,
      isAllowSelfSigned,
      codePage,
      ZVersion.ZOS_2_1
    ).apply { zoweConfigPath = "${myProject.basePath}/${ZOWE_CONFIG_NAME}" }
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
