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

package eu.ibagroup.formainframe.utils

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.tso.config.TSOSessionConfig
import eu.ibagroup.formainframe.config.ws.JobsFilter
import eu.ibagroup.formainframe.config.ws.MaskStateWithWS
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.explorer.ui.NodeData
import eu.ibagroup.formainframe.explorer.ui.UssDirNode
import eu.ibagroup.formainframe.explorer.ui.UssFileNode
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.find
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

private val urlRegex = Regex("^(https?|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
private val maskRegex = Regex("^[A-Za-z\\$\\*%@#][A-Za-z0-9\\-\\$\\*%@#]{0,7}")
private val ussPathRegex = Regex("^/$|^(/[^/]+)+$")
private val forbiddenSymbol = "/"
private val warningSymbols = "^[^>|:& ]*$"
private val prefixAndOwnerRegex = Regex("[A-Za-z0-9*%]+")
private val firstSymbol = "A-Za-z\$@#"
private val remainingSymbol = firstSymbol + "0-9\\-"
private val partPattern = "([$firstSymbol][$remainingSymbol]{0,7})"
private val notEmptyErrorText = "Dataset name must not be empty"
private val segmentLengthErrorText = "Each name segment (qualifier) is 1 to 8 characters"
private val charactersLengthExceededErrorText = "Dataset name cannot exceed 44 characters"
private val segmentCharsErrorText =
  "$segmentLengthErrorText," +
      "\nthe first of which must be alphabetic (A to Z) or national (# @ \$)." +
      "\nThe remaining seven characters are either alphabetic," +
      "\nnumeric (0 - 9), national, a hyphen (-)." +
      "\nName segments are separated by a period (.)"
private val jobIdRegex = Regex("[A-Za-z0-9]+")
private val volserRegex = Regex("[A-Za-z0-9]{1,6}")
private val firstLetterRegex = Regex("[A-Z@\$#a-z]")
private val memberRegex = Regex("[A-Z@$#a-z][A-Z@#\$a-z0-9]{0,7}")

/**
 * Validate text field for a match with the previous value
 * @param prev of the current node
 * @param component the component containing the invalid data
 */
fun validateForTheSameValue(prev: String?, component: JTextField): ValidationInfo? {
  return if (component.text == prev) ValidationInfo("Field value matches the previous one", component) else null
}

/**
 * Validate text field for blank value
 * @param text the text of the field to validate
 * @param component the component containing the invalid data
 */
fun validateForBlank(text: String, component: JComponent): ValidationInfo? {
  return if (text.isBlank()) ValidationInfo("This field must not be blank", component) else null
}

/**
 * Validate new password
 * @param password new password
 * @param component confirm password component
 */
fun validateForPassword(password: String, component: JPasswordField): ValidationInfo? {
  return if (password != component.text) ValidationInfo("Passwords do not match", component) else null
}

/**
 * Validate text field for blank value
 * @param component the component to check the text in
 */
fun validateForBlank(component: JTextField): ValidationInfo? {
  return validateForBlank(component.text, component)
}

/**
 * Validate connection name not to be the same as the existing one
 * @param component the component to check the connection name in
 * @param ignoreValue the value to ignore during the validation (blank value at the start)
 * @param crudable crudable object to find the connection config
 */
fun validateConnectionName(component: JTextField, ignoreValue: String? = null, crudable: Crudable): ValidationInfo? {
  val configAlreadyExists = crudable.find<ConnectionConfig> {
    ignoreValue != it.name && it.name == component.text.trim()
  }.count() > 0
  return if (configAlreadyExists) {
    ValidationInfo("You must provide unique connection name. Connection ${component.text} already exists.", component)
  } else {
    null
  }
}

/**
 * Validate working set name not to be the same as the existing one
 * @param component the component to check the working set name
 * @param ignoreValue the value to ignore during the validation (blank value at the start)
 * @param crudable crudable object to find the working set config
 * @param wsConfigClass working set config class to get the crudable
 */
fun <WSConfig : WorkingSetConfig> validateWorkingSetName(
  component: JTextField,
  ignoreValue: String? = null,
  crudable: Crudable,
  wsConfigClass: Class<out WSConfig>
): ValidationInfo? {
  val configAlreadyExists = crudable.find(wsConfigClass) {
    ignoreValue != it.name && it.name == component.text
  }.count() > 0
  return if (configAlreadyExists) {
    ValidationInfo(
      "You must provide unique working set name. Working Set ${component.text} already exists.",
      component
    )
  } else {
    null
  }
}

/**
 * Validate working set mask name not to be the same as the existing one.
 * Validation is skipped if the name of the mask stays the same as the previous one
 * @param component the component to check the working set mask name
 * @param state the mask state with working set inside
 */
fun validateWorkingSetMaskName(component: JTextField, state: MaskStateWithWS): ValidationInfo? {
  val textToCheck = if (state.type == MaskType.ZOS) component.text.uppercase() else component.text
  val sameMaskName = state.mask == textToCheck
  val maskAlreadyExists = state.ws.masks.map { it.mask }.contains(component.text.uppercase())
      || state.ws.ussPaths.map { it.path }.contains(component.text)

  return if (!sameMaskName && maskAlreadyExists) {
    ValidationInfo(
      "You must provide unique mask in working set. Working Set " +
          "\"${state.ws.name}\" already has mask - ${component.text}", component
    )
  } else {
    null
  }

}

/**
 * Validate z/OSMF URL to match the regular expression
 * @param component the component to check the URL
 */
fun validateZosmfUrl(component: JTextField): ValidationInfo? {
  return if (!component.text.matches(urlRegex)) {
    ValidationInfo("Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443", component)
  } else {
    null
  }
}

/**
 * Validate field with specified length restriction
 * @param component the component to validate the text
 * @param length the specified length to check whether the text exceeds it
 * @param fieldName field name for validation warning
 */
fun validateFieldWithLengthRestriction(component: JTextField, length: Int, fieldName: String): ValidationInfo? {
  return if (component.text.trim().length > length) {
    ValidationInfo("$fieldName length must not exceed $length characters.")
  } else {
    null
  }

}

/**
 * Validate that the username is not better than 8 characters long
 * @param component the component with the username to validate
 */
fun validateUsername(component: JTextField): ValidationInfo? {
  return validateFieldWithLengthRestriction(component, 8, "Username")
}

/**
 * Validate that the password is not better than 8 characters long
 * @param component the component with the password to validate
 */
fun validatePassword(component: JTextField): ValidationInfo? {
  return validateFieldWithLengthRestriction(component, 8, "Password")
}

/**
 * Validate that the dataset mask matches the generic rules
 * @param text the dataset mask text to validate
 * @param component the component to show the warning message in
 */
fun validateDatasetMask(text: String, component: JComponent): ValidationInfo? {
  val noMoreThan3AsteriskRule = "\\*{3,}"
  val noMoreThan2AsteriskBeforeTextInTheQualifierRule = "\\*{2,}[^\\*\\.]+"
  val noMoreThan2AsteriskAfterTextInTheQualifierRule = "[^\\*\\.]+\\*{2,}"
  val noSecondAsteriskInTheMiddleOfTheQualifierRule = "\\*+[^\\*\\.]+\\*+[^\\*\\.]+"
  val noFirstAsteriskInTheMiddleOfTheQualifierRule = "[^\\*\\.]+\\*+[^\\*\\.]+\\*+"
  val asteriskRegex = arrayOf(
    noMoreThan3AsteriskRule,
    noMoreThan2AsteriskAfterTextInTheQualifierRule,
    noMoreThan2AsteriskBeforeTextInTheQualifierRule,
    noSecondAsteriskInTheMiddleOfTheQualifierRule,
    noFirstAsteriskInTheMiddleOfTheQualifierRule
  ).joinToString(separator = "|")

  val qualifier = text.split('.')

  return if (text.length > 44) {
    ValidationInfo("Dataset mask length must not exceed 44 characters", component)
  } else if (qualifier.find { it.length > 8 } != null) {
    ValidationInfo("Qualifier must be in 1 to 8 characters", component)
  } else if (text.isBlank() || qualifier.find { !it.matches(maskRegex) } != null) {
    ValidationInfo("Enter valid dataset mask", component)
  } else if (text.contains(Regex(asteriskRegex))) {
    ValidationInfo("Invalid asterisks in the qualifier", component)
  } else if (text.matches(Regex("(\\.?\\*{1,2}\\.?)+"))) {
    ValidationInfo(
      "Dataset mask that contains only asterisks is invalid. You must specify at least one partial qualifier.",
      component
    )
  } else {
    null
  }
}

/**
 * Validate USS mask to match the USS path regular expression
 * @param text the USS path to validate
 * @param component the component to show the warning in
 */
fun validateUssMask(text: String, component: JComponent): ValidationInfo? {
  return if (text.isBlank() || !text.matches(ussPathRegex)) {
    ValidationInfo("Provide a valid USS path", component)
  } else {
    null
  }
}

/**
 * Validate USS file name to match the specified rules
 * @param component the component to validate the USS file name and show warning in
 */
fun validateUssFileName(component: JTextField): ValidationInfo? {
  return if (component.text.length > 255) {
    ValidationInfo("Filename must not exceed 255 characters.", component)
  } else if (component.text.isNotBlank() && component.text.contains(forbiddenSymbol)) {
    ValidationInfo("Filename must not contain reserved '/' symbol.", component)
  } else {
    null
  }
}

/**
 * Validate the job ID
 * @param component the job ID component to validate the text in and show the warning for
 */
fun validateJobId(component: JTextField): ValidationInfo? {
  return if (component.text.isNotBlank()) {
    if (component.text.length != 8) {
      ValidationInfo("Job ID length must be 8 characters.", component)
    } else if (!component.text.matches(jobIdRegex)) {
      ValidationInfo("Text field should contain only A-Z, a-z, 0-9", component)
    } else {
      null
    }
  } else {
    null
  }
}

/**
 * Validate prefix and owner
 * @param component the current active component to check the owner or prefix and show the warning for
 */
fun validatePrefixAndOwner(component: JTextField): ValidationInfo? {
  return if (component.text.isNotBlank()) {
    if (component.text.length > 8) {
      ValidationInfo("Text field must not exceed 8 characters.", component)
    } else if (!component.text.matches(prefixAndOwnerRegex)) {
      ValidationInfo("Text field should contain only A-Z, a-z, 0-9, *, %.", component)
    } else {
      null
    }
  } else {
    null
  }
}

/**
 * Validate job filter to match the specified rules
 * @param prefix the prefix for the job filter
 * @param owner the owner name for the job filter
 * @param jobId the job ID for the job filter
 * @param component the component to show the warning
 * @param isJobId value to check the job ID if it is "true"
 */
fun validateJobFilter(
  prefix: String,
  owner: String,
  jobId: String,
  component: JTextField,
  isJobId: Boolean
): ValidationInfo? {
  if (jobId.isNotBlank()) {
    if (owner.isNotBlank() || prefix.isNotBlank()) {
      return ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)
    }
  }
  if (jobId.isBlank()) {
    if (owner.isBlank() || prefix.isBlank()) {
      return ValidationInfo("You must provide either an owner and a prefix or a job ID.", component)
    }
  }
  return if (isJobId) {
    validateJobId(component)
  } else {
    validatePrefixAndOwner(component)
  }
}

