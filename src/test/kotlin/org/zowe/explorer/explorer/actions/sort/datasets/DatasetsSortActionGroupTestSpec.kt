/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions.sort.datasets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.FileLikeDatasetNode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class DatasetsSortActionGroupTestSpec : MockkAwareShouldSpec({
  context("datasets sort action group spec") {
    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // group action to spy
    val classUnderTest = spyk(DatasetsSortActionGroup())

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(any<DataKey<FileExplorerView>>()) } returns explorerViewMock
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is FileExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenDSMaskNode") {
      val nodeMock = mockk<DSMaskNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe true }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns null
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly { actualExplorer shouldBe null }
    }

    should("shouldReturnFalse_whenCheckNode_givenWrongNode") {
      val nodeMock = mockk<FileLikeDatasetNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe false }
    }
  }
})
