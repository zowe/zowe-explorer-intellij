package org.zowe.explorer.explorer.actions.sort.members

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import org.zowe.explorer.explorer.ui.DSMaskNode
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class MembersSortActionGroupTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("members sort action group spec") {

    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // group action to spy
    val classUnderTest = spyk(MembersSortActionGroup())

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns explorerViewMock
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is FileExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenLibraryNode") {
      val nodeMock = mockk<LibraryNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly {
        checkNode shouldBe true
      }
    }

    should("shouldReturnNull_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns null
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorer shouldBe null
      }
    }

    should("shouldReturnFalse_whenCheckNode_givenWrongNode") {
      val nodeMock = mockk<DSMaskNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly {
        checkNode shouldBe false
      }
    }
  }

})
