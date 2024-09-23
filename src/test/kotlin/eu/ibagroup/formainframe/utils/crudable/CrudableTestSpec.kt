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

package eu.ibagroup.formainframe.utils.crudable

import io.kotest.core.spec.style.ShouldSpec

class CrudableTestSpec : ShouldSpec({
  context("crudable module: Crudable") {
    // replaceGracefully
    should("replace the old rows with the new ones") {}
    // getByColumnValue
    should("get field from a row by its value") {}
    // getByUniqueKey
    should("get field by unique key") {}
    // deleteByUniqueKey
    should("delete field by unique key") {}
    // getUniqueValueForRow
    should("get unique value for row") {}
  }
})
