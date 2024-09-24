/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package org.zowe.explorer.zowe.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.*
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ZOSMFConnectionConfigurable.Companion.warningMessageForDeleteConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.runTask
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.utils.toMutableList
import org.zowe.explorer.utils.write
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigService.Companion.lock
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.zowe.client.sdk.core.ZOSConnection
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.collections.set


const val ZOWE_CONFIG_NOTIFICATION_GROUP_ID = "org.zowe.explorerzowe.service.ZoweConfigNotificationGroupId"

const val ZOWE_PROJECT_PREFIX = "zowe-"

/**
 * ZoweConfigService base implementation.
 * @see ZoweConfigService
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class ZoweConfigServiceImpl(override val myProject: Project) : ZoweConfigService {

  companion object {
    /**
     * This function is required for testing purposes
     */
    private fun getResourceStream(strPath: String): InputStream? {
      return ZoweConfigServiceImpl::class.java.classLoader?.getResourceAsStream(strPath)
    }

    /**
     * Returns Zowe connection name
     */
    fun getZoweConnectionName(myProject: Project?, type: ZoweConfigType, profileName: String = "zosmf"): String {
      return if (type == ZoweConfigType.LOCAL)
        "${ZOWE_PROJECT_PREFIX}${type}-${profileName}/${myProject?.name}"
      else
        "${ZOWE_PROJECT_PREFIX}${type}-${profileName}"
    }

    /**
     * Returns path to Zowe configuration file
     */
    fun getZoweConfigLocation(myProject: Project?, type: ZoweConfigType): String {
      return if (type == ZoweConfigType.LOCAL)
        "${myProject?.basePath}/$ZOWE_CONFIG_NAME"
      else
        System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
    }

    /**
     * Returns profile name for current connection
     */
    fun getProfileNameFromConnName(connName: String): String {
      var profileName = connName.replace(Regex("^$ZOWE_PROJECT_PREFIX((local)|(global))+(-)"), "")
      profileName = profileName.replace(Regex("/.*$"), "")
      return profileName
    }

  }

  private val configCrudable = ConfigService.getService().crudable

  override var localZoweConfig: ZoweConfig? = null

  override var globalZoweConfig: ZoweConfig? = null

  /**
   * Displays an error notification if an error was received.
   * @param t thrown error.
   * @param title error text.
   */
  private fun notifyError(t: Throwable, title: String? = null) {
    Notifications.Bus.notify(
      Notification(
        ZOWE_CONFIG_NOTIFICATION_GROUP_ID,
        title ?: "Error with Zowe config file",
        t.message ?: t.toString(),
        NotificationType.ERROR
      )
    )
  }

  /**
   * Checks project contains zowe.config.json. If zowe config presented
   * it will parse it and save to object model inside zoweConfig field.
   * @return ZoweConfig instance if zowe.config.json is presented or null otherwise.
   */
  private fun scanForZoweConfig(type: ZoweConfigType): ZoweConfig? {

    val zoweFile = runReadAction {
      VirtualFileManager.getInstance().findFileByNioPath(Path.of(getZoweConfigLocation(myProject, type)))
    } ?: return null
    return try {
      zoweFile.inputStream.use { zoweFileInputStream ->
        parseConfigJson(zoweFileInputStream).also { tmpZoweConfig ->
          tmpZoweConfig.extractSecureProperties(zoweFile.path.split("/").toTypedArray())
          lock.write {
            if (type == ZoweConfigType.LOCAL)
              localZoweConfig = tmpZoweConfig
            else
              globalZoweConfig = tmpZoweConfig
          }
        }
      }
    } catch (e: Exception) {
      throw Exception("Cannot parse $type Zowe config file")
    }
  }

  /**
   * Finds A/ll existing connections related to zowe.config.json.
   * @return list of found ConnectionConfigs related to zowe config if it exists or null otherwise.
   */
  private fun findAllZosmfExistingConnection(type: ZoweConfigType): List<ConnectionConfig>? {
    val zoweConnectionList = configCrudable.find<ConnectionConfig> {
      val pattern =
        if (type == ZoweConfigType.LOCAL) {
          Regex("^(" + ZOWE_PROJECT_PREFIX + type + "-).*(/" + myProject.name + ")$")
        } else {
          Regex("^(" + ZOWE_PROJECT_PREFIX + type + "-).*")
        }
      it.name.matches(pattern) && it.zoweConfigPath == getZoweConfigLocation(myProject, type)
    }.collect(Collectors.toList())
    return if (zoweConnectionList.isEmpty()) null else zoweConnectionList
  }

  /**
   * Finds existing connection config related to zowe.config.json.
   * @return ConnectionConfig instance related to zowe config if it exists or null otherwise.
   */
  private fun findExistingConnection(type: ZoweConfigType, profileName: String): ConnectionConfig? {
    val zoweConnectionList = configCrudable.find<ConnectionConfig> {
      it.name == getZoweConnectionName(myProject, type, profileName) && it.zoweConfigPath == getZoweConfigLocation(
        myProject,
        type
      )
    }.collect(Collectors.toList())
    return if (zoweConnectionList.isEmpty()) null else zoweConnectionList[0]
  }

  /**
   * Check if connection config related to zowe config is presented
   * then extracts existing uuid and generates a new one otherwise.
   * @return created or existing uuid.
   */
  private fun getOrCreateUuid(type: ZoweConfigType, profileName: String): String {
    return findExistingConnection(type, profileName)?.uuid ?: UUID.randomUUID().toString()
  }

  /**
   * Added notification about connection failure with action of force connection adding.
   * @param title - notification title.
   * @param content - notification content.
   * @return Nothing.
   */
  private fun notifyUiOnConnectionFailure(title: String, content: String, type: ZoweConfigType) {
    NotificationGroupManager.getInstance().getNotificationGroup(EXPLORER_NOTIFICATION_GROUP_ID)
      .createNotification(title, content, NotificationType.ERROR)
      .apply {
        addAction(object : DumbAwareAction("Add Anyway") {
          override fun actionPerformed(e: AnActionEvent) {
            addOrUpdateZoweConfig(checkConnection = false, type = type)
            hideBalloon()
          }
        })
        notify(myProject)
      }
  }

  /**
   * Checks the zowe connection using InfoOperation. It is also needed to load zos version and
   * real owner of the connection (acceptable for alias users).
   * IMPORTANT!!! It modifies passed connection object by setting zVersion and owner.
   * @param zoweConnection connection to check and prepare.
   * @throws Throwable if something went wrong (connection was not established or one of requests was failed ...)
   */
  private fun testAndPrepareConnection(zoweConnection: ConnectionConfig) {
    val throwable = runTask("Testing Connection to ${zoweConnection.url}", myProject) { indicator ->
      return@runTask try {
        runCatching {
          service<DataOpsManager>().performOperation(InfoOperation(zoweConnection), indicator)
        }.onSuccess {
          indicator.text = "Retrieving z/OS information"
          val systemInfo = service<DataOpsManager>().performOperation(ZOSInfoOperation(zoweConnection), indicator)
          zoweConnection.zVersion = when (systemInfo.zosVersion) {
            "04.25.00" -> ZVersion.ZOS_2_2
            "04.26.00" -> ZVersion.ZOS_2_3
            "04.27.00" -> ZVersion.ZOS_2_4
            "04.28.00" -> ZVersion.ZOS_2_5
            else -> ZVersion.ZOS_2_1
          }
        }.onSuccess {
          indicator.text = "Retrieving user information"
          zoweConnection.owner = whoAmI(zoweConnection) ?: ""
        }.onFailure {
          throw it
        }
        null
      } catch (t: Throwable) {
        t
      }
    }
    if (throwable != null) {
      throw throwable
    }
  }

  /**
   * @see ZoweConfigService.addOrUpdateZoweConfig
   */
  override fun addOrUpdateZoweConfig(scanProject: Boolean, checkConnection: Boolean, type: ZoweConfigType) {
    return try {
      val zoweConfig = if (scanProject) {
        scanForZoweConfig(type)
      } else if (type == ZoweConfigType.LOCAL)
        this.localZoweConfig
      else
        this.globalZoweConfig
      zoweConfig ?: throw Exception("Cannot get $type Zowe config")

      var allPreparedConn = mutableListOf<ConnectionConfig>()
      if (checkConnection) {
        val failedURLs = mutableListOf<String>()
        allPreparedConn = testAndPrepareAllZosmfConnections(zoweConfig, type, failedURLs)
        if (failedURLs.isNotEmpty()) {
          val andMore = if (failedURLs.size > 3)
            "..."
          else
            ""
          notifyUiOnConnectionFailure(
            "Connection failed to:",
            "${failedURLs.joinToString(separator = ", <p>")} ${andMore}",
            type
          )
          return
        }
      }
      for (zosmfConnection in allPreparedConn) {
        val connectionOpt = configCrudable.addOrUpdate(zosmfConnection)
        if (!connectionOpt.isEmpty) {
          var topic = if (type == ZoweConfigType.LOCAL)
            LOCAL_ZOWE_CONFIG_CHANGED
          else
            GLOBAL_ZOWE_CONFIG_CHANGED
          sendTopic(topic).onConfigSaved(zoweConfig, zosmfConnection)
        }
      }

    } catch (e: Exception) {
      notifyError(e)
    }
  }

  /**
   * Convert ZOSConnection to ConnectionConfig
   * @param zosmfConnection connection to prepare.
   * @param type of zowe config
   * @return prepared ConnectionConfig
   */
  private fun prepareConnection(zosmfConnection: ZOSConnection, type: ZoweConfigType): ConnectionConfig {
    val username = zosmfConnection.user
    val password = zosmfConnection.password
    val zoweConnection = findExistingConnection(type, zosmfConnection.profileName)?.let {
      zosmfConnection.toConnectionConfig(it.uuid, it.zVersion, type = type)
    } ?: zosmfConnection.toConnectionConfig(UUID.randomUUID().toString(), type = type)
    CredentialService.getService().setCredentials(zoweConnection.uuid, username, password)
    return zoweConnection
  }

  /**
   * Convert all zosmf connections from zowe config file to ConnectionConfig and tests them
   * @param zoweConfig
   * @param type of zowe config
   * @return list of URLs which did not pass the test
   */
  private fun testAndPrepareAllZosmfConnections(
    zoweConfig: ZoweConfig,
    type: ZoweConfigType,
    failedURLs: MutableList<String>
  ): MutableList<ConnectionConfig> {
    var allPreparedConn = mutableListOf<ConnectionConfig>()
    for (zosmfConnection in zoweConfig.getListOfZosmfConections()) {
      val zoweConnection = prepareConnection(zosmfConnection, type)
      try {
        testAndPrepareConnection(zoweConnection)
      } catch (t: Throwable) {
        failedURLs.add(zoweConnection.url)
      }
      allPreparedConn.add(zoweConnection)
    }
    return allPreparedConn
  }

  /**
   * @see ZoweConfigService.deleteZoweConfig
   */
  override fun deleteZoweConfig(type: ZoweConfigType) {
    try {

      val zoweConnections = findAllZosmfExistingConnection(type) ?: throw Exception("Cannot find any zosmf connections")

      zoweConnections.forEach { zoweConnection ->

        val filesWorkingSets = configCrudable.getAll<FilesWorkingSetConfig>().toMutableList()
        val filesWsUsages = filesWorkingSets.filter { filesWsConfig ->
          filesWsConfig.connectionConfigUuid == zoweConnection.uuid
        }

        val jesWorkingSet = configCrudable.getAll<JesWorkingSetConfig>().toMutableList()
        val jesWsUsages = jesWorkingSet.filter { jesWsConfig ->
          jesWsConfig.connectionConfigUuid == zoweConnection.uuid
        }

        if (filesWsUsages.isEmpty() && jesWsUsages.isEmpty()) {
          CredentialService.getService().clearCredentials(zoweConnection.uuid)
          configCrudable.delete(zoweConnection)
        } else {
          val ret = warningMessageForDeleteConfig(filesWsUsages, jesWsUsages)

          if (ret == Messages.OK) {
            CredentialService.getService().clearCredentials(zoweConnection.uuid)
            configCrudable.delete(zoweConnection)
          }
        }
      }

    } catch (e: Exception) {
      notifyError(e)
    }
  }

  /**
   * @see ZoweConfigService.addZoweConfigFile
   */
  override fun addZoweConfigFile(state: ConnectionDialogState) {
    checkAndRemoveOldZoweConnection(ZoweConfigType.LOCAL)

    val jsonFileName = "${myProject.basePath}/${ZOWE_CONFIG_NAME}"
    val charset: Charset = StandardCharsets.UTF_8

    createZoweSchemaJsonIfNotExists()

    val urlRegex =
      "^(https?|http)://([-a-zA-Z0-9+&@#/%?=~_|!,.;]*)(:((6553[0-5])|(655[0-2][0-9])|(65[0-4][0-9]{2})|(6[0-4][0-9]{3})|([1-5][0-9]{4})|([0-5]{1,5})|([0-9]{1,4})))?"
    val pattern: Pattern = Pattern.compile(urlRegex)
    val matcher: Matcher = pattern.matcher(state.connectionUrl)

    var host = "localhost"
    var port = "10443"
    if (matcher.matches()) {
      if (matcher.group(2) != null)
        host = matcher.group(2)
      if (matcher.group(3) != null)
        port = matcher.group(3).substring(1)
    }

    val content = getResourceStream("files/${ZOWE_CONFIG_NAME}")
      .use { iS -> iS?.readAllBytes()?.let { String(it, charset) } }
      ?.replace("<PORT>".toRegex(), port)
      ?.replace("<HOST>".toRegex(), "\"$host\"")
      ?.replace("<SSL>".toRegex(), (!state.isAllowSsl).toString())
      ?.toByteArray(charset)
      ?: throw Exception("$ZOWE_CONFIG_NAME is not found")
    Files.write(Paths.get(jsonFileName), content)

    runWriteAction {
      val configCredentialsMap = mutableMapOf<String, Any?>()
      configCredentialsMap["profiles.base.properties.user"] = state.username
      configCredentialsMap["profiles.base.properties.password"] = state.password
      ZoweConfig.saveNewSecureProperties(jsonFileName, configCredentialsMap)
    }
  }

  override fun checkAndRemoveOldZoweConnection(type: ZoweConfigType) {
    val allConnections = configCrudable.getAll<ConnectionConfig>().toList()
    val allConnectionsNames: MutableList<String> = allConnections.map { it.name }.toMutableList()

    allConnections.filter { it.zoweConfigPath == getZoweConfigLocation(myProject, type) }.forEach {
      var index = 1
      var newName = it.name
      while (allConnectionsNames.contains(newName)) {
        newName = it.name.plus(index.toString())
        index++
      }
      allConnectionsNames.add(newName)
      it.name = newName
      it.zoweConfigPath = null
      configCrudable.update(it)
    }
  }

  private fun createZoweSchemaJsonIfNotExists() {
    val schemaFileName = "${myProject.basePath}/zowe.schema.json"
    val schemaFile = File(schemaFileName)
    if (!schemaFile.exists()) {
      FileOutputStream(schemaFile, false).use { fOS ->
        getResourceStream("files/zowe.schema.json").use { iS -> iS?.transferTo(fOS) }
      }
    }
  }

  /**
   * Converts ZoweConfig to ConnectionConfig.
   * @param uuid - uuid returned connection.
   * @return converted ConnectionConfig.
   */
  fun ZOSConnection.toConnectionConfig(
    uuid: String,
    zVersion: ZVersion = ZVersion.ZOS_2_1,
    owner: String = "",
    type: ZoweConfigType
  ): ConnectionConfig {
    val basePath = if (basePath.last() == '/') basePath.dropLast(1) else basePath
    val domain = "${host}:${zosmfPort}"
    val zoweUrl = "${protocol}://${domain}${basePath}"
    val isAllowSelfSigned = !(rejectUnauthorized ?: false)

    return ConnectionConfig(
      uuid,
      getZoweConnectionName(myProject, type, profileName),
      zoweUrl,
      isAllowSelfSigned,
      zVersion,
      getZoweConfigLocation(myProject, type),
      owner
    )
  }

  /**
   * @see ZoweConfigService.getZoweConfigState
   */
  override fun getZoweConfigState(scanProject: Boolean, type: ZoweConfigType): ZoweConfigState {
    if (scanProject) {
      try {
        scanForZoweConfig(type)
      } catch (e: Exception) {
        notifyError(e)
      }
    }
    val zoweConfig = if (type == ZoweConfigType.LOCAL)
      localZoweConfig ?: return ZoweConfigState.NOT_EXISTS
    else
      globalZoweConfig ?: return ZoweConfigState.NOT_EXISTS

    findAllZosmfExistingConnection(type) ?: return ZoweConfigState.NEED_TO_ADD
    var ret = ZoweConfigState.SYNCHRONIZED

    for (zosConnection in zoweConfig.getListOfZosmfConections()) {
      val existingConnection =
        findExistingConnection(type, zosConnection.profileName)
      if (existingConnection == null)
        ret = setZoweConfigState(ret, ZoweConfigState.NEED_TO_ADD)
      else {
        val newConnectionList = zoweConfig.getListOfZosmfConections()
          .filter { it.profileName == getProfileNameFromConnName(existingConnection.name) }
        val newConnection = if (newConnectionList.isNotEmpty()) {
          newConnectionList[0].toConnectionConfig(
            existingConnection.uuid.toString(), existingConnection.zVersion, existingConnection.owner, type = type
          )
        } else {
          ret = setZoweConfigState(ret, ZoweConfigState.NEED_TO_ADD)
          continue
        }
        val zoweUsername = zosConnection.user
        val zowePassword = zosConnection.password

        ret = if (existingConnection == newConnection &&
          getUsername(newConnection) == zoweUsername &&
          getPassword(newConnection) == zowePassword
        ) {
          setZoweConfigState(ret, ZoweConfigState.SYNCHRONIZED)
        } else {
          setZoweConfigState(ret, ZoweConfigState.NEED_TO_UPDATE)
        }
      }
    }
    return ret
  }

  /**
   * Returns the resulting ZoweConfigState depending on the previous and new ones
   * @param prev previous state
   * @param curr new state
   * @return resulting ZoweConfigState
   */
  private fun setZoweConfigState(prev: ZoweConfigState, curr: ZoweConfigState): ZoweConfigState {
    return if (prev == ZoweConfigState.ERROR || curr == ZoweConfigState.SYNCHRONIZED)
      prev
    else curr
  }

}