/**
 * Validate job filter to match the specified rules
 * @param prefix the prefix for the job filter
 * @param owner the owner name for the job filter
 * @param jobId the job ID for the job filter
 * @param existentJobFilters the existing job filters to check whether the provided info is already inserted
 * @param component the component to show the warning
 * @param isJobId value to check the job ID if it is "true"
 */
fun validateJobFilter(
  prefix: String,
  owner: String,
  jobId: String,
  existentJobFilters: Collection<JobsFilter>,
  component: JBTextField,
  isJobId: Boolean
): ValidationInfo? {
  val baseValidation = validateJobFilter(prefix, owner, jobId, component, isJobId)
  if (baseValidation != null) {
    return baseValidation
  }
  val newOwner = owner.ifBlank { "" }.uppercase()
  val newPrefix = prefix.ifBlank { "" }.uppercase()
  val newJobId = jobId.ifBlank { "" }.uppercase()
  val isAlreadyExist = existentJobFilters.any { it.owner == newOwner && it.prefix == newPrefix && it.jobId == newJobId }
  return if (isAlreadyExist) {
    ValidationInfo("Job Filter with provided data already exists.", component)
  } else null
}

/**
 * Validate job filter to match the specified rules.
 * Filters out the initial job filter from existent filters to skip check of the initial values left on Edit Job Filter
 * @param initJobFilter the initial jobs filter to skip validation of the same one in existing, when the validation is happening in Edit Job Filter dialog
 * @param prefix the prefix for the job filter
 * @param owner the owner name for the job filter
 * @param jobId the job ID for the job filter
 * @param existentJobFilters the existing job filters to check whether the provided info is already inserted
 * @param component the component to show the warning
 * @param isJobId value to check the job ID if it is "true"
 */
