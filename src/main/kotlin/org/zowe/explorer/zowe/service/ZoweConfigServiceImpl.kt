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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.zowe.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ZOSMFConnectionConfigurable.Companion.warningMessageForDeleteConfig
import org.zowe.explorer.config.connect.whoAmI
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.explorer.EXPLORER_NOTIFICATION_GROUP_ID
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.utils.*
import org.zowe.explorer.utils.crudable.find
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigService.Companion.lock
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.exceptions.EmptyZoweConfigFileException
import org.zowe.kotlinsdk.zowe.client.sdk.core.ZOSConnection
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.io.File
import java.io.FileOutputStream
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
    private fun produceRareErrorNotification() {
      NotificationsService.errorNotification(
        NotificationCompatibleException(
          "Error during Zowe Config handling",
          "Project must not be null during Zowe Config local file recognition",
          "This is likely a rarely occurring situation, please, contact Zowe Support in case you encountered it"
        )
      )
    }

    /**
     * Returns base Zowe connection name
     */
    private fun getBaseZoweConnectionName(
      myProject: Project?,
      type: ZoweConfigType,
      profileName: String = "zosmf"
    ): String {
      return if (type == ZoweConfigType.LOCAL) {
        if (myProject == null) {
          produceRareErrorNotification()
          ""
        } else {
          "${ZOWE_PROJECT_PREFIX}${type}-${profileName}/${myProject.name}"
        }
      } else {
        "${ZOWE_PROJECT_PREFIX}${type}-${profileName}"
      }
    }

    /**
     * Returns Zowe connection name
     */
    fun getZoweConnectionName(myProject: Project?, type: ZoweConfigType, profileName: String = "zosmf"): String {
      val configCrudable = ConfigService.getService().crudable
      val allConnections = configCrudable.getAll<ConnectionConfig>().toList()
      val allConnectionsNames: MutableList<String> = allConnections.map { it.name }.toMutableList()
      val connectionNameBase = getBaseZoweConnectionName(myProject, type, profileName)
      var connectionName = connectionNameBase

      var index = 1
      while (allConnectionsNames.contains(connectionName)) {
        connectionName = connectionNameBase.plus(index.toString())
        index++
      }
      return connectionName
    }

    /**
     * Returns path to Zowe configuration file
     */
    fun getZoweConfigLocation(myProject: Project?, type: ZoweConfigType): String {
      return if (type == ZoweConfigType.LOCAL) {
        if (myProject == null) {
          produceRareErrorNotification()
          ""
        } else {
          "${myProject.basePath}/$ZOWE_CONFIG_NAME"
        }
      } else {
        System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
      }
    }

    /**
     * Returns profile name for current connection
     */
    fun getProfileNameFromConnName(connName: String): String {
      var profileName = connName
        .replace(
          Regex("^$ZOWE_PROJECT_PREFIX(${ZoweConfigType.LOCAL}|${ZoweConfigType.GLOBAL})+(-)"),
          ""
        )
      profileName = profileName.replace(Regex("/.*$"), "")
      return profileName
    }

  }

  private val configCrudable = ConfigService.getService().crudable

  override var localZoweConfig: ZoweConfig? = null

  override var globalZoweConfig: ZoweConfig? = null

  /**
   * Checks project contains zowe.config.json. If zowe config presented
   * it will parse it and save to object model inside zoweConfig field.
   * @return ZoweConfig instance if zowe.config.json is presented or null otherwise.
   */
  private fun scanForZoweConfig(type: ZoweConfigType): ZoweConfig? {
    return runTask(title = "Scanning for ${type} zowe config file", project = myProject) {
      val zoweFile = runReadAction {
        VirtualFileManager.getInstance().findFileByNioPath(Path.of(getZoweConfigLocation(myProject, type)))
      } ?: return@runTask null
      return@runTask try {
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
        //parseConfigJson returns EmptyZoweConfigFileException in case of empty file
        if (e !is EmptyZoweConfigFileException)
          NotificationsService.errorNotification(e, project = myProject, custTitle = "Error with Zowe config file")
        return@runTask null
      }
    }
  }

  /**
   * @see ZoweConfigService.findAllZosmfExistingConnection
   */
  override fun findAllZosmfExistingConnection(type: ZoweConfigType): List<ConnectionConfig> {
    return configCrudable
      .find<ConnectionConfig> {
        val pattern =
          if (type == ZoweConfigType.LOCAL) {
            Regex("^(" + ZOWE_PROJECT_PREFIX + type + "-).*(/" + myProject.name + ")\\d*$")
          } else {
            Regex("^($ZOWE_PROJECT_PREFIX$type-).*")
          }
        it.name.matches(pattern) && it.zoweConfigPath == getZoweConfigLocation(myProject, type)
      }
      .collect(Collectors.toList())
  }

  /**
   * Finds existing connection config related to zowe.config.json.
   * @return ConnectionConfig instance related to zowe config if it exists or null otherwise.
   */
  private fun findExistingConnection(type: ZoweConfigType, profileName: String): ConnectionConfig? {
    return configCrudable
      .find<ConnectionConfig> {
        it.name.startsWith(
          getBaseZoweConnectionName(myProject, type, profileName)
        )
          && it.zoweConfigPath == getZoweConfigLocation(myProject, type)
      }
      .collect(Collectors.toList())
      .firstOrNull()
  }

  /**
   * Added notification about connection failure with action of force connection adding.
   * @param title notification title.
   * @param content notification content.
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
   * Checks all passed zowe connections using InfoOperation in parallel. It is also needed to load zos version and
   * real owner of the connection (acceptable for alias users).
   * IMPORTANT!!! It modifies passed connection objects by setting zVersion and owner.
   * @param zoweConnections list of connection to check and prepare.
   * @param type of zowe config
   * @throws ProcessCanceledException if connection has been canceled by user
   * @return a pair of lists with successfully tested connections and invalid connections
   */
  @Throws(ProcessCanceledException::class)
  private fun testAndPrepareConnections(
    zoweConnections: List<ZOSConnection>,
    type: ZoweConfigType
  ): Pair<MutableList<ConnectionConfig>, MutableList<ConnectionConfig>> {
    val succeededConnections = mutableListOf<ConnectionConfig>()
    val failedConnections = mutableListOf<ConnectionConfig>()
    val results = mutableListOf<Deferred<ConnectionConfig>>()
    var throwable: ProcessCanceledException? = null
    runTask("Testing Connections...", myProject, cancellable = true) { indicator ->
      runBlocking {
        for (zosmfConnection in zoweConnections) {
          results.add(
            async {
              val zoweConnection: ConnectionConfig = prepareConnection(zosmfConnection, type)
              try {
                runCatching {
                  if (!indicator.isCanceled) {
                    indicator.text = if (zoweConnections.size == 1)
                      "Testing Connection to ${zoweConnection.url}"
                    else
                      "Testing Connection to ${zoweConnections.size} connections"
                    DataOpsManager.getService().performOperation(InfoOperation(zoweConnection), indicator)
                  } else {
                    throw ProcessCanceledException()
                  }
                }.onSuccess {
                  if (!indicator.isCanceled) {

                    indicator.text = if (zoweConnections.size == 1)
                      "Retrieving z/OS information for ${zoweConnection.url}"
                    else
                      "Retrieving z/OS information for ${zoweConnections.size} connections"
                    val systemInfo =
                      DataOpsManager.getService().performOperation(
                        ZOSInfoOperation(zoweConnection),
                        indicator
                      )
                    zoweConnection.zVersion = when (systemInfo.zosVersion) {
                      "04.25.00" -> ZVersion.ZOS_2_2
                      "04.26.00" -> ZVersion.ZOS_2_3
                      "04.27.00" -> ZVersion.ZOS_2_4
                      "04.28.00" -> ZVersion.ZOS_2_5
                      "04.29.00" -> ZVersion.ZOS_3_1
                      else -> ZVersion.ZOS_2_1
                    }
                  } else {
                    throw ProcessCanceledException()
                  }
                }.onSuccess {
                  if (!indicator.isCanceled) {
                    indicator.text = if (zoweConnections.size == 1)
                      "Retrieving user information for ${zoweConnection.url}"
                    else
                      "Retrieving user information for ${zoweConnections.size} connections"
                    zoweConnection.owner = whoAmI(zoweConnection) ?: ""
                  } else {
                    throw ProcessCanceledException()
                  }
                }.onFailure {
                  if (indicator.isCanceled)
                    throw ProcessCanceledException()
                  else {
                    throw it
                  }
                }
              } catch (t: Throwable) {
                if (t is ProcessCanceledException)
                  throwable = t
                failedConnections.add(zoweConnection)
                return@async zoweConnection
              }
              succeededConnections.add(zoweConnection)
              return@async zoweConnection
            }
          )
        }
      }
    }
    if (throwable != null) {
      throw ProcessCanceledException()
    }
    runBlocking {
      results.map { it.await() }
    }
    return succeededConnections to failedConnections
  }

  /**
   * @see ZoweConfigService.addOrUpdateZoweConfig
   */
  override fun addOrUpdateZoweConfig(scanProject: Boolean, checkConnection: Boolean, type: ZoweConfigType) {
    try {
      val zoweConfig = if (scanProject) {
        scanForZoweConfig(type)
      } else if (type == ZoweConfigType.LOCAL)
        this.localZoweConfig
      else
        this.globalZoweConfig
      zoweConfig ?: throw Exception("Cannot get $type Zowe config")

      val allConnectionsToTest = zoweConfig.getListOfZosmfConections()
      val uniqueConnectionsToTest = allConnectionsToTest.filterIndexed { _, element ->
        allConnectionsToTest.filter {
          it.host == element.host &&
            it.zosmfPort == element.zosmfPort &&
            it.user == element.user &&
            it.password == element.password &&
            it.protocol == element.protocol &&
            it.rejectUnauthorized == element.rejectUnauthorized &&
            it.basePath == element.basePath &&
            it.encoding == element.encoding &&
            it.responseTimeout == element.responseTimeout
        }.sortedWith(compareBy { it.profileName.length })[0] == element
      }
      val duplicatedConnections = (allConnectionsToTest + uniqueConnectionsToTest).groupBy { it.profileName }
        .filter { it.value.size == 1 }
        .flatMap { it.value }

      val (succeededConnections, failedConnections) =
        testAndPrepareConnections(uniqueConnectionsToTest, type)

      for (tmpZOSMFConn in duplicatedConnections) {
        restoreFullConnectionList(tmpZOSMFConn, succeededConnections, type)
        restoreFullConnectionList(tmpZOSMFConn, failedConnections, type)
      }

      if (checkConnection and failedConnections.isNotEmpty()) {
        val andMore = if (failedConnections.size > 3) "..." else ""
        notifyUiOnConnectionFailure(
          "Unsuccessfully tested profiles:",
          "${failedConnections.joinToString(separator = ", <p>") { getProfileNameFromConnName(it.name) }} $andMore",
          type
        )
      }
      val conToAdd = if (checkConnection)
        succeededConnections
      else
        succeededConnections + failedConnections
      conToAdd.forEach { zosmfConnection ->
        val connectionOpt = configCrudable.addOrUpdate(zosmfConnection)
        if (!connectionOpt.isEmpty) {
          val topic = if (type == ZoweConfigType.LOCAL)
            LOCAL_ZOWE_CONFIG_CHANGED
          else
            GLOBAL_ZOWE_CONFIG_CHANGED
          sendTopic(topic).onConfigSaved(zoweConfig, zosmfConnection)
        }
      }

    } catch (e: Exception) {
      if (e !is ProcessCanceledException)
        NotificationsService.errorNotification(
          e,
          project = myProject,
          custTitle = "Error with Zowe config file"
        )
      else { /* Nothing to do */
      }
    }
  }

  /**
   * Restore connection lists after testing
   * @param tmpZOSMFConn duplicated connection to restore.
   * @param connListToRestore list with tested connections (succeeded or failed)
   * @param type of zowe config
   */
  private fun restoreFullConnectionList(
    tmpZOSMFConn: ZOSConnection,
    connListToRestore: MutableList<ConnectionConfig>,
    type: ZoweConfigType
  ) {
    val tmpConn = tmpZOSMFConn.toConnectionConfig(uuid = UUID.randomUUID().toString(), type = type)
    val existingConn = findExistingConnection(type, tmpZOSMFConn.profileName)
    if (existingConn?.name != null) {
      tmpConn.name = existingConn.name
      tmpConn.uuid = existingConn.uuid
    }
    val tmpDuplicatedConnList = connListToRestore.filter {
      it.isAllowSelfSigned == tmpConn.isAllowSelfSigned &&
        it.url == tmpConn.url &&
        it.name != tmpConn.name &&
        CredentialService.getUsername(it) == tmpZOSMFConn.user &&
        CredentialService.getPassword(it).contentEquals(tmpZOSMFConn.password.toCharArray())
    }
    if (tmpDuplicatedConnList.isNotEmpty()) {
      val tmpDuplConn = tmpDuplicatedConnList[0].clone()
      tmpDuplConn.name = tmpConn.name
      tmpDuplConn.uuid = tmpConn.uuid
      CredentialService.getService()
        .setCredentials(tmpConn.uuid, tmpZOSMFConn.user, tmpZOSMFConn.password.toCharArray())
      connListToRestore.add(tmpDuplConn)
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
    val zoweConnection = findExistingConnection(type, zosmfConnection.profileName)
      ?.let { oldConn ->
        zosmfConnection.toConnectionConfig(oldConn.uuid, oldConn.zVersion, type = type)
          .also { newConn -> newConn.name = oldConn.name }
      }
      ?: zosmfConnection.toConnectionConfig(UUID.randomUUID().toString(), type = type)
    CredentialService.getService().setCredentials(zoweConnection.uuid, username, password.toCharArray())
    return zoweConnection
  }

  /**
   * @see ZoweConfigService.deleteZoweConfig
   */
  override fun deleteZoweConfig(type: ZoweConfigType) {
    try {

      val zoweConnections = findAllZosmfExistingConnection(type)
      if (zoweConnections.isEmpty()) throw Exception("Cannot find any z/OSMF connections")

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
      NotificationsService.errorNotification(e, project = myProject, custTitle = "Error with Zowe config file")
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
      if (!matcher.group(2).isNullOrBlank())
        host = matcher.group(2)
      if (!matcher.group(3).isNullOrBlank())
        port = matcher.group(3).substring(1)
    }

    val content = getResourceAsStreamWrappable(ZoweConfigServiceImpl::class.java.classLoader, "files/${ZOWE_CONFIG_NAME}")
      .use { iS -> iS?.readAllBytes()?.let { String(it, charset) } }
      ?.replace("<PORT>".toRegex(), port)
      ?.replace("<HOST>".toRegex(), "\"$host\"")
      ?.replace("<SSL>".toRegex(), (!state.isAllowSsl).toString())
      ?.toByteArray(charset)
      ?: throw Exception("$ZOWE_CONFIG_NAME is not found")
    saveChangedZoweConfig(jsonFileName, content)

    runWriteAction {
      val configCredentialsMap = mutableMapOf<String, Any?>()
      configCredentialsMap["profiles.base.properties.user"] = state.username
      configCredentialsMap["profiles.base.properties.password"] = String(state.password)
      ZoweConfig.saveNewSecureProperties(jsonFileName, configCredentialsMap)
    }
  }

  override fun checkAndRemoveOldZoweConnection(type: ZoweConfigType) {
    val allConnections = configCrudable.getAll<ConnectionConfig>().toList()
    val allConnectionsNames: MutableList<String> = allConnections.map { it.name }.toMutableList()

    allConnections
      .filter { it.zoweConfigPath == getZoweConfigLocation(myProject, type) }
      .forEach {
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
        getResourceAsStreamWrappable(ZoweConfigServiceImpl::class.java.classLoader,"files/zowe.schema.json").use { iS -> iS?.transferTo(fOS) }
      }
    }
  }

  /** Write changes of Zowe config by the [jsonFileName] with the changed [content] */
  private fun saveChangedZoweConfig(jsonFileName: String, content: ByteArray) {
    Files.write(Paths.get(jsonFileName), content)
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
      scanForZoweConfig(type)
    }
    val zoweConfig = if (type == ZoweConfigType.LOCAL)
      localZoweConfig ?: return ZoweConfigState.NOT_EXISTS
    else
      globalZoweConfig ?: return ZoweConfigState.NOT_EXISTS

    if (findAllZosmfExistingConnection(type).isEmpty()) return ZoweConfigState.NEED_TO_ADD

    val zoweConfigZosmfConnections = zoweConfig.getListOfZosmfConections()

    return zoweConfigZosmfConnections
      .fold(ZoweConfigState.SYNCHRONIZED) { prevZoweConfigState, zosConnection ->
        val existingConnection = findExistingConnection(type, zosConnection.profileName)
        val currZoweConfigState = if (existingConnection == null) {
          ZoweConfigState.NEED_TO_ADD
        } else {
          val updatedConnection = zosConnection.toConnectionConfig(
            existingConnection.uuid, existingConnection.zVersion, existingConnection.owner, type = type
          )
          updatedConnection.name = existingConnection.name
          val zoweUsername = zosConnection.user
          val zowePassword = zosConnection.password
          if (
            existingConnection == updatedConnection
            && CredentialService.getUsername(updatedConnection) == zoweUsername
            && CredentialService.getPassword(updatedConnection).contentEquals(zowePassword.toCharArray())
          ) {
            ZoweConfigState.SYNCHRONIZED
          } else {
            ZoweConfigState.NEED_TO_UPDATE
          }
        }
        if (prevZoweConfigState == ZoweConfigState.ERROR || currZoweConfigState == ZoweConfigState.SYNCHRONIZED)
          prevZoweConfigState
        else currZoweConfigState
      }
  }
}
