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

package eu.ibagroup.formainframe.dataops.content.synchronizer

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.utils.runInEdtAndWait

/**
 * Functional interface to decide if file content can be uploaded or should be updated from mainframe.
 * @author Valiantsin Krus
 */
@FunctionalInterface
fun interface SaveStrategy {

  companion object {

    /**
     * Request user permission when the remote content is different from the last fetched content bytes.
     * Should return true when user wants to save the content from file to the mainframe
     * @param project the project to issue dialog in
     * @param file the file to display its name
     * @param shouldUpload is the current bytes should be uploaded to the mainframe in case if the last fetched bytes are the same as the remote bytes
     * @param remoteLastSame is the last fetched bytes are the same as the remote bytes
     * @return boolean that indicates, should the current local file bytes be uploaded to the mainframe
     */
    private fun requestPermissionToUploadOnDiff(
      project: Project?,
      file: VirtualFile,
      shouldUpload: Boolean = true,
      remoteLastSame: Boolean
    ): Boolean {
      return if (!remoteLastSame) {
        var result = shouldUpload
        runInEdtAndWait {
          result = showYesNoDialog(
            title = "Remote Conflict in File ${file.name}",
            message = "The file you are currently editing was changed on remote. Do you want to accept remote changes and discard local ones, or overwrite content on the mainframe by local version?",
            noText = "Accept Remote",
            yesText = "Overwrite Content on the Mainframe",
            project = project,
            icon = AllIcons.General.WarningDialog
          )
        }
        result
      } else {
        shouldUpload
      }
    }

    /**
     * Creates a default save strategy with "yes/no" dialog when the last fetched bytes are different from the remote bytes.
     * It uploads changes in case the current bytes are different from the remote bytes
     * @param project project instance to show dialog in
     * @return instance of default [SaveStrategy]
     */
    fun default(project: Project? = null): SaveStrategy {
      return SaveStrategy { f, lastSuccessfulState, remoteBytes ->
        requestPermissionToUploadOnDiff(project, f, true, (lastSuccessfulState contentEquals remoteBytes))
      }
    }

    /**
     * Creates a default save strategy with "yes/no" dialog when the last fetched bytes are different from the remote bytes.
     * It uploads changes in case the current bytes are different from the remote bytes
     * @param project project instance to show dialog in
     * @return instance of default [SaveStrategy]
     */
    fun syncOnOpen(project: Project? = null): SaveStrategy {
      return SaveStrategy { f, lastSuccessfulState, remoteBytes ->
        requestPermissionToUploadOnDiff(
          project,
          f,
          false,
          (lastSuccessfulState contentEquals remoteBytes)
        )
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
