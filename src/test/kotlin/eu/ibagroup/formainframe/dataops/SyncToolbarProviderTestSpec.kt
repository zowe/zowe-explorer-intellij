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

package eu.ibagroup.formainframe.dataops

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.dataops.content.synchronizer.SyncToolbarProvider
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll

class SyncToolbarProviderTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("sync action toolbar") {

    val mockedProject = mockk<Project>()
    val mockedActionManagerInstance = mockk<ActionManager>()
    val syncToolbarProviderForTest = spyk(SyncToolbarProvider())
    mockkStatic(ActionManager::getInstance)

    should("run activity and resolve action group toolbar when action group is not null") {
      var isResolved = false
      val mockedActionGroupForTest = mockk<ActionGroup>()
      every { ActionManager.getInstance() } returns mockedActionManagerInstance
      every {
        mockedActionManagerInstance.getAction(any() as String)
      } answers {
        isResolved = true
        mockedActionGroupForTest
      }
      syncToolbarProviderForTest.runActivity(mockedProject)

      assertSoftly {
        isResolved shouldBe true
      }
    }

    should("run activity and resolve action group toolbar when action group is simple action") {
      var isResolved = false
      val mockedNotActionGroupForTest = mockk<AnAction>()
      every { ActionManager.getInstance() } returns mockedActionManagerInstance
      every { mockedActionManagerInstance.getAction(any() as String) } returns mockedNotActionGroupForTest
      every { mockedActionManagerInstance.registerAction(any() as String, any() as DefaultActionGroup) } answers {
        isResolved = true
      }

      syncToolbarProviderForTest.runActivity(mockedProject)

      assertSoftly {
        isResolved shouldBe true
      }
    }

    unmockkAll()
  }
})