fun validateJobFilter(
  initJobFilter: JobsFilter,
  prefix: String,
  owner: String,
  jobId: String,
  existentJobFilters: Collection<JobsFilter>,
  component: JBTextField,
  isJobId: Boolean
): ValidationInfo? {
  val existentJobFiltersWithoutInit = existentJobFilters
    .filter { it.owner != initJobFilter.owner || it.prefix != initJobFilter.prefix || it.jobId != initJobFilter.jobId }
  return validateJobFilter(prefix, owner, jobId, existentJobFiltersWithoutInit, component, isJobId)
}

/**
 * Validate the USS file name is it already exist
 * @param component the component to check the USS file name and show the warning for
 * @param selectedNode the selected node to check whether it is a file or a directory
 */
fun validateUssFileNameAlreadyExists(component: JTextField, selectedNode: NodeData<*>): ValidationInfo? {
  val text: String = component.text
  val childrenNodesFromParent = selectedNode.node.parent?.children?.filter { it != selectedNode.node }
  when (selectedNode.node) {
    is UssFileNode -> {
      childrenNodesFromParent?.forEach {
        if (it is UssFileNode && it.value.filenameInternal == text) {
          return ValidationInfo("Filename already exists. Please specify another filename.", component).asWarning()
        }
      }
    }

    is UssDirNode -> {
      childrenNodesFromParent?.forEach {
        if (it is UssDirNode && text == it.value.path.split("/").last()) {
          return ValidationInfo(
            "Directory name already exists. Please specify another directory name.",
            component
          ).asWarning()
        }
      }
    }
  }
  return null
}

