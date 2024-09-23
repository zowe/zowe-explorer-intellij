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

package eu.ibagroup.formainframe.editor

import com.intellij.ide.actions.PasteAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyModificationException
import com.intellij.openapi.fileEditor.FileDocumentManager
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.content.adapters.MFContentAdapter
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.debounce
import eu.ibagroup.formainframe.utils.runWriteActionInEdt
import eu.ibagroup.formainframe.utils.subscribe
import eu.ibagroup.formainframe.vfs.MFVirtualFile

typealias EditorPasteAction = com.intellij.openapi.editor.actions.PasteAction

/**
 * Implementation of [ChangeContentService] to adapt mainframe content in editor.
 * @author Valiantsin Krus
 */
class ChangeContentServiceImpl : ChangeContentService {
  private val dataOpsManager = DataOpsManager.getService()
  private var adaptContentFunc: (() -> Unit)? = null
  private var subscribed = false

  /**
   * Subscribes on the topic [AnActionListener.TOPIC] to handle events for changing file content.
   */
  override fun initialize() {
    if (!subscribed) {

      subscribe(
        AnActionListener.TOPIC,
        object : AnActionListener {
          override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
            if (action is EditorPasteAction || (action is PasteAction && event.place == "EditorPopup")) {
              val editor = event.getData(CommonDataKeys.EDITOR) ?: return
              val isFileWritable = requestDocumentWriting(editor)
              if (isFileWritable) {
                processMfContent(editor)
              }
            }
          }

          override fun afterEditorTyping(c: Char, dataContext: DataContext) {
            val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
            val isFileWritable = requestDocumentWriting(editor)
            if (isFileWritable) {
              processMfContent(editor)
            }
          }
        }
      )
      subscribed = true
    }
  }

  /** Searches for [MFContentAdapter] in [DataOpsManager] and applies it to the file. */
  override fun processMfContent(editor: Editor) {
    val file = FileDocumentManager.getInstance().getFile(editor.document).castOrNull<MFVirtualFile>() ?: return
    if (adaptContentFunc == null) {
      adaptContentFunc = debounce(500) {
        adaptContentFunc = null
        val contentAdapter = dataOpsManager.getMFContentAdapter(file)
        val currentContent = editor.document.text
        val adaptedContent = contentAdapter.prepareContentToMainframe(currentContent, file)
        runWriteActionInEdt {
          CommandProcessor.getInstance().runUndoTransparentAction {
            try {
              editor.document.setText(adaptedContent)
            } catch (e: ReadOnlyModificationException) {
              return@runUndoTransparentAction
            }
          }
        }
      }
    }
    adaptContentFunc?.let { it() }
  }

}
