/*
 * Copyright (c) 2024 IBA Group
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

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ZoweTeamConfigDialog
import org.zowe.explorer.testutils.WithApplicationShouldSpec
import org.zowe.explorer.testutils.testServiceImpl.TestConfigServiceImpl
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KFunction

class AddZoweTeamConfigActionTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
    unmockkAll()
  }

  var isFindFileByNioPathCalled = false
  var isShowUntilDoneSucceeded = false

  beforeEach {
    isFindFileByNioPathCalled = false
    isShowUntilDoneSucceeded = false
  }

  context("AddZoweTeamConfigAction") {

    val addZoweTeamConfigAction = spyk<AddZoweTeamConfigAction>(recordPrivateCalls = true)
    val event = mockk<AnActionEvent>()
    every { event.project } returns ProjectManager.getInstance().defaultProject
    every { event.presentation } returns Presentation()

    val vfMock = mockk<VirtualFile>()
    every { vfMock.refresh(any(), any()) } returns Unit
    val vfmMock: VirtualFileManager = mockk<VirtualFileManager>()
    mockkStatic(VirtualFileManager::class)
    every { VirtualFileManager.getInstance() } returns vfmMock
    every { vfmMock.findFileByNioPath(any<Path>()) } answers {
      isFindFileByNioPathCalled = true
      vfMock
    }

    val configService = ConfigService.getService() as TestConfigServiceImpl
    every { configService.crudable.nextUniqueValue<ConnectionConfig, String>(ConnectionConfig::class.java) } returns "uuid"
    every {
      configService.crudable.find(
        ConnectionConfig::class.java,
        any()
      )
    } answers { listOf(ConnectionConfig()).stream() }
    every { configService.crudable.getAll(ConnectionConfig::class.java) } answers { listOf(ConnectionConfig()).stream() }
    every { configService.crudable.add(any()) } returns Optional.of(ConnectionConfig::class.java)

    mockkConstructor(ZoweConfigServiceImpl::class)
    every { anyConstructed<ZoweConfigServiceImpl>().addZoweConfigFile(any<ConnectionDialogState>()) } returns

        mockkConstructor(ZoweTeamConfigDialog::class)
    every { anyConstructed<ZoweTeamConfigDialog>().showAndGet() } returns true
    var initState = ConnectionDialogState()


    val showUntilDoneMockk: (
      ConnectionDialogState, (ConnectionDialogState) -> StatefulDialog<ConnectionDialogState>, (ConnectionDialogState) -> Boolean
    ) -> ConnectionDialogState? = ::showUntilDone
    mockkStatic(showUntilDoneMockk as KFunction<*>)
    every {
      hint(ConnectionDialogState::class)
      showUntilDoneMockk(
        any<ConnectionDialogState>(),
        any<(ConnectionDialogState) -> StatefulDialog<ConnectionDialogState>>(),
        any<(ConnectionDialogState) -> Boolean>()
      )
    } answers {
      initState = firstArg<ConnectionDialogState>()
      val thirdBlockResult = thirdArg<(ConnectionDialogState) -> Boolean>()
      isShowUntilDoneSucceeded = thirdBlockResult(initState)
      initState
    }

    mockkObject(MessageDialogBuilder.Companion)
    every { MessageDialogBuilder.yesNo(any<String>(), any<String>()) } returns mockk {
      every { icon(any()) } returns this
      every { asWarning() } returns this
      every { ask(any<Project>()) } returns true
    }

    should("getActionUpdateThread") {
      addZoweTeamConfigAction.actionUpdateThread shouldBe ActionUpdateThread.EDT
    }

    should("isDumbAware") {
      addZoweTeamConfigAction.isDumbAware shouldBe true
    }

    should("update") {
      addZoweTeamConfigAction.update(event)
      isFindFileByNioPathCalled shouldBe true
    }

    should("update null project ") {
      every { event.project } returns null
      addZoweTeamConfigAction.update(event)
      every { event.project } returns ProjectManager.getInstance().defaultProject
      isFindFileByNioPathCalled shouldBe false
    }

    should("update null file ") {
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        null
      }
      addZoweTeamConfigAction.update(event)
      isFindFileByNioPathCalled shouldBe true
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        vfMock
      }
    }

    should("actionPerformed") {


      runBlocking {
        withContext(Dispatchers.EDT) {
          addZoweTeamConfigAction.actionPerformed(event)
        }
      }
      isFindFileByNioPathCalled shouldBe true
      isShowUntilDoneSucceeded shouldBe true
    }

    should("actionPerformed null project ") {
      every { event.project } returns null
      runBlocking {
        withContext(Dispatchers.EDT) {
          addZoweTeamConfigAction.actionPerformed(event)
        }
      }
      isShowUntilDoneSucceeded shouldBe true
      isFindFileByNioPathCalled shouldBe false
      every { event.project } returns ProjectManager.getInstance().defaultProject
    }

    should("actionPerformed null findFileByNioPath ") {
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        null
      }
      runBlocking {
        withContext(Dispatchers.EDT) {
          addZoweTeamConfigAction.actionPerformed(event)
        }
      }
      isFindFileByNioPathCalled shouldBe true
      isShowUntilDoneSucceeded shouldBe true
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        vfMock
      }
    }

  }
})