/**
 * Validate the dataset name on input
 * @param component the component text to validate the dataset name
 */
fun validateDatasetNameOnInput(component: JTextField): ValidationInfo? {
  val text = component.text.trim()
  val length = text.length
  val parts = text.split('.')
  return if (text.isNotEmpty()) {
    if (length > 44) {
      ValidationInfo(charactersLengthExceededErrorText, component)
    } else if (parts.find { !it.matches(Regex(partPattern)) || it.length > 8 } != null) {
      ValidationInfo(segmentCharsErrorText, component)
    } else {
      return null
    }
  } else {
    ValidationInfo(notEmptyErrorText, component)
  }
}

/**
 * Validate the VOLSER value
 * @param component the component with the VOLSER to validate it and show the warning for
 */
fun validateVolser(component: JTextField): ValidationInfo? {
  return if (component.text.isNotBlank() && !component.text.matches(volserRegex)) {
    ValidationInfo("Enter a valid volume serial", component)
  } else {
    null
  }
}

/**
 * Validate the number of type Int to be positive
 * @param component the component with the number to validate
 */
fun validateForPositiveInteger(component: JTextField): ValidationInfo? {
  return validateForGreaterOrEqualValue(component, 0)
}

/**
 * Validate the number of type Long to be positive
 * @param component the component with the number to validate
 */
