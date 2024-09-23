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

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class EditorUtilsTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("Utils common test spec") {

    val editorMock = mockk<Editor>()
    val documentMock = mockk<Document>()
    val projectMock = mockk<Project>()
    every { editorMock.document } returns documentMock
    every { editorMock.project } returns projectMock
    mockkStatic(FileDocumentManager::getInstance)
    mockkStatic(EditorModificationUtil::checkModificationAllowed)

    should("requestDocumentWriting. Check if current document is writable") {
      var isFileWritable = false
      every { FileDocumentManager.getInstance().requestWritingStatus(documentMock, projectMock) } answers {
        isFileWritable = true
        FileDocumentManager.WriteAccessStatus.WRITABLE
      }

      requestDocumentWriting(editorMock)

      assertSoftly {
        isFileWritable shouldBe true
      }

    }

    should("requestDocumentWriting. Check if current document is not writable") {
      var isFileWritable = true
      every { FileDocumentManager.getInstance().requestWritingStatus(documentMock, projectMock) } answers {
        FileDocumentManager.WriteAccessStatus.NON_WRITABLE
      }
      every { EditorModificationUtil.setReadOnlyHint(editorMock, any() as String) } just Runs
      every { EditorModificationUtil.checkModificationAllowed(editorMock) } answers {
        isFileWritable = false
        false
      }

      requestDocumentWriting(editorMock)

      assertSoftly {
        isFileWritable shouldBe false
      }

    }

    unmockkAll()
  }
})
