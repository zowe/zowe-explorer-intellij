package eu.ibagroup.formainframe.utils

import com.intellij.openapi.ui.ValidationInfo
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

            actual.shouldBe(expected)
        }

        should("return null") {
            jTextField.text = ""
            val text = "value"
            val actual = validateForBlank(text, jTextField)
            val expected = null

            actual.shouldBe(expected)
        }
    }
})