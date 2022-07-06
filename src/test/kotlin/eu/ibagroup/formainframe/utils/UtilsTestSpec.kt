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

import io.kotest.core.spec.style.ShouldSpec

class UtilsTestSpec : ShouldSpec({
  context("utils module: validationFunctions") {
    // validateForBlank(text: String, component: JComponent)
    should("check that text is not blank") {}
    should("check that text is blank and the validation info object is returned") {}
    // validateForBlank(component: JTextField)
    should("check that text is not blank in a component") {}
    should("check that text is blank in a component and validation info object is returned") {}
    // validateConnectionName
    should("validate connection name when there are no other connections") {}
    should("validate connection name when there are other connections and the name is unique") {}
    should("validate connection name when there are other connections and the name is not unique") {}
    // validateWorkingSetName
    should("validate working set name when there are no other working sets") {}
    should("validate working set name when there are other working sets and the name is unique") {}
    should("validate working set name when there are other working sets and the name is not unique") {}
    // validateWorkingSetMaskName
    should("validate working set mask name when there are no other working set masks") {}
    should("validate working set mask name when there are other working set masks and the mask is unique") {}
    should("validate working set mask name when there are other working set masks and the mask is not unique") {}
    // validateZosmfUrl
    should("validate correct URL") {}
    should("validate wrong URL") {}
    // validateFieldWithLengthRestriction
    should("validate that text does not exceed the specified length") {}
    should("validate that text exceeds the specified length") {}
    // validateDatasetMask
    should("validate that long dataset mask matches all the rules") {}
    should("validate that short dataset mask matches all the rules") {}
    should("validate that dataset mask does not match the 44 characters long rule") {}
    should("validate that dataset mask does not match the qualifier 1 to 8 rule") {}
    should("validate that dataset mask does not match the blank rule") {}
    should("validate that dataset mask does not match the regular expression rule") {}
    should("validate that dataset mask does not match the asterisks rule") {}
    // validateUssMask
    should("validate that USS path when it is valid") {}
    should("validate that USS path when it is blank") {}
    should("validate that USS path when it does not match the regular expression") {}
    // validateUssFileName
    should("validate that USS file name when it is valid") {}
    should("validate that USS path when it exceeds the 255 characters long rule") {}
    should("validate that USS path when it contains the forbidden symbol") {}
    // validateJobFilter(prefix: String, owner: String, jobId: String, ws: JesWorkingSet, component: JBTextField, isJobId: Boolean)
    should("validate that the job filter is already exist") {}
    should("validate that the job filter is already exist") {}
    // validateJobFilter(prefix: String, owner: String, jobId: String, component: JBTextField, isJobId: Boolean)
    should("validate that the job filter does not provide owner but provides prefix") {}
    should("validate that the job filter does not provide prefix but provides owner") {}
    should("validate that the job filter does not provide prefix but provides owner") {}
    should("validate that the job filter provides only the blank fields") {}
    // validatePrefixAndOwner
    should("validate that the job owner and prefix are provided and the fields does not match the regular expression") {}
    should("validate that the job owner and prefix are provided and the fields are exceed the 8 characters rule") {}
    should("validate that the job owner and prefix are provided and they are valid") {}
    // validateJobId
    should("validate that the job ID is provided and it exceeds the 8 characters length") {}
    should("validate that the job ID is provided and it does not match the regular expression") {}
    should("validate that the job ID is provided and it is valid") {}
    // validateUssFileNameAlreadyExists
    should("validate that the USS file name is not already exist") {}
    should("validate that the USS file name is already exist") {}
    should("validate that the USS directory name is already exist") {}
    // validateDataset
    should("validate that the provided dataset parameters are valid") {}
    // validateDatasetNameOnInput
    should("check that the dataset name is valid") {}
    should("validate that the dataset name exceeds the 44 characters length rule") {}
    should("validate that the dataset name exceeds the 8 characters per HLQ rule") {}
    should("validate that the dataset name does not match the regular expression") {}
    should("validate the dataset name is blank") {}
    // validateVolser
    should("validate the VOLSER does not match the regular expression")
    should("check the VOLSER is valid")
    // validateForGreaterValue
    should("validate that the number is greater than the provided one") {}
    should("validate that the number is not greater than the provided one") {}
    // validateMemberName
    should("validate that the dataset member name exceeds the 8 character length rule") {}
    should("validate that the dataset member name does not match the dataset member name first letter regular expression") {}
    should("validate that the dataset member name does not match the dataset member name regular expression") {}
  }
  context("utils module: retrofitUtils") {
    // cancelByIndicator
    should("cancel the call on the progress indicator finish") {}
  }
  context("utils module: miscUtils") {
    // debounce
    should("run a block of code after the debounce action") {}
  }
})