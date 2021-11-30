package eu.ibagroup.formainframe.zowe.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.password
import eu.ibagroup.formainframe.config.connect.username
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.zowe.ZOWE_CONFIG_NAME
import eu.ibagroup.r2z.CodePage
import eu.ibagroup.r2z.annotations.ZVersion
import eu.ibagroup.r2z.zowe.config.ZoweConfig
import eu.ibagroup.r2z.zowe.config.parseConfigJson
import java.nio.file.Path
import java.util.*
import kotlin.streams.toList

class ZoweConfigServiceImpl(override val myProject: Project) : ZoweConfigService {

  private val configCrudable = ConfigService.instance.crudable
  private var zoweConfig: ZoweConfig? = null
  private val zoweConnectionName: String
    get() = "zowe-${myProject.name}"

  override fun getZoweConfig(): ZoweConfig? {
    return zoweConfig
  }

  override fun scanForZoweConfig (): ZoweConfig? {
    val zoweConfigLocation = "${myProject.basePath}/$ZOWE_CONFIG_NAME"
    val zoweFile = VirtualFileManager.getInstance().findFileByNioPath(Path.of(zoweConfigLocation)) ?: return null
    return try {
      parseConfigJson(zoweFile.inputStream).also { zoweConfig = it }
    } catch (e: Exception) {
      null
    }
  }

  override fun findExistingConnection(): ConnectionConfig? {
    val zoweConnectionList = configCrudable.find<ConnectionConfig> {
      it.isZoweConnection && it.name == zoweConnectionName
    }.toList()
    return if (zoweConnectionList.isEmpty()) null else zoweConnectionList[0]
  }

  fun getOrCreateUuid(): String {
    return findExistingConnection()?.uuid ?: UUID.randomUUID().toString()
  }


  override fun addOrUpdateZoweConfig(scanProject: Boolean): ConnectionConfig? {
    val zoweConfig = if (scanProject) {
      scanForZoweConfig()
    } else this.zoweConfig
    zoweConfig ?: return null
    val username = zoweConfig.user ?: return null
    val password = zoweConfig.password ?: return null
    val connectionOpt = configCrudable.addOrUpdate(
      zoweConfig.toConnectionConfig(getOrCreateUuid(), "zowe-${myProject.name}")
    )
    return if (connectionOpt.isEmpty) null else connectionOpt.get().also {
      CredentialService.instance.setCredentials(it.uuid, username, password)
    }
  }

  override fun getZoweConfigState(scanProject: Boolean): ZoweConfigState {
    if (scanProject) {
      scanForZoweConfig()
    }
    val existingConnection = findExistingConnection() ?: return ZoweConfigState.NEED_TO_ADD
    val zoweConfig = zoweConfig ?: return ZoweConfigState.NOT_EXISTS
    val newConnection = zoweConfig.toConnectionConfig(existingConnection.uuid, zoweConnectionName)

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


fun ZoweConfig.toConnectionConfig(uuid: String, connectionName: String): ConnectionConfig {
  val zoweUrl = "${protocol}://${host}:${port}${basePath}"
  val isAllowSelfSigned = protocol == "https"
  val tsoProfile = tsoProfile
  var codePage = CodePage.IBM_1047
  if (tsoProfile != null) {
    runCatching {
      codePage = CodePage.valueOf("IBM_${tsoProfile.properties["codePage"]}")
    }
  }

  return ConnectionConfig(
    uuid,
    connectionName,
    zoweUrl,
    isAllowSelfSigned,
    codePage,
    ZVersion.ZOS_2_1
  ).apply { isZoweConnection = true }
}
