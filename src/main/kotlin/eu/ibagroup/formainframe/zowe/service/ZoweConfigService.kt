package eu.ibagroup.formainframe.zowe.service

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.r2z.zowe.config.ZoweConfig

interface ZoweConfigService {
  val myProject: Project

  fun getZoweConfig(): ZoweConfig?
  fun scanForZoweConfig (): ZoweConfig?
  fun findExistingConnection (): ConnectionConfig?
  fun getZoweConfigState (scanProject: Boolean = true): ZoweConfigState
  fun addOrUpdateZoweConfig (scanProject: Boolean = true): ConnectionConfig?

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
