/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.editor

import io.kotest.core.spec.style.ShouldSpec

class EditorTestSpec : ShouldSpec({
  context("editor module: FileEditorEventsListener") {
    // beforeFileClosed
    should("perform file sync before it is closed") {}
    should("not perform file sync before it is closed as it is already synced") {}
  }
})
