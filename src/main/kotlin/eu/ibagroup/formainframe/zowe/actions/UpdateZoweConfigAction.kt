/*
 * This is a property of IBA Group
 */
package eu.ibagroup.formainframe.zowe.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import eu.ibagroup.formainframe.zowe.ZOWE_CONFIG_NAME
import eu.ibagroup.formainframe.zowe.service.ZoweConfigService
import eu.ibagroup.formainframe.zowe.service.ZoweConfigState
import eu.ibagroup.r2z.zowe.config.parseConfigJson

/**
 * Synchronizes zowe config with crudable config if needed.
 * @author Valiantsin Krus
 * @version 0.5
 * @since 2021-02-12
 */
class UpdateZoweConfigAction: DumbAwareAction() {

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
    e.presentation.isEnabledAndVisible = zoweState == ZoweConfigState.NEED_TO_UPDATE || zoweState == ZoweConfigState.NEED_TO_ADD
    zoweConfigService.zoweConfig = prevZoweConfig
  }
}
