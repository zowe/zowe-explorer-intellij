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

package eu.ibagroup.formainframe.explorer.actions

import eu.ibagroup.formainframe.explorer.ui.CreateFileDialogState
import eu.ibagroup.formainframe.explorer.ui.emptyDirState

/** Action to create USS directory. Directory will be empty when created */
class CreateUssDirectoryAction : CreateUssEntityAction() {

  override val fileType: CreateFileDialogState
    get() = emptyDirState

  override val ussFileType: String
    get() = "USS directory"
}
