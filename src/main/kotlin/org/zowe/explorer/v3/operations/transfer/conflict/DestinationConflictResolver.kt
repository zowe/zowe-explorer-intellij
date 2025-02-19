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

package org.zowe.explorer.v3.operations.transfer.conflict

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.explorer.ui.ConflictResolution
import org.zowe.explorer.utils.ui.WindowsLikeMessageDialog

/**
 * Conflict resolver for conflicts between source files and files in the destination.
 * @param sourceConflictResolutions list of [ConflictResolution] that contains resolved conflicts for source files
 */
class DestinationConflictResolver(
  sourceFiles: List<VirtualFile>,
  pasteDestinations: List<VirtualFile>,
  private val sourceConflictResolutions: List<ConflictResolution>,
  project: Project?
) : ConflictResolver(
  sourceFiles,
  pasteDestinations,
  project
) {

  override fun getConflicts(): List<Pair<VirtualFile, VirtualFile>> {
    return pasteDestinations.map { destFile ->
      sourceFiles.filter { source ->
        dataOpsManager.getNameResolver(source, destFile)
          .getConflictingChild(source, sourceFiles, destFile) != null
      }.map {
        Pair(destFile, it)
      }
    }
      .flatten()
  }

  override fun showMessageAndResolve(
    conflicts: MutableList<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): MutableList<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()
    val choice = WindowsLikeMessageDialog.showWindowsLikeMessageDialog(
      project = project,
      message = "The destination already has file(s) with\nthe same name.\n" +
        "Please, select an action.",
      title = "Name conflicts in ${conflicts.size + conflictsThatCannotBeOverwritten.size} file(s)",
      options = arrayOf(
        "Skip the conflicting file(s)",
        "Replace the file(s) in the destination",
        "Decide for each file"
      ),
      defaultOptionIndex = 0,
      focusedOptionIndex = 0
    )

    when (choice) {
      0 -> {
        result.addAll(conflicts.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } })
        result.addAll(
          conflictsThatCannotBeOverwritten.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } }
        )
      }

      1 -> {
        result.addAll(conflicts.map { ConflictResolution(it.second, it.first).apply { resolveByOverwrite() } })
        result.addAll(
          conflictsThatCannotBeOverwritten.map { ConflictResolution(it.second, it.first).apply { resolveBySkip() } }
        )
        if (conflictsThatCannotBeOverwritten.isNotEmpty()) {
          val startMessage = "There are some conflicts that cannot be resolved:"
          val finishMessage = "File(s) above will be skipped."
          val conflictsToShow = conflictsThatCannotBeOverwritten.map {
            if (it.first == it.second.parent) {
              "The file '${it.second.name}' cannot overwrite itself"
            } else if (it.second.isDirectory) {
              "Directory '${it.second.name}' cannot replace file '${it.second.name}'"
            } else {
              "File '${it.second.name}' cannot replace directory '${it.second.name}'"
            }
          }
          Messages.showDialog(
            project,
            createHtmlMessageWithItemsList(startMessage, conflictsToShow, finishMessage),
            "Not Resolvable Conflicts",
            arrayOf("Ok"),
            0,
            Messages.getErrorIcon()
          )
        }
      }

      2 -> result.addAll(
        askUserAboutConflictResolution(
          conflicts,
          conflictsThatCannotBeOverwritten
        )
      )

      else -> throw Exception("Selected option is not supported.")
    }
    return result
  }

  /**
   * Resolve conflicts one by one for case when user select option "Decide for Each".
   * @param conflicts conflict pairs (target - source) that could be resolved using any method.
   * @param conflictsThatCannotBeOverwritten conflict pairs (target - source) that couldn't be resolved
   *                                         using "Overwrite" option.
   * @return list of [ConflictResolution] that indicates list of conflicts and how to resolve them.
   */
  private fun askUserAboutConflictResolution(
    conflicts: List<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): List<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()
    val allConflicts = arrayListOf<Pair<VirtualFile, VirtualFile>>().apply {
      addAll(conflicts)
      addAll(conflictsThatCannotBeOverwritten)
    }

    // The map contains a list of the new names resolved for the destination
    val destinationToNewNamesMap = mutableMapOf<VirtualFile, MutableList<String>>()
    sourceConflictResolutions.forEach { resolution ->
      resolution.newName?.let { newName ->
        destinationToNewNamesMap.getOrPut(resolution.destinationFile) { mutableListOf() }.add(newName)
      }
    }

    allConflicts.forEach { conflict ->

      val newNames = destinationToNewNamesMap[conflict.first]?.toList() ?: emptyList()
      val newName = dataOpsManager.getNameResolver(conflict.second, conflict.first)
        .resolve(conflict.second, sourceFiles, conflict.first, newNames)

      val newNameMessage = "If you select option \"Use new name\", the following name will be selected: <b>$newName</b>"

      val resolution: ConflictResolution
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
        resolution = ConflictResolution(conflict.second, conflict.first)
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
        resolution = ConflictResolution(conflict.second, conflict.first)
        when (choice) {
          0 -> resolution.resolveBySkip()
          1 -> resolution.resolveByUsingNewName(newName)
          else -> throw Exception("Selected option is not supported.")
        }
        result.add(resolution)
      }
      if (resolution.shouldUseNewName()) {
        destinationToNewNamesMap.getOrPut(conflict.first) { mutableListOf() }.add(newName)
      }
    }

    return result
  }

}
