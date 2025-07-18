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

package org.zowe.explorer.v3.actions.filter.ds

import com.intellij.openapi.actionSystem.AnActionEvent
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.explorer.ui.EXPLORER_VIEW
import org.zowe.explorer.explorer.ui.FileExplorerView
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.testutils.MockkAwareShouldSpec

class FilterChildrenHandlerTestSpec : MockkAwareShouldSpec({
  context("v3/actions/filter/ds/FilterChildrenHandler") {
    val eventMock = mockk<AnActionEvent>()

    beforeEach {
      every { eventMock.getData(EXPLORER_VIEW) } returns mockk<FileExplorerView> {
        every { mySelectedNodesData } returns listOf(
          mockk { every { node } returns mockk<LibraryNode>() }
        )
      }
    }

    context("getSingleSelectedNode") {
      should("get an exact single selected node from the Files explorer view") {
        val result = FilterChildrenHandler.getSingleSelectedNode(eventMock)
        assertSoftly { result shouldNotBe null }
      }

      should("get null as there is more than one node selected") {
        every { eventMock.getData(EXPLORER_VIEW) } returns mockk {
          every { mySelectedNodesData } returns listOf(
            mockk { every { node } returns mockk<LibraryNode>() },
            mockk { every { node } returns mockk<LibraryNode>() }
          )
        }

        val result = FilterChildrenHandler.getSingleSelectedNode(eventMock)
        assertSoftly { result shouldBe null }
      }

      should("get null as there is no selected nodes") {
        every { eventMock.getData(EXPLORER_VIEW) } returns mockk {
          every { mySelectedNodesData } returns listOf()
        }

        val result = FilterChildrenHandler.getSingleSelectedNode(eventMock)
        assertSoftly { result shouldBe null }
      }

      should("get null as the current view is not a Files explorer view") {
        every { eventMock.getData(EXPLORER_VIEW) } returns null

        val result = FilterChildrenHandler.getSingleSelectedNode(eventMock)
        assertSoftly { result shouldBe null }
      }
    }
  }
})
