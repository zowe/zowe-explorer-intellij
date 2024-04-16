package eu.ibagroup.formainframe.explorer.actions.sort.datasets

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import eu.ibagroup.formainframe.explorer.actions.sort.members.MembersSortActionGroup
import eu.ibagroup.formainframe.explorer.ui.DSMaskNode
import eu.ibagroup.formainframe.explorer.ui.FileExplorerView
import eu.ibagroup.formainframe.explorer.ui.FileLikeDatasetNode
import eu.ibagroup.formainframe.explorer.ui.LibraryNode
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*

class DatasetsSortActionGroupTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  context("datasets sort action group spec") {

    val actionEventMock = mockk<AnActionEvent>()
    val explorerViewMock = mockk<FileExplorerView>()
    // group action to spy
    val classUnderTest = spyk(DatasetsSortActionGroup())

    should("shouldReturnExplorerView_whenGetExplorerView_givenActionEvent") {
      every { actionEventMock.getData(any() as DataKey<FileExplorerView>) } returns explorerViewMock
      val actualExplorer = classUnderTest.getSourceView(actionEventMock)

      assertSoftly {
        actualExplorer shouldNotBe null
        actualExplorer is FileExplorerView
      }
    }

    should("shouldReturnTrue_whenCheckNode_givenDSMaskNode") {
      val nodeMock = mockk<DSMaskNode>()
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
      val nodeMock = mockk<FileLikeDatasetNode>()
      val checkNode = classUnderTest.checkNode(nodeMock)

      assertSoftly {
        checkNode shouldBe false
      }
    }
  }

})