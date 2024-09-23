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
package eu.ibagroup.formainframe.dataops.operations.mover.names

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsComponentFactory

interface CopyPasteNameResolverFactory: DataOpsComponentFactory<CopyPasteNameResolver>

/**
 * Class to represent a name resolution for conflicting situation.
 * @author Valiantsin Krus
 */
interface CopyPasteNameResolver {
  companion object {
    @JvmField
    val EP = ExtensionPointName.create<CopyPasteNameResolverFactory>("eu.ibagroup.formainframe.nameResolver")
  }

  /**
   * Determines whether this name resolver could resolve conflict for passed files or not.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param destination folder-like entity to copy file to.
   * @return true if this name resolver could dot it or false otherwise.
   */
  fun accepts(source: VirtualFile, destination: VirtualFile): Boolean

  /**
   * Finds child in destination folder that conflicts with source file.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param sourceFiles list of all source files to copy.
   * @param destination folder-like entity to copy file to.
   * @return instance of conflicting child file or null if it was not found.
   */
  fun getConflictingChild(source: VirtualFile, sourceFiles: List<VirtualFile>, destination: VirtualFile): VirtualFile?

  /**
   * Creates new name for source file to make it possible to be copied in destination folder.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param sourceFiles list of all source files to copy
   * @param destination folder-like entity to copy file to.
   * @return string with new file name.
   */
  fun resolve(source: VirtualFile, sourceFiles: List<VirtualFile>, destination: VirtualFile): String
}
