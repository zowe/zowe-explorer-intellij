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

class RemoteDatasetComputeConflicts(
  dataOpsManager: DataOpsManager,
  sourceFiles: List<VirtualFile>,
  pasteDestinations: List<VirtualFile>,
  project: Project?
) : ComputeConflicts(
  dataOpsManager,
  sourceFiles,
  pasteDestinations,
  project
){
  override fun showMessageAndResolve(
    conflicts: MutableList<Pair<VirtualFile, VirtualFile>>,
    conflictsThatCannotBeOverwritten: List<Pair<VirtualFile, VirtualFile>>
  ): MutableList<ConflictResolution> {
    val result = mutableListOf<ConflictResolution>()
    var messageTextConflictingFiles = ""
    var showRenameMessage = false
    var nameIssuesCount = 0
    val renameMap = mutableMapOf<VirtualFile, String>()

    // IJMP-1959: Allow conflicts related to copying/moving into the same library
    conflictsThatCannotBeOverwritten.forEach {
      if (it.first == it.second.parent) conflicts.add(it)
    }

    for (sourceFileTmp in sourceFiles) {
      val newNameStr = dataOpsManager.getNameResolver(sourceFileTmp, pasteDestinations[0])
        .resolve(sourceFileTmp, sourceFiles, pasteDestinations[0])
      if (sourceFileTmp.name != newNameStr) {
        renameMap[sourceFileTmp] = newNameStr
      }
    }
    val candidates = HashSet<VirtualFile>()
    for (f in renameMap.keys)
      if (renameMap.values.contains(f.name)) {
        val tmpName = renameMap.getOrDefault(renameMap.filterValues { it == f.name }.keys.first(), "DEFNAME")
        renameMap[renameMap.filterValues { it == f.name }.keys.first()] = renameMap.getOrDefault(f, "DEFNAME")
        renameMap[f] = tmpName
        candidates.add(f)
      }
    val renameMapForMessage: MutableMap<VirtualFile, String> = renameMap.toMutableMap()
    for (candidate in candidates) {
      renameMapForMessage.remove(candidate)
    }
    for (f in renameMapForMessage.keys) {
      if (nameIssuesCount < 5) {
        messageTextConflictingFiles += "&emsp;${f.name} -> ${renameMapForMessage[f]}\n"
        showRenameMessage = true
      }
      nameIssuesCount++
    }
    if (nameIssuesCount > 5)
      messageTextConflictingFiles += "&emsp;etc..."

    if (showRenameMessage) {
      if (Messages.showOkCancelDialog(
          "There are some conflicts for the member names in the destination.\n"
              + "The source items will be changed to:\n\n"
              + "${messageTextConflictingFiles}\n",
          "Name issues for $nameIssuesCount file(s)",
          "Ok",
          "Cancel",
          AllIcons.General.QuestionDialog
        ) == Messages.OK
      ) {
        conflicts.forEach { conflict ->
          val resolution = ConflictResolution(conflict.second, conflict.first)
          val newName = if (conflict.second in renameMap.keys)
            renameMap.getOrDefault(conflict.second, "DEFNAME")
          else
            dataOpsManager.getNameResolver(conflict.second, conflict.first)
              .resolve(conflict.second, sourceFiles, conflict.first)
          resolution.resolveByUsingNewName(newName)
          result.add(resolution)
        }
      } else throw Exception("Selected option is not supported.")
    }
    return result
  }
}
