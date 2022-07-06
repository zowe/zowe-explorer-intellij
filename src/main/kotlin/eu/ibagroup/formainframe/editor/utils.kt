/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder

/**
 * Show dialog which prevents user from closing the file without synchronizing
 * @param fileName the name of the file for the dialog message
 * @param project the project on which to issue the dialog
 */
fun showSyncOnCloseDialog(fileName: String, project: Project): Boolean {
  return MessageDialogBuilder
    .yesNo(
      title = "File $fileName Is Not Synced",
      message = "Do you want to sync the file with the Mainframe before it is closed?"
    )
    .asWarning()
    .ask(project = project)
}
