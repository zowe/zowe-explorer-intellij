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

import com.intellij.openapi.project.ProjectManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.explorer.FileExplorer
import org.zowe.explorer.explorer.UIComponentManager
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.runInEdtAndWait

class FilterChildrenNodeTestSpec : AppInitShouldSpec("v3/actions/filter/ds/FilterChildrenNode", {
  context("all functions") {
    var didCallCleanCache = false
    var didChangeSavedFilter = false

    val oldFilter = "TESTOLD"
    val newFilter = "TESTNEW"
    val project = ProjectManager.getInstance().defaultProject

    val parentNodeMock = mockk<LibraryNode> {
      every {
        cleanCache(any(), any(), any(), any())
      } answers {
        didCallCleanCache = true
      }
    }

    mockkConstructor(FilterChildrenDialog::class)

    val uiComponentManager = UIComponentManager.getService()
    every { uiComponentManager.getExplorerContentProvider(any<Class<FileExplorer>>()) } returns mockk()

    val filterChildrenNode = FilterChildrenNode(project, parentNodeMock, mockk(), mockk(relaxUnitFun = true))

    beforeEach {
      didCallCleanCache = false
      didChangeSavedFilter = false

      every { parentNodeMock.savedFilter } returns oldFilter
      every { anyConstructed<FilterChildrenDialog>().waitForUserInput() } returns oldFilter

      filterChildrenNode.savedFilter = oldFilter
    }

    context("navigate") {
      should("change the filter value on the actual filter double click") {
        every { anyConstructed<FilterChildrenDialog>().waitForUserInput() } returns newFilter

        every {
          parentNodeMock.savedFilter = any()
        } answers {
          if (firstArg<String>() == newFilter) {
            didChangeSavedFilter = true
          }
        }

        runInEdtAndWait {
          filterChildrenNode.navigate(true)
        }

        assertSoftly {
          didCallCleanCache shouldBe true
          didChangeSavedFilter shouldBe true
        }
      }

      should("not change the filter value on the actual filter double click") {
        every {
          parentNodeMock.savedFilter = any()
        } answers {
          if (firstArg<String>() == newFilter) {
            didChangeSavedFilter = true
          }
        }

        runInEdtAndWait {
          filterChildrenNode.navigate(true)
        }

        assertSoftly {
          didCallCleanCache shouldBe false
          didChangeSavedFilter shouldBe false
        }
      }
    }
  }
})
