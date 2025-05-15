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
 *   Katsiaryna Tsytsenia
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.common.ui.showUntilDone
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.ui.zosmf.ZoweTeamConfigDialog
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.zowe.service.ZoweConfigServiceImpl
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KFunction

class AddZoweTeamConfigActionTestSpec : AppInitShouldSpec("explorer/actions/AddZoweTeamConfigAction", {
  var isFindFileByNioPathCalled = false
  var isShowUntilDoneSucceeded = false
  var isErrorNotificationProduced = false

  beforeEach {
    isFindFileByNioPathCalled = false
    isShowUntilDoneSucceeded = false
    isErrorNotificationProduced = false
  }

  context("AddZoweTeamConfigAction") {
    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      isErrorNotificationProduced = true
    }

    val addZoweTeamConfigAction = spyk<AddZoweTeamConfigAction>(recordPrivateCalls = true)
    val event = mockk<AnActionEvent> {
      every { project } returns ProjectManager.getInstance().defaultProject
      every { presentation } returns Presentation()
    }

    val vfMock = mockk<VirtualFile> {
      every { refresh(any(), any()) } returns Unit
      every { isValid } returns true
      every { isDirectory } returns false
      every { fileType } returns com.intellij.openapi.fileTypes.FileTypes.PLAIN_TEXT
    }
    mockkConstructor(OpenFileDescriptor::class)
    every { anyConstructed<OpenFileDescriptor>().navigate(any<Boolean>()) } returns Unit

    val vfmMock: VirtualFileManager = mockk<VirtualFileManager>()
    mockkStatic(VirtualFileManager::class)
    every { VirtualFileManager.getInstance() } returns vfmMock
    every { vfmMock.findFileByNioPath(any<Path>()) } answers {
      isFindFileByNioPathCalled = true
      vfMock
    }

    val configServiceCrudable = mockk<Crudable> {
      every { nextUniqueValue<ConnectionConfig, String>(ConnectionConfig::class.java) } returns "uuid"
      every {
        find(ConnectionConfig::class.java, any())
      } answers {
        listOf(ConnectionConfig()).stream()
      }
      every {
        getAll(ConnectionConfig::class.java)
      } answers {
        listOf(ConnectionConfig()).stream()
      }
      every { add(any()) } returns Optional.of(ConnectionConfig::class.java)
    }
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudable

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()
    every { credentialService.setCredentials(any<String>(), any<String>(), any<CharArray>()) } returns Unit

    mockkConstructor(ZoweConfigServiceImpl::class)
    every {
      anyConstructed<ZoweConfigServiceImpl>().addZoweConfigFile(any<ConnectionDialogState>())
    } returns mockkConstructor(ZoweTeamConfigDialog::class)
    every { anyConstructed<ZoweTeamConfigDialog>().showAndGet() } returns true
    lateinit var initState: ConnectionDialogState

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

    mockkObject(MessageDialogBuilder)
    every {
      MessageDialogBuilder.yesNo(any<String>(), any<String>())
    } returns mockk {
      every { icon(any()) } returns this
      every { asWarning() } returns this
      every { ask(any<Project>()) } returns true
    }

    should("getActionUpdateThread") {
      addZoweTeamConfigAction.actionUpdateThread shouldBe ActionUpdateThread.EDT

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("isDumbAware") {
      addZoweTeamConfigAction.isDumbAware shouldBe true

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("update") {
      addZoweTeamConfigAction.update(event)
      isFindFileByNioPathCalled shouldBe true

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("update null project ") {
      every { event.project } returns null
      addZoweTeamConfigAction.update(event)
      every { event.project } returns ProjectManager.getInstance().defaultProject
      isFindFileByNioPathCalled shouldBe false

      assertSoftly { isErrorNotificationProduced shouldBe false }
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

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("actionPerformed") {
      runInEdtAndWait {
        addZoweTeamConfigAction.actionPerformed(event)
      }

      isFindFileByNioPathCalled shouldBe true
      isShowUntilDoneSucceeded shouldBe false

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("actionPerformed null project and file exists") {
      every { event.project } returns null
      runInEdtAndWait {
        addZoweTeamConfigAction.actionPerformed(event)
      }

      isShowUntilDoneSucceeded shouldBe false
      isFindFileByNioPathCalled shouldBe true
      every { event.project } returns ProjectManager.getInstance().defaultProject

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

    should("actionPerformed null project and file does not exist") {
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        null
      }
      every { event.project } returns null
      runInEdtAndWait {
        addZoweTeamConfigAction.actionPerformed(event)
      }

      isShowUntilDoneSucceeded shouldBe true
      isFindFileByNioPathCalled shouldBe true
      every { event.project } returns ProjectManager.getInstance().defaultProject
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        vfMock
      }

      assertSoftly { isErrorNotificationProduced shouldBe true }
    }

    should("actionPerformed null findFileByNioPath ") {
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        null
      }
      runInEdtAndWait {
        addZoweTeamConfigAction.actionPerformed(event)
      }

      isFindFileByNioPathCalled shouldBe true
      isShowUntilDoneSucceeded shouldBe true
      every { vfmMock.findFileByNioPath(any<Path>()) } answers {
        isFindFileByNioPathCalled = true
        vfMock
      }

      assertSoftly { isErrorNotificationProduced shouldBe false }
    }

  }
})
