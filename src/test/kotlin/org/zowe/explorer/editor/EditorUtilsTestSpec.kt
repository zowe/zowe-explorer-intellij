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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class EditorUtilsTestSpec : MockkAwareShouldSpec({
  context("editor/utils") {
    val documentMock = mockk<Document>()
    val projectMock = mockk<Project>()
    val editorMock = mockk<Editor> {
      every { document } returns documentMock
      every { project } returns projectMock
    }

    mockkStatic(FileDocumentManager::getInstance)
    mockkStatic(EditorModificationUtil::checkModificationAllowed)

    should("requestDocumentWriting. Check if current document is writable") {
      var isFileWritable = false
      every {
        FileDocumentManager.getInstance().requestWritingStatus(documentMock, projectMock)
      } answers {
        isFileWritable = true
        FileDocumentManager.WriteAccessStatus.WRITABLE
      }

      requestDocumentWriting(editorMock)

      assertSoftly { isFileWritable shouldBe true }
    }

    should("requestDocumentWriting. Check if current document is not writable") {
      var isFileWritable = true
      every {
        FileDocumentManager.getInstance().requestWritingStatus(documentMock, projectMock)
      } answers {
        FileDocumentManager.WriteAccessStatus.NON_WRITABLE
      }
      every { EditorModificationUtil.setReadOnlyHint(editorMock, any<String>()) } just Runs
      every {
        EditorModificationUtil.checkModificationAllowed(editorMock)
      } answers {
        isFileWritable = false
        false
      }

      requestDocumentWriting(editorMock)

      assertSoftly { isFileWritable shouldBe false }
    }
  }
})
