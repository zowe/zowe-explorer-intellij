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
 *   Katsiaryna Tsytsenia
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.config.settings.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import io.kotest.assertions.assertSoftly
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.utils.validateJobReturnCode
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.testutils.setPrivateFieldValue
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JButton

class SettingsConfigurableTestSpec : AppInitShouldSpec("config/settings/ui/SettingsConfigurable", {
  context("all functions") {
    var didChangeSuccessMaxCode = false
    var didChangeWarningMaxCode = false

    val configService = ConfigService.getService()
    every { configService.successMaxCode } returns 0
    every {
      configService.successMaxCode = any()
    } answers {
      didChangeSuccessMaxCode = true
    }
    every { configService.warningMaxCode } returns 7
    every {
      configService.warningMaxCode = any()
    } answers {
      didChangeWarningMaxCode = true
    }

    val settingsConfigurable = spyk<SettingsConfigurable>(recordPrivateCalls = true)

    val panel = settingsConfigurable.createPanel()

    val successTextField = panel.getComponent(2) as JBTextField
    val warningTextField = panel.getComponent(4) as JBTextField
    val batchTextField = panel.getComponent(8) as JBTextField

    beforeEach {
      successTextField.text = "0"
      warningTextField.text = "7"
      setPrivateFieldValue(settingsConfigurable, "isReturnCodesValid", true)
      settingsConfigurable.apply()

      didChangeSuccessMaxCode = false
      didChangeWarningMaxCode = false
    }

    should("isModified") {
      settingsConfigurable.isModified shouldBe false
      val isAutoSyncEnabledField = panel.getComponent(9) as JBCheckBox
      isAutoSyncEnabledField.doClick()
      settingsConfigurable.isModified shouldBe true
      settingsConfigurable.reset()
      settingsConfigurable.isModified shouldBe false
      batchTextField.text = "200"
      settingsConfigurable.isModified shouldBe true
      settingsConfigurable.reset()
      settingsConfigurable.isModified shouldBe false
      successTextField.text = "2"
      settingsConfigurable.isModified shouldBe true
      settingsConfigurable.reset()
      settingsConfigurable.isModified shouldBe false
      warningTextField.text = "9"
      settingsConfigurable.isModified shouldBe true
      settingsConfigurable.reset()
      settingsConfigurable.isModified shouldBe false
    }

    should("Rate Us") {
      var isRateUsOpened = false
      mockkStatic(BrowserUtil::class)
      every {
        BrowserUtil.browse(any<String>())
      } answers {
        isRateUsOpened = true
      }
      ApplicationManager.getApplication().invokeAndWait {
        val rateUsButton = panel.getComponent(13) as JButton
        rateUsButton.doClick()
        isRateUsOpened shouldBe true
      }
    }

    should("Validation") {
      successTextField.text = "-1"
      setPrivateFieldValue(settingsConfigurable, "isReturnCodesValid", true)
      settingsConfigurable.apply()
      settingsConfigurable.reset()
      getPrivateFieldValue(settingsConfigurable, "isReturnCodesValid") shouldBe false

      setPrivateFieldValue(settingsConfigurable, "panel", null)
      setPrivateFieldValue(settingsConfigurable, "isReturnCodesValid", true)
      successTextField.text = "2"
      settingsConfigurable.apply()
      settingsConfigurable.reset()
      setPrivateFieldValue(settingsConfigurable, "panel", panel)
      getPrivateFieldValue(settingsConfigurable, "isReturnCodesValid") shouldBe true
    }

    should("JES Explorer") {
      successTextField.text = "-1"
      setPrivateFieldValue(settingsConfigurable, "isReturnCodesValid", false)
      settingsConfigurable.apply()
      assertSoftly {
        didChangeSuccessMaxCode shouldBe false
        didChangeWarningMaxCode shouldBe false
      }
      setPrivateFieldValue(settingsConfigurable, "isReturnCodesValid", true)
      successTextField.text = "2"
      warningTextField.text = "9"
      settingsConfigurable.apply()
      assertSoftly {
        didChangeSuccessMaxCode shouldBe true
        didChangeWarningMaxCode shouldBe true
      }
      settingsConfigurable.cancel()
      setPrivateFieldValue(settingsConfigurable, "panel", null)
      settingsConfigurable.reset()
      setPrivateFieldValue(settingsConfigurable, "panel", panel)
    }

    should("validateJobReturnCode") {
      val jobReturnCodeValidationInfo1 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      )
      jobReturnCodeValidationInfo1 shouldBe null

      successTextField.text = "-1"
      val jobReturnCodeValidationInfo2 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo2.message shouldBe "Return code should be greater or equal than 0"
      successTextField.text = "1"

      successTextField.text = "T"
      val jobReturnCodeValidationInfo3 = validateJobReturnCode(
        successTextField,
        AtomicInteger(-1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo3.message shouldBe "Return code should be greater or equal than 0"
      successTextField.text = "1"

      warningTextField.text = "-1"
      successTextField.text = "-1"
      val jobReturnCodeValidationInfo4 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo4.message shouldBe "Return code should be greater or equal than 0"
      successTextField.text = "1"

      warningTextField.text = "-1"
      val jobReturnCodeValidationInfo5 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo5.message shouldBe "Return code should be greater or equal than 0"
      warningTextField.text = "8"

      warningTextField.text = "T"
      val jobReturnCodeValidationInfo6 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(-1),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo6.message shouldBe "Return code should be greater or equal than 0"
      warningTextField.text = "8"

      successTextField.text = "-"
      val jobReturnCodeValidationInfo7 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      )
      jobReturnCodeValidationInfo7 shouldBe null
      successTextField.text = "1"

      warningTextField.text = "-"
      val jobReturnCodeValidationInfo8 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      )
      jobReturnCodeValidationInfo8 shouldBe null
      warningTextField.text = "8"

      successTextField.text = "9"
      val jobReturnCodeValidationInfo9 = validateJobReturnCode(
        successTextField,
        AtomicInteger(1),
        warningTextField,
        AtomicInteger(8),
        successTextField
      ) as ValidationInfo
      jobReturnCodeValidationInfo9.message shouldBe "Success return code should be less than warning return code"

      settingsConfigurable.apply()
    }
  }
})