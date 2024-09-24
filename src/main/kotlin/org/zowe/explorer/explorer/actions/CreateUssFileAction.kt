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

package org.zowe.explorer.explorer.actions

import org.zowe.explorer.explorer.ui.CreateFileDialogState
import org.zowe.explorer.explorer.ui.emptyFileState

/** Action to create USS file. The file will be empty when created */
class CreateUssFileAction : CreateUssEntityAction() {

  override val fileType: CreateFileDialogState
    get() = emptyFileState

  override val ussFileType: String
    get() = "USS file"
}
