/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.UiInterceptors
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.EventQueue
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JPanel

class WindowsLikeMessageDialogTestSpec : WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("Windows dialog common spec") {
    val application = ApplicationManager.getApplication()
    val project = ProjectManager.getInstance().defaultProject
    mockkStatic(EventQueue::class)
    mockkObject(application)
    every { EventQueue.isDispatchThread() } returns true
    every { application.isDispatchThread } returns true

    val actions = arrayOf(
      "Skip the conflicting file(s)",
      "Replace the file(s) in the destination",
      "Decide for each file"
    )

    val customDialog = WindowsLikeMessageDialog(
      project,
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

    val classUnderTest = spyk(customDialog, "testDialog")

    should("create right side empty actions of the dialog") {
      val expected = mutableListOf<Action>().toTypedArray()
      val methodToTest = classUnderTest::class.java.declaredMethods.single { it.name == "createActions" }
      val result = methodToTest.invoke(classUnderTest)

      assertSoftly {
        result as Array<*> shouldBe expected
      }
    }

    should("create left side actions of the dialog") {
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
      val methodToTest = classUnderTest::class.java.declaredMethods.single { it.name == "createButtonsPanel" }
      val arguments = mutableListOf(JButton(actions[0]), JButton(actions[1]), JButton(actions[2]))
      val result = methodToTest.invoke(classUnderTest, arguments) as JPanel

      assertSoftly {
        ((result.getComponent(0) as JPanel).getComponent(0) as JButton).text shouldBe "Skip the conflicting file(s)"
        ((result.getComponent(1) as JPanel).getComponent(0) as JButton).text shouldBe "Replace the file(s) in the destination"
        ((result.getComponent(2) as JPanel).getComponent(0) as JButton).text shouldBe "Decide for each file"
      }
    }
    unmockkAll()
  }

  context("test showWindowsLikeMessageDialog static function") {
    val project = ProjectManager.getInstance().defaultProject
    mockkStatic(EventQueue::class)
    every { EventQueue.isDispatchThread() } returns true

    val actions = arrayOf(
      "Skip the conflicting file(s)",
      "Replace the file(s) in the destination",
      "Decide for each file"
    )

    should("call showWindowsLikeMessageDialog") {
      mockkStatic(UiInterceptors::class)
      every { UiInterceptors.tryIntercept(any()) } returns true
      val exitCode = runBlocking {
        withContext(Dispatchers.EDT) {
          WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
            project,
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
      assertSoftly {
        exitCode shouldBe 1
      }
    }
    unmockkAll()
  }
})
