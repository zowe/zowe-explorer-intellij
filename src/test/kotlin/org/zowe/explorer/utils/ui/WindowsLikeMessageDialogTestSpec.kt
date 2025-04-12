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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.utils.ui

import com.intellij.internal.statistic.eventLog.FeatureUsageUiEvents
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperPeerFactory
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.UiInterceptors
import com.intellij.util.ui.EDT
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.EventQueue
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JPanel

class WindowsLikeMessageDialogTestSpec : ShouldSpec({

  afterSpec {
    unmockkAll()
    clearAllMocks()
  }

  context("utils/ui/WindowsLikeMessageDialog") {
    mockkStatic(ApplicationManager::getApplication)
    every { ApplicationManager.getApplication() } returns mockk(relaxUnitFun = true) {
      every { isDispatchThread } returns true
      every { isUnitTestMode } returns true
      every {
        getService(any<Class<*>>())
      } answers {
        val serviceClass = firstArg<Class<*>>()
        when (serviceClass) {
          WindowManager::class.java -> mockk<WindowManagerEx> {
            every { suggestParentWindow(any<Project>()) } returns mockk()
          }
          ApplicationInfo::class.java -> mockk<ApplicationInfo> {
            every { hasContextHelp() } returns false
          }
          FeatureUsageUiEvents::class.java -> mockk<FeatureUsageUiEvents>(relaxUnitFun = true)
          DialogWrapperPeerFactory::class.java -> mockk<DialogWrapperPeerFactory> {
            every { createPeer(any(), any<Project>(), any<Boolean>(), any<DialogWrapper.IdeModalityType>()) } returns mockk(relaxUnitFun = true) {
              every { window } returns null
              every { rootPane } returns null
            }
          }
          else -> null
        }
      }
      every { isDisposed } returns false
      every {
        runWriteIntentReadAction(any<ThrowableComputable<*, *>>())
      } answers {
        firstArg<ThrowableComputable<*, *>>().compute()
      }
      every { defaultModalityState } returns mockk()
      every { isReadAccessAllowed } returns true
    }

    mockkStatic(EDT::isCurrentThreadEdt)
    every { EDT.isCurrentThreadEdt() } returns true

    mockkStatic(EventQueue::isDispatchThread)
    every { EventQueue.isDispatchThread() } returns true

    val actions = arrayOf(
      "Skip the conflicting file(s)",
      "Replace the file(s) in the destination",
      "Decide for each file"
    )

    val mockProject = mockk<Project>()

    context("common functions") {
      val customDialog = runBlocking {
        withContext(Dispatchers.IO) {
          WindowsLikeMessageDialog(
            mockProject,
            null,
            "The destination already has file(s) with\nthe same name.\n" +
              "Please, select an action.",
            "Name conflicts in 1 file(s)",
            actions,
            0,
            0,
            null,
            null,
            false,
            "helpId"
          )
        }
      }

      should("create right side empty actions of the dialog") {
        val classUnderTest = spyk(customDialog, "testDialog")
        val methodToTest = classUnderTest::class.java.declaredMethods.single { it.name == "createActions" }
        val result = methodToTest.invoke(classUnderTest)
        val expected = mutableListOf<Action>().toTypedArray()
        assertSoftly { result as Array<*> shouldBe expected }
      }

      should("create left side actions of the dialog") {
        val classUnderTest = spyk(customDialog, "testDialog")
        val methodToTest = classUnderTest::class.java.declaredMethods.single { it.name == "createLeftSideActions" }
        val result = methodToTest.invoke(classUnderTest) as Array<*>
        val actionToPerform = (result[0] as Action)

        // Call default action to be able to cover lambda expression
        actionToPerform.actionPerformed(null)

        assertSoftly {
          // Plus help action, because helpId is not null
          result.size shouldBe 4
        }
      }

      should("create buttons panel of the dialog with 3 buttons") {
        val classUnderTest = spyk(customDialog, "testDialog")
        val methodToTest = classUnderTest::class.java.declaredMethods.single { it.name == "createButtonsPanel" }
        val arguments = mutableListOf(JButton(actions[0]), JButton(actions[1]), JButton(actions[2]))
        val result = methodToTest.invoke(classUnderTest, arguments) as JPanel

        assertSoftly {
          ((result.getComponent(0) as JPanel).getComponent(0) as JButton).text shouldBe actions[0]
          ((result.getComponent(1) as JPanel).getComponent(0) as JButton).text shouldBe actions[1]
          ((result.getComponent(2) as JPanel).getComponent(0) as JButton).text shouldBe actions[2]
        }
      }
    }

    context("WindowsLikeMessageDialog.showWindowsLikeMessageDialog") {
      should("call showWindowsLikeMessageDialog") {
        mockkStatic(UiInterceptors::class)
        every { UiInterceptors.tryIntercept(any()) } returns true
        val exitCode = runBlocking {
          withContext(Dispatchers.IO) {
            WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
              mockProject,
              null,
              "The destination already has file(s) with\nthe same name.\n" +
                "Please, select an action.",
              "Name conflicts in 1 file(s)",
              actions,
              0,
              0,
              null,
              null,
              false,
              null
            )
          }
        }
        assertSoftly { exitCode shouldBe 1 }
      }
    }
  }
})
