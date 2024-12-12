/*
 * Copyright (c) 2024-2025 IBA Group.
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

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.explorer.ui.ConflictResolution

private const val MAX_NUMBER_OF_LINES = 5;
private const val DEFAULT_NAME = "DEFNAME"

/**
 * Conflict resolver for conflicts between source files.
 */
class SourceConflictResolver(
  sourceFiles: List<VirtualFile>,
  pasteDestinations: List<VirtualFile>,
  project: Project?
) : ConflictResolver(sourceFiles, pasteDestinations, project) {

  override fun getConflicts(): List<Pair<VirtualFile, VirtualFile>> {
    return pasteDestinations.map { destFile ->
      sourceFiles.filter { source ->
        dataOpsManager.getNameResolver(source, destFile)
          .hasConflictingSource(source, sourceFiles)
      }.map { source ->
        Pair(destFile, source)
      }
    }
      .flatten()
  }

  override fun showMessageAndResolve(
    conflicts: MutableList<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>,
  ): List<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()

    pasteDestinations.forEach { destination ->
      val renameMap = mutableMapOf<VirtualFile, String>()

      val conflictSources = conflicts.filter { it.first == destination }.map { it.second }
      conflictSources.forEach { source ->
        val newName = dataOpsManager.getNameResolver(source, destination)
          .resolveSources(source, conflictSources)
        renameMap[source] = newName
      }

      val sourceDirToRenameMap = mutableMapOf<VirtualFile, MutableMap<VirtualFile, String>>()
      renameMap.forEach { (source, newName) ->
        sourceDirToRenameMap.getOrPut(source.parent) { mutableMapOf() }[source] = newName
      }
      val multipleSourceDirs = sourceDirToRenameMap.size > 1

      var numberOfLines = 0
      var messageTextConflictingFiles = ""
      var showRenameMessage = false
      sourceDirToRenameMap.values.forEachIndexed { index, mutableMap ->
        mutableMap.forEach { (source, newName) ->
          if (numberOfLines < MAX_NUMBER_OF_LINES) {
            if (multipleSourceDirs && mutableMap.keys.firstOrNull() == source) {
              messageTextConflictingFiles += "For the ${index + 1} source path:\n"
              numberOfLines++
            }
            messageTextConflictingFiles += "&emsp;${source.name} -> $newName\n"
            showRenameMessage = true
          }
          numberOfLines++
        }
      }
      if (numberOfLines > MAX_NUMBER_OF_LINES)
        messageTextConflictingFiles += "&emsp;etc..."

      if (showRenameMessage) {
        if (Messages.showOkCancelDialog(
            project,
            "There are some conflicts in the names of the source files for the destination '${destination.name}'.\n" +
              "The source items will be changed to:\n\n" +
              "${messageTextConflictingFiles}\n",
            "Name issues for $numberOfLines file(s)",
            "Ok",
            "Cancel",
            AllIcons.General.QuestionDialog
          ) == Messages.OK
        ) {
          val conflictResolutions = conflicts.filter { it.first == destination }.map {
            val newName = renameMap[it.second] ?: DEFAULT_NAME
            val resolution = ConflictResolution(it.second, it.first)
            resolution.resolveByUsingNewName(newName)
            resolution
          }
          result.addAll(conflictResolutions)
        } else throw Exception("Selected option is not supported.")
      }
    }
    return result
  }

}
