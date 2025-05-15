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
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.tso.config.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.utils.crudable.Crudable
import java.util.*
import javax.swing.JTextField

class TSOSessionDialogTestSpec : AppInitShouldSpec("tso/config/ui/TSOSessionDialog", {
  val crudableMock = mockk<Crudable> {
    every {
      getAll(any<Class<*>>())
    } answers {
      emptyList<Any>().stream()
    }
    every { find(any<Class<*>>(), any()) } answers { listOf<Any>().stream() }
  }

  beforeEach {
    every {
      crudableMock.getAll(any<Class<*>>())
    } answers {
      emptyList<Any>().stream()
    }
    every { crudableMock.getByUniqueKey(any<Class<*>>(), any<Any>()) } returns Optional.ofNullable(null)
  }

  context("all functions") {
    should("reset field values to default") {
      val resetToDefaultMethod = TSOSessionDialog::class.java.getDeclaredMethod("resetToDefault")
      resetToDefaultMethod.isAccessible = true

      val tsoSessionDialogState = TSOSessionDialogState()
      tsoSessionDialogState.logonProcedure = "TSTHERE"
      tsoSessionDialogState.charset = "123"

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)

        resetToDefaultMethod.invoke(tsoSessionDialog)

        val defaultTSOSessionDialogState = TSOSessionDialogState()
        val resetLogonProcField = getPrivateFieldValue(tsoSessionDialog, "logonProcField") as JTextField
        val resetCharsetField = getPrivateFieldValue(tsoSessionDialog, "charsetField") as JTextField

        assertSoftly {
          resetLogonProcField.text shouldBe defaultTSOSessionDialogState.logonProcedure
          resetCharsetField.text shouldBe defaultTSOSessionDialogState.charset
        }
      }
    }
    should("validate dialog panel fields with the default TSO session dialog state") {
      val tsoSessionDialogState = TSOSessionDialogState(mode = DialogMode.UPDATE)

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)
        val tsoSessionDialogPanel = getPrivateFieldValue(tsoSessionDialog, "mainPanel") as DialogPanel

        val sessionNameField = getPrivateFieldValue(tsoSessionDialog, "sessionNameField")
        val connectionBox = getPrivateFieldValue(tsoSessionDialog, "connectionBox")

        val validationResult = tsoSessionDialogPanel.validateAll()

        val sessionNameFieldValidationStatus = validationResult.find { it.component == sessionNameField }
        val connectionBoxValidationStatus = validationResult.find { it.component == connectionBox }

        assertSoftly {
          validationResult.size shouldBe 2
          sessionNameFieldValidationStatus shouldNotBe null
          (sessionNameFieldValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          connectionBoxValidationStatus shouldNotBe null
          (connectionBoxValidationStatus?.message ?: "") shouldBe "You must provide a connection"
        }
      }
    }
    should("validate dialog panel fields with the filled in dialog") {
      val tsoSessionDialogState = TSOSessionDialogState(
        name = "Not empty",
        charset = ""
      )

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)
        val tsoSessionDialogPanel = getPrivateFieldValue(tsoSessionDialog, "mainPanel") as DialogPanel

        val colsField = getPrivateFieldValue(tsoSessionDialog, "colsField") as JTextField
        colsField.text = ""
        val rowsField = getPrivateFieldValue(tsoSessionDialog, "rowsField") as JTextField
        rowsField.text = ""
        val regionField = getPrivateFieldValue(tsoSessionDialog, "regionField") as JTextField
        regionField.text = ""
        val timeoutField = getPrivateFieldValue(tsoSessionDialog, "timeoutField") as JTextField
        timeoutField.text = ""
        val maxAttemptsField = getPrivateFieldValue(tsoSessionDialog, "maxAttemptsField") as JTextField
        maxAttemptsField.text = ""

        val connectionBox = getPrivateFieldValue(tsoSessionDialog, "connectionBox")
        val charsetField = getPrivateFieldValue(tsoSessionDialog, "charsetField")

        val validationResult = tsoSessionDialogPanel.validateAll()

        val rowsFieldValidationStatus = validationResult.find { it.component == rowsField }
        val colsFiledValidationStatus = validationResult.find { it.component == colsField }
        val regionFiledValidationStatus = validationResult.find { it.component == regionField }
        val connectionBoxValidationStatus = validationResult.find { it.component == connectionBox }
        val charsetFieldValidationStatus = validationResult.find { it.component == charsetField }
        val timeoutFieldValidationStatus = validationResult.find { it.component == charsetField }
        val maxAttemptsFieldValidationStatus = validationResult.find { it.component == maxAttemptsField }

        assertSoftly {
          validationResult.size shouldBe 7
          rowsFieldValidationStatus shouldNotBe null
          (rowsFieldValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          colsFiledValidationStatus shouldNotBe null
          (colsFiledValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          regionFiledValidationStatus shouldNotBe null
          (regionFiledValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          connectionBoxValidationStatus shouldNotBe null
          (connectionBoxValidationStatus?.message ?: "") shouldBe "You must provide a connection"
          charsetFieldValidationStatus shouldNotBe null
          (charsetFieldValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          timeoutFieldValidationStatus shouldNotBe null
          (timeoutFieldValidationStatus?.message ?: "") shouldBe "This field must not be blank"
          maxAttemptsFieldValidationStatus shouldNotBe null
          (maxAttemptsFieldValidationStatus?.message ?: "") shouldBe "This field must not be blank"
        }
      }
    }
    should("perform validation on the TSO session dialog panel and get a validation info") {
      val doValidateMethod = TSOSessionDialog::class.java.getDeclaredMethod("doValidate")
      doValidateMethod.isAccessible = true

      val tsoSessionDialogState = TSOSessionDialogState()
      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)
        val sessionNameField = getPrivateFieldValue(tsoSessionDialog, "sessionNameField")

        val sessionNameFieldValidationStatus = doValidateMethod.invoke(tsoSessionDialog) as ValidationInfo

        assertSoftly {
          sessionNameFieldValidationStatus shouldNotBe null
          sessionNameFieldValidationStatus.component shouldBe sessionNameField
          sessionNameFieldValidationStatus.message shouldBe "This field must not be blank"
        }
      }
    }
    should("perform validation on the TSO session dialog panel and return null as all the fields are valid") {
      every {
        crudableMock.getAll(any<Class<ConnectionConfig>>())
      } answers {
        listOf(mockk<ConnectionConfig>()).stream()
      }
      every {
        crudableMock.getByUniqueKey(any<Class<ConnectionConfig>>(), any<String>())
      } answers {
        Optional.of(
          mockk<ConnectionConfig> {
            every { uuid } returns "test"
          }
        )
      }

      val doValidateMethod = TSOSessionDialog::class.java.getDeclaredMethod("doValidate")
      doValidateMethod.isAccessible = true

      val tsoSessionDialogState = TSOSessionDialogState(name = "Not empty")

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)

        val validationInfo = doValidateMethod.invoke(tsoSessionDialog) as ValidationInfo?

        assertSoftly { validationInfo shouldBe null }
      }
    }
    should("get a default preferred focused component") {
      val tsoSessionDialogState = TSOSessionDialogState(name = "Not empty")

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)

        val result = tsoSessionDialog.preferredFocusedComponent

        assertSoftly { result shouldNotBe null }
      }
    }
    should("get a null instead of a preferred focused component cause the dialog panel's preferred focused component is not set") {
      val tsoSessionDialogState = TSOSessionDialogState(name = "Not empty")

      ApplicationManager.getApplication().invokeAndWait {
        val tsoSessionDialog = TSOSessionDialog(crudableMock, tsoSessionDialogState)
        val tsoSessionDialogPanel = getPrivateFieldValue(tsoSessionDialog, "mainPanel") as DialogPanel
        tsoSessionDialogPanel.preferredFocusedComponent = null

        val result = tsoSessionDialog.preferredFocusedComponent

        assertSoftly { result shouldBe null }
      }
    }
  }
})
