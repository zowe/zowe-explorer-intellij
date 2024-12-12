/*
 * Copyright (c) 2020-2025 IBA Group.
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

package org.zowe.explorer.v3.operations.move.conflict

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.explorer.ui.ConflictResolution

/**
 * Class for conflict resolve procedure. It contains source and destination files that have a conflict between them.
 * It is also needed to indicate how to properly resolve them. 3 options are possible: skip, overwrite, use new name.
 * @param sourceFiles list of the source files.
 * @param pasteDestinations list of the destination files.
 * @param project current project
 */
abstract class ConflictResolver(
  protected val sourceFiles: List<VirtualFile>,
  protected val pasteDestinations: List<VirtualFile>,
  protected val project: Project?
) {

  val dataOpsManager = DataOpsManager.getService()

  /**
   * Finds name conflicts between source and target child files.
   * @return list of conflict pairs (target - source).
   */
  abstract fun getConflicts(): List<Pair<VirtualFile, VirtualFile>>

  /**
   * Shows a dialog in which conflicts need to be resolved.
   * @param conflicts conflict pairs (target - source) that could be resolved using any method.
   * @param conflictsThatCannotBeOverwritten conflict pairs (target - source) that couldn't be resolved
   *                                         using "Overwrite" option.
   * @return list of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  abstract fun showMessageAndResolve(
    conflicts: MutableList<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): List<ConflictResolution>

  /**
   * Computes conflicts and asks how to resolve them.
   * @return list of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  fun computeConflictsAndAskForResolution(): List<ConflictResolution> {
    var result = listOf<ConflictResolution>()
    val conflicts = getConflicts().toMutableList()

    // Handle conflicts with different file type (file - directory, directory - file)

    val conflictsThatCannotBeOverwritten = conflicts.filter {
      val conflictChild = it.first.findChild(it.second.name)
      (conflictChild?.isDirectory == true && !it.second.isDirectory)
        || (conflictChild?.isDirectory == false && it.second.isDirectory)
        || it.first == it.second.parent
    }
    conflicts.removeAll(conflictsThatCannotBeOverwritten)

    if (conflicts.isNotEmpty() || conflictsThatCannotBeOverwritten.isNotEmpty()) {
      result = showMessageAndResolve(conflicts, conflictsThatCannotBeOverwritten)
    }

    return result
  }

}
