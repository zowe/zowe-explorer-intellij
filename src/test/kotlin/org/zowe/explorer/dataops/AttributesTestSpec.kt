/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops

import io.kotest.core.spec.style.ShouldSpec

class AttributesTestSpec : ShouldSpec({
  context("dataops module: attributes") {
    // RemoteUssAttributesService.reassignAttributesAfterUrlFolderRenaming
    should("reassign attributes after folder is renamed") {}
    should("reassign attributes after folder path is changed") {}
    // RemoteUssAttributesService.updateWritableFlagAfterContentChanged
    should("update writable flag when content is binary") {}
    should("update writable flag from provided attributes") {}
  }
})
