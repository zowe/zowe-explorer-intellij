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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager

/**
 * Class for conflict resolve procedure. It contains source and destination files that have a conflict between them.
 * It is also needed to indicate how to properly resolve them. 3 options are possible: skip, overwrite, use new name.
 * @param dataOpsManager DataOpsManager
 * @param sourceFile File to be copied.
 * @param destinationFile Child of file to be copy to that have conflict with source file.
 * @param project Current project
 */
abstract class ComputeConflicts(
  val dataOpsManager: DataOpsManager,
  val sourceFiles: List<VirtualFile>,
  val pasteDestinations: List<VirtualFile>,
  val project: Project?
) {
  /**
   * Finds name conflicts between source and target child files.
   * @param conflicts Conflict pairs (target - source) that could be resolved using any method.
   * @param conflictsThatCannotBeOverwritten Conflict pairs (target - source) that couldn't be resolved
   *                                         using "Overwrite" option.
   * @return List of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  abstract fun showMessageAndResolve(
    conflicts: MutableList<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): MutableList<ConflictResolution>

  /**
   * Finds name conflicts between source and target child files.
   * @return List of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  internal fun computeConflictAndAskForResolution(): List<ConflictResolution> {
    var result = mutableListOf<ConflictResolution>()
    val conflicts = pasteDestinations.map { destFile ->

      val conflictingSources = sourceFiles.filter { source ->
        val nameResolver = dataOpsManager.getNameResolver(source, destFile)
        nameResolver.getConflictingChild(source, sourceFiles, destFile) != null
      }
      val foundConflicts = mutableListOf<Pair<VirtualFile, VirtualFile>>()
      if (conflictingSources.isNotEmpty()) {
        conflictingSources.forEach { foundConflict -> foundConflicts.add(Pair(destFile, foundConflict)) }
      }
      foundConflicts
    }
      .flatten()
      .toMutableList()

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

  /**
   * Resolve conflicts one by one for case when user select option "Decide for Each".
   * @param conflicts Conflict pairs (target - source) that could be resolved using any method.
   * @param conflictsThatCannotBeOverwritten Conflict pairs (target - source) that couldn't be resolved
   *                                         using "Overwrite" option.
   * @return List of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  internal fun askUserAboutConflictResolution(
    conflicts: List<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): List<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()
    val allConflicts = arrayListOf<Pair<VirtualFile, VirtualFile>>().apply {
      addAll(conflicts)
      addAll(conflictsThatCannotBeOverwritten)
    }

    allConflicts.forEach { conflict ->

      val newName = dataOpsManager.getNameResolver(conflict.second, conflict.first)
        .resolve(conflict.second, sourceFiles, conflict.first)

      val newNameMessage = "If you select option \"Use new name\", the following name will be selected: <b>$newName</b>"

      if (!conflictsThatCannotBeOverwritten.contains(conflict)) {
        // Conflicts between text/binary files.
        val choice = Messages.showDialog(
          project,
          "Cannot move '${conflict.second.name}' to ${conflict.first.name}\n\n$newNameMessage",
          "Name Conflict",
          arrayOf("Skip", "Overwrite", "Use new name"),
          0,
          Messages.getWarningIcon()
        )
        val resolution = ConflictResolution(conflict.second, conflict.first)
        when (choice) {
          0 -> resolution.resolveBySkip()
          1 -> resolution.resolveByOverwrite()
          2 -> resolution.resolveByUsingNewName(newName)
          else -> throw Exception("Selected option is not supported.")
        }
        result.add(resolution)
      } else {
        // Conflicts between text/binary files and directories.
        val messageToShow = if (conflict.first == conflict.second.parent) {
          "The file '${conflict.second.name}' cannot replace itself"
        } else if (conflict.second.isDirectory) {
          "Directory '${conflict.second.name}' cannot replace file '${conflict.second.name}'"
        } else {
          "File '${conflict.second.name}' cannot replace directory '${conflict.second.name}'"
        }
        val choice = Messages.showDialog(
          project,
          "$messageToShow\n\n$newNameMessage",
          "Name Conflict",
          arrayOf("Skip", "Use new name"),
          0,
          Messages.getWarningIcon()
        )
        val resolution = ConflictResolution(conflict.second, conflict.first)
        when (choice) {
          0 -> resolution.resolveBySkip()
          1 -> resolution.resolveByUsingNewName(newName)
          else -> throw Exception("Selected option is not supported.")
        }
        result.add(resolution)
      }
    }

    return result
  }

  companion object {
    /**
     * Creates an HTML message from an items list
     * Message structure:
     *   startMessage
     *     Next, a string is constructed from the items list.
     *     Elements are added to the string until its length does not exceed the limit.
     *     If not all elements were added to the string, then "and more..." is added to the end of the string
     *   finishMessage.
     * @param startMessage beginning of the message
     * @param items list of items to display
     * @param finishMessage end of message
     * @param limit the maximum allowed length for a converted list of elements.
     * @return created HTML message
     */
    internal fun createHtmlMessageWithItemsList(
      startMessage: String, items: List<String>, finishMessage: String, limit: Int = 130
    ): String {
      val pTag = "<p style=\"margin-left: 10px\">"
      val itemsMerged = items.joinToString(", ")
      val result = if (itemsMerged.length > limit)
        itemsMerged.substring(0, limit - 3).plus("...</p>${pTag}and more...")
      else
        itemsMerged
      val itemsString = pTag.plus(result).plus("</p>")
      return "<html><span>$startMessage\n</span>\n$itemsString\n<span>$finishMessage</span></html>"
    }
  }
}
