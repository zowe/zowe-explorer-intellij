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

import com.intellij.openapi.vfs.VirtualFile

/**
 * Name resolver that generates new name based on the index (e.g. file_(1), file_(2) and etc.)
 * This is an abstract class, it only finds the necessary index but doesn't create a new name.
 * It could be, for example file1, file2, file3. It all depends on implementation needs.
 * @author Valiantsin Krus
 */
abstract class IndexedNameResolver: CopyPasteNameResolver {

  override fun getConflictingChild(source: VirtualFile, sourceFiles: List<VirtualFile>, destination: VirtualFile): VirtualFile? {
    val rowNameToCopy = resolveNameWithIndex(source, destination, null)
    val resolveName = internalResolve(source, destination, buildNewNamesList(source, sourceFiles, destination))
    var ret = destination.children.firstOrNull { it.name == rowNameToCopy }
    if (rowNameToCopy != resolveName && ret == null)
      ret = destination
    return ret
  }

  /**
   * Creates new name for a source file based on passed index.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param destination folder-like entity to copy file to.
   * @param index generated index to add to the source name. If it is null, then no index is needed to add.
   * @return new name with joined index.
   */
  abstract fun resolveNameWithIndex(source: VirtualFile, destination: VirtualFile, index: Int?): String

  /**
   * Creates new names list for a source files list.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param sourceFiles list of all source files to copy
   * @param destination folder-like entity to copy file to.
   * @return new names list
   */
  private fun buildNewNamesList(source: VirtualFile, sourceFiles: List<VirtualFile>, destination: VirtualFile) :List<String>
  {
    val newNames: MutableList<String> = mutableListOf<String>()
    for (s in sourceFiles.take(sourceFiles.indexOf(source))) {
      newNames.add(internalResolve(s, destination, newNames))
    }
    return newNames
  }

  /**
   * Creates new name for source file taking into account the new names of previous files.
   * @param source source file to copy in destination folder (or folder-like entity).
   * @param destination folder-like entity to copy file to.
   * @param newNames list of new file names
   * @return string with new file name.
   */
  private fun internalResolve(source: VirtualFile, destination: VirtualFile, newNames: List<String>): String {
    var newName = resolveNameWithIndex(source, destination, null)
    var index = 1
    while (newName in newNames || destination.children.any { it.name == newName }) {
      newName = resolveNameWithIndex(source, destination, index++)
    }
    return newName
  }

  override fun resolve(source: VirtualFile, sourceFiles: List<VirtualFile>, destination: VirtualFile): String {
    return internalResolve(source, destination,  buildNewNamesList(source, sourceFiles, destination))
  }
}
