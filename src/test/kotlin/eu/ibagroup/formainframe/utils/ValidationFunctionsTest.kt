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
import eu.ibagroup.formainframe.config.ConfigSandboxImpl
import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import eu.ibagroup.formainframe.config.connect.ui.ConnectionsTableModel
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import javax.swing.JTextField

// TODO: transform to ShouldSpec
class ValidationFunctionsTest {
  val sandbox = ConfigSandboxImpl()
  val conTab = ConnectionsTableModel(sandbox.crudable)
  val connectionDialogStateA = ConnectionDialogState(
    connectionName = "a", connectionUrl = "https://a.com",
    username = "a", password = "a"
  )
  val connectionDialogStateB = ConnectionDialogState(
    connectionName = "b", connectionUrl = "https://b.com",
    username = "b", password = "b"
  )
  val jTextField = JTextField()
  val wsConfigClass = FilesWorkingSetConfig::class.java

  @Test
  fun testValidateConnectionName_ExistingCon() {
    conTab.onAdd(sandbox.crudable, connectionDialogStateA)
    jTextField.text = "a"
    val initialConName = "initialName"
    val actual = validateConnectionName(jTextField, initialConName, sandbox.crudable)
    val expected = ValidationInfo(
      "You must provide unique connection name. Connection ${jTextField.text} already exists.",
      jTextField
    )

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateConnectionName_NewCon() {
    conTab.onAdd(sandbox.crudable, connectionDialogStateA)
    conTab.onAdd(sandbox.crudable, connectionDialogStateB)
    jTextField.text = "c"
    val initialConName = "b"
    val actual = validateConnectionName(jTextField, initialConName, sandbox.crudable)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateForBlank_Null() {
    jTextField.text = null
    val actual = validateForBlank(jTextField)
    val expected = ValidationInfo("This field must not be blank", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateForBlank_BothParams() {
    jTextField.text = ""
    val text = "value"
    val actual = validateForBlank(text, jTextField)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateZosmfUrl_ValidUrl() {
    jTextField.text = "https://some.url"
    val actual = validateZosmfUrl(jTextField)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateZosmfUrl_InvalidUrl() {
    jTextField.text = "wrong url\""
    val actual = validateZosmfUrl(jTextField)
    val expected = ValidationInfo("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  @DisplayName("xgf")
  fun testValidateFieldWithLengthRestriction_OutOfLimit() {
    jTextField.text = "ewrtyugifkhuf"
    val lengthLimit = 10
    val fieldName = "Test field"
    val actual = validateFieldWithLengthRestriction(jTextField, lengthLimit, fieldName)
    val expected = ValidationInfo("$fieldName length must not exceed $lengthLimit characters.")

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateFieldWithLengthRestriction_InLimit() {
    jTextField.text = "ewrtyugifkhuf"
    val lengthLimit = 15
    val fieldName = "Test field"
    val actual = validateFieldWithLengthRestriction(jTextField, lengthLimit, fieldName)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_OutOfBoundDsMask() {
    jTextField.text = "qwertyui.qwertyui.qwertyui.qwertyui.qwertyui.qwe"
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = ValidationInfo("Dataset mask length must not exceed 44 characters", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_NineSymQualifier() {
    jTextField.text = "qwertyui.qwertyuio"
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = ValidationInfo("Qualifier must be in 1 to 8 characters", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_ForbiddenSymbol() {
    jTextField.text = "qwerty^.qwerty&"
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = ValidationInfo("Enter valid dataset mask", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_EmptyMask() {
    val jTextField2 = JTextField()
    val actual = validateDatasetMask(jTextField2.text, jTextField2)
    val expected = ValidationInfo("Enter valid dataset mask", jTextField2)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_InvalidAsterisks() {
    jTextField.text = "*gh*gh*.qwerty.qwerty"
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = ValidationInfo("Invalid asterisks in the qualifier", jTextField)

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_ValidMask() {
    jTextField.text = "DSM.*"
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateWorkingSetMaskName() {
    jTextField.text = ""
    val actual = validateWorkingSetName(jTextField, "", sandbox.crudable, wsConfigClass)
    val expected = null

    assertEquals(expected, actual)
  }

  @Test
  fun testValidateDatasetMask_EmptyMask4() {
    jTextField.text = ""
    val actual = validateDatasetMask(jTextField.text, jTextField)
    val expected = ValidationInfo("Enter valid dataset mask", jTextField)

    assertEquals(expected, actual)
  }
}
