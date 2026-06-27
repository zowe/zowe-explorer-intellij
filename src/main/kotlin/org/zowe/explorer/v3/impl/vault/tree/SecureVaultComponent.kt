/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.vault.tree

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import org.zowe.explorer.utils.subscribe
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigChangeListener
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
import org.zowe.explorer.v3.tree.ExplorerTreeComponent

/**
 * Secure Vault component for viewing credentials stored in the Zowe Team Config.
 * Displays profiles that have non-empty `secure` arrays and their credential entries.
 * Automatically syncs with config file edits and programmatic writes
 */
class SecureVaultComponent(private val project: Project) : ExplorerTreeComponent() {
  companion object {
    const val SECURE_VAULT_COMPONENT_NAME = "Secure Vault"
  }

  override val explorerName = SECURE_VAULT_COMPONENT_NAME
  override val explorerTreeStructure = SecureVaultTreeStructure(project)
  override val explorerTreeView = SecureVaultTreeView(explorerName, explorerAsyncTreeModel)

  private var docListenerDisposable: Disposable = Disposer.newDisposable(this, "docListener")

  init {
    explorerTreeStructure.addProfilesFromConfig()

    subscribe(
      ZoweConfigService.CONFIG_CHANGED_TOPIC,
      ZoweConfigChangeListener { syncProfiles() },
      this
    )

    attachDocumentListener()
  }

  /**
   * Attaches a [DocumentListener] to the active config file's document.
   * Disposes the previous listener before re-attaching so the tree tracks
   * the correct config file after a config type change
   */
  private fun attachDocumentListener() {
    Disposer.dispose(docListenerDisposable)
    docListenerDisposable = Disposer.newDisposable(this, "docListener")
    val configService = ZoweConfigService.getService()
    val configType = configService.getSelectedConfigType(project)
    val configFile = configService.resolveConfigFile(configType, project.basePath)
    val vf = LocalFileSystem.getInstance().refreshAndFindFileByPath(configFile.absolutePath) ?: return
    val document = FileDocumentManager.getInstance().getDocument(vf) ?: return
    document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        syncProfiles()
      }
    }, docListenerDisposable)
  }

  private fun syncProfiles() {
    explorerTreeStructure.syncProfilesWithConfig()
    invalidateNode(explorerTreeStructure.rootElement)
    attachDocumentListener()
  }
}
