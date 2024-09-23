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

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import eu.ibagroup.formainframe.utils.`is`
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Action for adapting text to mainframe and displaying results right after user typed any char sequence.
 * @author Valiantsin Krus
 */
class MFTypedHandler : TypedHandlerDelegate() {

  /** Finds content adapter for mf files and performs its adapting. */
  override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {

    val vFile = file.virtualFile
    if (vFile != null && vFile.`is`<MFVirtualFile>()) {
      ChangeContentService.getService().processMfContent(editor)
    }
    return super.charTyped(c, project, editor, file)
  }
}
