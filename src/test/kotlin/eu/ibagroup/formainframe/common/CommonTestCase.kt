/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.common

import io.kotest.core.spec.style.ShouldSpec

class CommonTestCase : ShouldSpec({
  context("common module: ui") {
    // ValidatingCellRenderer.getTableCellRendererComponent
    should("get table cell renderer") {}
    // ValidatingCellEditor.getTableCellEditorComponent
    should("get table cell editor") {}
    // treeUtils.makeNodeDataFromTreePath
    should("make node data from tree path") {}
    // treeUtils.getVirtualFile
    should("get virtual file from tree path") {}
    should("not get virtual file from tree path if it cannot be casted") {}
    // StatefulDialog.showUntilDone
    should("show dialog until it is fulfilled") {}
  }
})
