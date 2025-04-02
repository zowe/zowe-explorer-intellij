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

package org.zowe.explorer.dataops.content.synchronizer

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.utils.runInEdtAndWait
import java.nio.charset.StandardCharsets

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
          val choice = Messages.showDialog(
            project,
            "The file you are currently editing was changed on remote. Do you want to accept remote changes and discard local ones, or overwrite content on the mainframe by local version?",
            "Remote Conflict in File ${file.name}",
            arrayOf("Compare And Decide", "Accept Remote", "Overwrite Content on the Mainframe"),
            0, // Default is now "Compare And Decide"
            AllIcons.General.WarningDialog
          )
          
          when (choice) {
            0 -> { // Compare And Decide
              // Get file content for comparison
              val dataOpsManager = DataOpsManager.getService()
              val contentSynchronizer = dataOpsManager.getContentSynchronizer(file)
              if (contentSynchronizer != null) {
                // Create a sync provider to get content
                val syncProvider = DocumentedSyncProvider(file, SaveStrategy.default(project))
                
                // Use the current file content as local content
                val localBytes = file.contentsToByteArray()
                
                // Get remote content from the stored remote state
                val remoteBytes = contentSynchronizer.successfulContentStorage(syncProvider)
                if (remoteBytes.isNotEmpty()) {
                  showFileComparison(project, file, localBytes, remoteBytes)
                } else {
                  Messages.showErrorDialog(
                    project,
                    "Cannot compare files: remote content unavailable",
                    "Comparison Error"
                  )
                }
              } else {
                Messages.showErrorDialog(
                  project,
                  "Cannot compare files: content synchronizer not available",
                  "Comparison Error"
                )
              }
              // By default, after comparison we'll preserve local changes
              result = shouldUpload
            }
            1 -> { // Accept Remote (equivalent to previous "No")
              result = false
            }
            2 -> { // Overwrite Content on the Mainframe (equivalent to previous "Yes")
              result = true
            }
            else -> {
              result = shouldUpload
            }
          }
        }
        result
      } else {
        shouldUpload
      }
    }
    
    /**
     * Shows a comparison dialog between local and remote versions of a file
     * @param project the project to show the dialog in
     * @param file the file to compare
     * @param localContent the current local content
     * @param remoteContent the current remote content
     */
    private fun showFileComparison(
      project: Project?,
      file: VirtualFile,
      localContent: ByteArray,
      remoteContent: ByteArray
    ) {
      val contentFactory = DiffContentFactory.getInstance()
      
      // For local content, we'll use the file directly
      val localDiffContent = contentFactory.create(project, file)
      
      // For remote content, create from bytes
      // The correct parameter order is: project, byteContent, file
      val remoteDiffContent = contentFactory.createFromBytes(
        project,
        remoteContent, // The byte array content (this should be the second parameter)
        file // The file for context (this should be the third parameter)
      )
      
      // Create the diff request
      val diffRequest = SimpleDiffRequest(
        "Comparing Local and Remote Versions of ${file.name}",
        localDiffContent,
        remoteDiffContent,
        "Local Version (Current)",
        "Remote Version"
      )
      
      // Show the diff
      DiffManager.getInstance().showDiff(project, diffRequest)
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
