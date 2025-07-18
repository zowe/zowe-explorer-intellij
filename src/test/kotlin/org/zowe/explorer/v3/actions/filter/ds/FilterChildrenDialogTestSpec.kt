/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.zowe.explorer.explorer.ui.LibraryNode
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.setPrivateFieldValue
import org.zowe.explorer.utils.runInEdtAndWait

class FilterChildrenDialogTestSpec : AppInitShouldSpec("v3/actions/filter/ds/FilterChildrenDialog", {
  context("all functions") {
    val oldFilter = "TESTOLD"

    mockkConstructor(FilterChildrenDialog::class)

    val project = ProjectManager.getInstance().defaultProject
    val libraryNodeMock = mockk<LibraryNode>()

    beforeEach {
      every { anyConstructed<FilterChildrenDialog>().showAndGet() } returns true

      every { libraryNodeMock.savedFilter } returns oldFilter
    }

    context("waifForUserInput") {
      val newFilter = "TESTNEW"

      should("wait for user input changing a filter") {
        runInEdtAndWait {
          val filterChildrenDialog = FilterChildrenDialog(project, libraryNodeMock)
          setPrivateFieldValue(filterChildrenDialog, "newFilter", newFilter)
          val result = filterChildrenDialog.waitForUserInput()
          assertSoftly { result shouldBe newFilter }
        }
      }
      should("wait for user input not changing a filter") {
        every { anyConstructed<FilterChildrenDialog>().showAndGet() } returns false

        runInEdtAndWait {
          val filterChildrenDialog = FilterChildrenDialog(project, libraryNodeMock)
          setPrivateFieldValue(filterChildrenDialog, "newFilter", newFilter)
          val result = filterChildrenDialog.waitForUserInput()
          assertSoftly { result shouldBe oldFilter }
        }
      }
    }
  }
})
