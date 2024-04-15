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

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.explorer.zowe.service.ZoweConfigService
import org.zowe.explorer.zowe.service.ZoweConfigState
import org.zowe.kotlinsdk.zowe.config.parseConfigJson

/**
 * Synchronizes zowe config with crudable config if needed.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class UpdateZoweConfigAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val editor = e.getData(CommonDataKeys.EDITOR) ?: let {
      e.presentation.isEnabledAndVisible = false
      return
    }
    FileDocumentManager.getInstance().saveDocument(editor.document)

    val zoweConfigService = project.service<ZoweConfigService>()

    zoweConfigService.addOrUpdateZoweConfig(true)
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
    if (vFile?.path != "${project.basePath}/$ZOWE_CONFIG_NAME") {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val zoweConfigService = project.service<ZoweConfigService>()

    val prevZoweConfig = zoweConfigService.zoweConfig
    runCatching {
      zoweConfigService.zoweConfig = parseConfigJson(editor.document.text)
      zoweConfigService.zoweConfig?.extractSecureProperties(vFile.path.split("/").toTypedArray())
    }
    val zoweState = zoweConfigService.getZoweConfigState(false)
    e.presentation.isEnabledAndVisible =
      zoweState == ZoweConfigState.NEED_TO_UPDATE || zoweState == ZoweConfigState.NEED_TO_ADD
    zoweConfigService.zoweConfig = prevZoweConfig
  }
}
