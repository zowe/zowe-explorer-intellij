/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.actions

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.utils.`is`
import org.zowe.explorer.utils.debounce
import org.zowe.explorer.utils.runWriteActionInEdt
import org.zowe.explorer.vfs.MFVirtualFile

class ChangeContentAction: TypedHandlerDelegate() {
  private val dataOpsManager = service<DataOpsManager>()
  private var adaptContentFunc: (() -> Unit)? = null

  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {

    val vFile = file.virtualFile
    if (vFile.`is`<MFVirtualFile>()) {
      if (adaptContentFunc == null) {
        adaptContentFunc = debounce(500) {
          adaptContentFunc = null
          val contentAdapter = dataOpsManager.getMFContentAdapter(file.virtualFile)
          val currentContent = editor.document.text.toByteArray(vFile.charset)
          val adaptedContent = contentAdapter.prepareContentToMainframe(currentContent, vFile)
          runWriteActionInEdt { editor.document.setText(adaptedContent.toString(vFile.charset)) }
        }
      }
      adaptContentFunc?.let { it() }
    }
    return super.charTyped(c, project, editor, file)
  }
}
