/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.zowe.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import org.zowe.explorer.utils.write
import org.zowe.explorer.zowe.service.ZoweConfigService
import org.zowe.explorer.zowe.service.ZoweConfigService.Companion.lock
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.explorer.zowe.service.ZoweConfigType
import org.zowe.kotlinsdk.zowe.config.parseConfigJson

/**
 * Synchronizes zowe config with crudable config if needed.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class UpdateZoweConfigAction : DumbAwareAction() {
  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val editor = e.getData(CommonDataKeys.EDITOR) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }

    var type = ZoweConfigType.GLOBAL
    val zoweLocalConfigLocation = ZoweConfigServiceImpl.getZoweConfigLocation(project, ZoweConfigType.LOCAL)
    if (e.getData(CommonDataKeys.VIRTUAL_FILE)?.path == zoweLocalConfigLocation)
      type = ZoweConfigType.LOCAL

    FileDocumentManager.getInstance().saveDocument(editor.document)

    val zoweConfigService = project.service<ZoweConfigService>()

    zoweConfigService.addOrUpdateZoweConfig(true, type = type)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val editor = e.getData(CommonDataKeys.EDITOR) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val vFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
    var type = ZoweConfigType.GLOBAL
    val zoweLocalConfigLocation = ZoweConfigServiceImpl.getZoweConfigLocation(project, ZoweConfigType.LOCAL)
    val zoweGlobalConfigLocation = ZoweConfigServiceImpl.getZoweConfigLocation(project, ZoweConfigType.GLOBAL)
    if (vFile?.path != zoweLocalConfigLocation && vFile?.path != zoweGlobalConfigLocation) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    if (vFile.path == zoweLocalConfigLocation)
      type = ZoweConfigType.LOCAL

    val zoweConfigService = project.service<ZoweConfigService>()
    lock.write {
      val prevZoweConfig = if (type == ZoweConfigType.LOCAL)
        zoweConfigService.localZoweConfig
      else
        zoweConfigService.globalZoweConfig
      if (type == ZoweConfigType.LOCAL) {
        zoweConfigService.localZoweConfig = parseConfigJson(editor.document.text)
        zoweConfigService.localZoweConfig?.extractSecureProperties(vFile.path.split("/").toTypedArray())
      } else {
        zoweConfigService.globalZoweConfig = parseConfigJson(editor.document.text)
        zoweConfigService.globalZoweConfig?.extractSecureProperties(vFile.path.split("/").toTypedArray())
      }
      val zoweState = zoweConfigService.getZoweConfigState(false, type = type)
      e.presentation.isEnabledAndVisible =
        zoweState == ZoweConfigState.NEED_TO_UPDATE || zoweState == ZoweConfigState.NEED_TO_ADD
      if (type == ZoweConfigType.LOCAL) {
        zoweConfigService.localZoweConfig = prevZoweConfig
      } else {
        zoweConfigService.globalZoweConfig = prevZoweConfig
      }
    }
  }
}
