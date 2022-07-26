/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile

/**
 * Functional interface to decide if file content can be uploaded or should be updated from mainframe.
 * @author Valiantsin Krus
 */
@FunctionalInterface
fun interface SaveStrategy {

  companion object {
    /**
     * Creates a default save strategy with "yes/no" dialog
     * @param project project instance to show dialog in.
     * @return instance of default [SaveStrategy]
     */
    fun default(project: Project? = null): SaveStrategy {
      return SaveStrategy { f, lastSuccessfulState, remoteBytes ->
        (lastSuccessfulState contentEquals remoteBytes).let decision@{ result ->
          return@decision if (!result) {
            invokeAndWaitIfNeeded {
              showYesNoDialog(
                title = "Remote Conflict in File ${f.name}",
                message = "The file you are currently editing was changed on remote. Do you want to accept remote changes and discard local ones, or overwrite content on the mainframe by local version?",
                noText = "Accept Remote",
                yesText = "Overwrite Content on the Mainframe",
                project = project,
                icon = AllIcons.General.WarningDialog
              )
            }
          } else {
            true
          }
        }
      }
    }
  }

  /**
   * Checks either file content can be uploaded or should be updated from mainframe.
   * @param file virtual file to check if content can be uploaded
   * @param lastSuccessfulState previously fetched file content from mainframe.
   * @param currentRemoteState currently fetched file content from mainframe.
   * @return true if file content should be uploaded to mainframe or false if file content should be updated from mainframe.
   */
  fun decide(
    file: VirtualFile,
    lastSuccessfulState: ByteArray,
    currentRemoteState: ByteArray
  ): Boolean
}
