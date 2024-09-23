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

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.ui.WindowsLikeMessageDialog

class CommonComputeConflicts(
  dataOpsManager: DataOpsManager,
  sourceFiles: List<VirtualFile>,
  pasteDestinations: List<VirtualFile>,
  project: Project?
) : ComputeConflicts(
  dataOpsManager,
  sourceFiles,
  pasteDestinations,
  project
) {
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
}
