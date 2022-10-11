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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetSettings
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * File selection change listener.
 * Needed to handle change event of the selected file.
 */
class FileSelectionChangeListener: FileEditorManagerListener {

  /**
   * Handle dropdown visibility change with file encoding.
   * @param event selected file change event.
   */
  override fun selectionChanged(event: FileEditorManagerEvent) {
    val project = event.manager.project
    val widgetManager = project.service<StatusBarWidgetsManager>()
    val widgetSettings = ApplicationManager.getApplication().getService(StatusBarWidgetSettings::class.java)
    val factory = widgetManager.widgetFactories.find { it.id == StatusBar.StandardWidgets.ENCODING_PANEL }
    factory?.let {
      if (event.newFile is MFVirtualFile) {
        widgetSettings.setEnabled(it, false)
      } else {
        widgetSettings.setEnabled(it, true)
      }
      widgetManager.updateWidget(it)
    }
    super.selectionChanged(event)
  }
}