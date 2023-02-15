/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.dataops.UnitOperation
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.explorer.Explorer

/**
 * Data class which represents a force rename operation
 * @param file represents a virtual file on which rename will be performed
 * @param attributes represents a file attributes of the given virtual file
 * @param newName a new name of the file in VFS
 * @param override responsible for the file override behavior in VFS
 * @param explorer represents explorer object
 */
data class ForceRenameOperation(
  val file: VirtualFile,
  val attributes: FileAttributes,
  val newName: String,
  val override: Boolean,
  val explorer: Explorer<ConnectionConfig, *>?
) : UnitOperation {
  override fun toString(): String {
    return "ForceRenameOperation(file=$file, attributes=$attributes, newName='$newName', override=$override, explorer=$explorer)"
  }
}
