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

package eu.ibagroup.formainframe.explorer

import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import io.mockk.clearAllMocks

class ExplorerTestSpec : WithApplicationShouldSpec({
  afterSpec {
    clearAllMocks()
  }

  context("explorer module: ui/ExplorerPasteProvider") {
    // performPaste
    should("perform paste without conflicts") {}
    should("perform paste accepting conflicts") {}
    should("perform paste declining conflicts") {}
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    // actionPerformed
    should("get dataset properties") {}
    should("get dataset member properties") {}
    should("get USS file properties") {}
  }
  context("explorer module: actions/ChangeContentModeAction") {
    // isSelected
    should("check if the 'Use binary mode' selected for a file") {}
    // setSelected
    should("select 'Use binary mode' for a file") {}
    should("unselect 'Use binary mode' for a file") {}
  }
  context("explorer module: actions/AddMemberAction") {
    // actionPerformed
    should("perform dataset member allocate with default parameters") {}
  }
})
