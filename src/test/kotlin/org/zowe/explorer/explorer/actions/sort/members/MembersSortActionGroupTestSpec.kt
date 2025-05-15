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

package org.zowe.explorer.explorer.actions.sort.members

import com.intellij.openapi.actionSystem.AnActionEvent
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class MembersSortActionGroupTestSpec : MockkAwareShouldSpec({
  context("members sort action group spec") {
    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // group action to spy
    val classUnderTest = spyk(MembersSortActionGroup())

    beforeEach {
      every { actionEventMock.getData(EXPLORER_VIEW) } returns explorerViewMock
    }

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is FileExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenLibraryNode") {
      val nodeMock = mockk<LibraryNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe true }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(EXPLORER_VIEW) } returns null

      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly { actualExplorer shouldBe null }
    }

    should("shouldReturnFalse_whenCheckNode_givenWrongNode") {
      val nodeMock = mockk<DSMaskNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly { checkNode shouldBe false }
    }
  }
})
