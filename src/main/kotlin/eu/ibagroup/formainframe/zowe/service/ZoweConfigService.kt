/*
 * This is a property of IBA Group
 */
package eu.ibagroup.formainframe.zowe.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.r2z.zowe.config.ZoweConfig

/**
 * ZoweConfigService implements ability to interact
 * with zowe config file stored inside project root.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
interface ZoweConfigService {
  /**
   * Project instance.
   */
  val myProject: Project

  /**
   * Instance of zowe config file object model
   */
  var zoweConfig: ZoweConfig?

  /**
   * Compares zoweConfig data with related connection config.
   * @param scanProject - will rescan project for zowe.config.json if true.
   * @return - NEED_TO_UPDATE if connection config related to zowe.config.json exists but data is differ.
   *           NEED_TO_ADD if zowe.config.json is presented but there no connection config related to it.
   *           SYNCHRONIZED if zowe.config.json and connection config are presented and their data are the same.
   *           NOT_EXISTS if zowe.config.json file is not presented in project.
   */
  fun getZoweConfigState (scanProject: Boolean = true): ZoweConfigState

  /**
   * Adds or updates connection config related to zoweConnection
   * @param scanProject - will rescan project for zowe.config.json if true.
   * @param checkConnection - Verify zowe connection by sending info request if true.
   * @return - ConnectionConfig that was added or updated
   */
  fun addOrUpdateZoweConfig (scanProject: Boolean = true, checkConnection: Boolean = true): ConnectionConfig?

  companion object {
    fun getInstance(project: Project): ZoweConfigService = project.getService(ZoweConfigService::class.java)
  }
}

enum class ZoweConfigState(val value: String) {
  NEED_TO_UPDATE("Update"),
  NEED_TO_ADD("Add"),
  SYNCHRONIZED("Synchronized"),
  NOT_EXISTS("NotExists"),
  ERROR("Error");

  override fun toString(): String {
    return value
  }
}
