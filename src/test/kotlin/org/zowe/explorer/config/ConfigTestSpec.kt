/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import io.kotest.core.spec.style.ShouldSpec

class ConfigTestSpec : ShouldSpec({
  context("config module: xmlUtils") {
    // get
    should("get XML child element by tag") {}
    // toElementList
    should("convert node list to elements list") {}
  }
  context("config module: ConfigSandboxImpl") {
    // apply
    should("apply changes of the config sandbox") {}
    // rollback
    should("rollback all the changes of the config sandbox") {}
    // isModified
    should("check if the sandbox is modified") {}
  }
  context("config module: ws") {
    // WSNameColumn.validateEntered
    should("check that the entered working set name is not empty") {}
    should("check that the entered working set name is not blank") {}
    // jobs/JobsWsDisalog.validateOnApply
    should("check that there are no errors for job filters") {}
    should("check that the error appears on any errors for job filters") {}
    should("check that the error appears on empty jobs working set") {}
    should("check that the error appears on adding the same job filter again") {}
    // files/WorkingSetDialog.validateOnApply
    should("check that there are no errors for file masks") {}
    should("check that the error appears on any errors for file masks") {}
    should("check that the error appears on empty file working set") {}
    should("check that the error appears on adding the same file mask again") {}
  }
})
