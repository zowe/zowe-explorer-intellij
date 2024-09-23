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

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes

/**
 * Data class which represents a rename operation.
 * @param file represents a virtual file on which rename will be performed.
 * @param attributes represents a file attributes of the given virtual file.
 * @param newName a new name of the file in VFS.
 * @param requester a requester for this operation
 */
data class RenameOperation(
  val file: VirtualFile,
  val attributes: FileAttributes,
  val newName: String,
  val requester: AnAction? = null
) : UnitOperation
