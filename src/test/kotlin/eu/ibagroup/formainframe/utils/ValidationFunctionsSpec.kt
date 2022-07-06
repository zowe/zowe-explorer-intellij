/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.ui.ValidationInfo
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import javax.swing.JTextField

class ValidationFunctionsSpec : ShouldSpec({
  context("validateForBlank test") {
    val jTextField = JTextField()

    should("return 'must not be blank' error, text = null") {
      jTextField.text = null
      val actual = validateForBlank(jTextField)
      val expected = ValidationInfo("This field must not be blank", jTextField)

      assertSoftly {
        actual shouldBe expected
      }
    }

    should("return null") {
      jTextField.text = ""
      val text = "value"
      val actual = validateForBlank(text, jTextField)
      val expected = null

      assertSoftly {
        actual shouldBe expected
      }
    }
  }
})