fun validateForPositiveLong(component: JTextField): ValidationInfo? {
  return validateForGreaterOrEqualValue(component, 0L)
}

/**
 * Validate the component value is greater than or equal to the provided value
 * @param component the component to check the value
 * @param value the value of type Int to check that the component's value is greater
 */
fun validateForGreaterOrEqualValue(component: JTextField, value: Int): ValidationInfo? {
  val number = component.text.toIntOrNull() ?: return ValidationInfo("Enter a valid number", component)
  return if (number < value) {
    ValidationInfo(
      if (value == 0) "Enter a positive number" else "Enter a number greater than or equal to $value",
      component
    )
  } else null
}

/**
 * Validate the component value is greater than or equal to the provided value
 * @param component the component to check the value
 * @param value the value of type Long to check that the component's value is greater
 */
fun validateForGreaterOrEqualValue(component: JTextField, value: Long): ValidationInfo? {
  val number = component.text.toLongOrNull() ?: return ValidationInfo("Enter a valid number", component)
  return if (number < value) {
    ValidationInfo(
      if (value == 0L) "Enter a positive number" else "Enter a number greater than or equal to $value",
      component
    )
  } else null
}

/**
 * Validate that the dataset member name matches the provided rules
 * @param component the component to check the dataset member name and show the warning for
 */
fun validateMemberName(component: JTextField): ValidationInfo? {
  return if (component.text.length > 8) {
    ValidationInfo("Member name must not exceed 8 characters.", component)
  } else if (component.text.isNotEmpty() && !component.text[0].toString().matches(firstLetterRegex)) {
    ValidationInfo("Member name should start with A-Z a-z or national characters", component)
  } else if (component.text.isNotBlank() && !component.text.matches(memberRegex)) {
    ValidationInfo("Member name should contain only A-Z a-z 0-9 or national characters", component)
  } else {
    null
  }
}

/**
 * Validates batch size in int text field. If the value is 0 user should be
 * warned that in this case all files will be fetched together.
 * @param component the component to check - should be registered in UI like an int text field
 *                                           (validation on a digit larger than 0 should be included).
 */
fun validateBatchSize(component: JTextField): ValidationInfo? {
  return if (component.text.toIntOrNull() == 0)
    ValidationInfo("Setting 0 may lead to performance issues due to elements long fetch processing.").asWarning()
      .withOKEnabled()
  else null
}

/**
 * Validate the TSO session name not to be the same as the existing one
 * @param component the component to check the TSO session name
 * @param ignoreValue the value to ignore during the validation (blank value at the start)
 * @param crudable crudable object to find the TSO session config
 */
fun validateTsoSessionName(component: JTextField, ignoreValue: String? = null, crudable: Crudable): ValidationInfo? {
  val sessionAlreadyExist =
    crudable.find<TSOSessionConfig> {
      ignoreValue != it.name && it.name == component.text
    }.count() > 0
  return if (sessionAlreadyExist) {
    ValidationInfo(
      "You must provide unique TSO session name. TSO session \"${component.text}\" already exists.",
      component
    )
  } else null
}

/**
 * Validate that the connection is selected
 * @param component the combobox component to check the selection
 */
fun validateConnectionSelection(component: ComboBox<*>): ValidationInfo?  {
  return if (component.selectedItem == null) {
    ValidationInfo("You must provide a connection", component)
  } else {
    null
  }
}

/**
 * Validate that the TSO session is selected and contains an existing connection
 * @param component the combobox component to check
 * @param crudable the crudable object to find the connection config
 */
fun validateTsoSessionSelection(component: ComboBox<*>, crudable: Crudable): ValidationInfo? {
  return if (component.selectedItem == null) {
    ValidationInfo("You must provide a TSO session", component)
  } else {
    val tsoSessionConfig = component.selectedItem as TSOSessionConfig
    val connectionConfig = crudable.getByUniqueKey<ConnectionConfig>(tsoSessionConfig.connectionConfigUuid)
    if (connectionConfig == null) {
      ValidationInfo("TSO session must contain a connection", component)
    } else null
  }
}
