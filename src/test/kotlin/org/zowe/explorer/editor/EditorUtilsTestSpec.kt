/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class EditorUtilsTestSpec : ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }
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
