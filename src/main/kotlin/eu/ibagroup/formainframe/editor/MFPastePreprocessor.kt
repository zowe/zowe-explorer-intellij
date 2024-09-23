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

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import eu.ibagroup.formainframe.utils.`is`
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable


/**
 * Preprocess to handle paste events for [MFVirtualFile].
 * @author Valiantsin Krus
 */
class MFPastePreprocessor : CopyPastePostProcessor<TextBlockTransferableData>() {

  /** List of [TextBlockTransferableData] that will be constantly returned in [extractTransferableData]. */
  private val DATA_LIST = mutableListOf<TextBlockTransferableData>(DumbData())

  /**
   * Implementation of [TextBlockTransferableData] that doesn't carry a functional load.
   * It is only needed to trigger [processTransferableData].
   */
  internal class DumbData : TextBlockTransferableData {
    override fun getFlavor(): DataFlavor {
      return DATA_FLAVOR
    }

    companion object {
      private val DATA_FLAVOR = DataFlavor(MFPastePreprocessor::class.java, "class: ChangeContentByPasteAction")
    }
  }

  /** This method is triggered after copy action. That's why this method always return empty list. */
  override fun collectTransferableData(
    file: PsiFile,
    editor: Editor,
    startOffsets: IntArray,
    endOffsets: IntArray
  ): MutableList<TextBlockTransferableData> {
    return mutableListOf()
  }

  /**
   * This method is triggered after paste action.
   * [processTransferableData] would be triggered if this method returned not empty list of transferable data.
   */
  override fun extractTransferableData(content: Transferable): MutableList<TextBlockTransferableData> {
    return DATA_LIST
  }

  /** Adapts content after paste action for [MFVirtualFile] using [ChangeContentService]. */
  override fun processTransferableData(
    project: Project,
    editor: Editor,
    bounds: RangeMarker,
    caretOffset: Int,
    indented: Ref<in Boolean>,
    values: MutableList<out TextBlockTransferableData>
  ) {
    val vFile = FileDocumentManager.getInstance().getFile(editor.document)
    if (vFile != null && vFile.`is`<MFVirtualFile>()) {
      ChangeContentService.getService().processMfContent(editor)
    }
    super.processTransferableData(project, editor, bounds, caretOffset, indented, values)
  }
}
