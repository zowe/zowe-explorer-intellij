/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.editor

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.adapters.MFContentAdapter
import eu.ibagroup.formainframe.utils.debounce
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Implementation of [ChangeContentService] to adapt mainframe content in editor.
 * @author Valiantsin Krus
 */
class ChangeContentServiceImpl : ChangeContentService {
  private val dataOpsManager = service<DataOpsManager>()
  private var adaptContentFunc: (() -> Unit)? = null

  /** Searches for [MFContentAdapter] in [DataOpsManager] and applies it to the file. */
  override fun processMfContent(editor: Editor, file: MFVirtualFile) {
    if (adaptContentFunc == null) {
      adaptContentFunc = debounce(500) {
        adaptContentFunc = null
        val contentAdapter = dataOpsManager.getMFContentAdapter(file)
        val currentContent = editor.document.text.toByteArray(file.charset)
        val adaptedContent = contentAdapter.prepareContentToMainframe(currentContent, file)
        runWriteActionInEdt {
          CommandProcessor.getInstance().runUndoTransparentAction {
            editor.document.setText(adaptedContent.toString(file.charset))
          }
        }
      }
    }
    adaptContentFunc?.let { it() }
  }

}