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

import com.intellij.openapi.extensions.LoadingOrder
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.wm.*
import com.intellij.openapi.wm.impl.status.EncodingPanelWidgetFactory
import com.intellij.openapi.wm.impl.status.LineSeparatorWidgetFactory
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * File selection change listener.
 * Needed to handle change event of the selected file.
 */
class FileSelectionChangeListener: FileEditorManagerListener {

  /**
   * Handles the visibility of the status bar widgets when a file is selected.
   * @param event selected file change event.
   */
  override fun selectionChanged(event: FileEditorManagerEvent) {
    val extensionPoint = StatusBarWidgetFactory.EP_NAME.point
    val project = event.manager.project
    val statusBar = WindowManager.getInstance().getStatusBar(project)
    val file = event.newFile
    if (file is MFVirtualFile) {
      extensionPoint.extensionList.filter {
        it.id == StatusBar.StandardWidgets.ENCODING_PANEL || it.id == StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL
      }.forEach {
        extensionPoint.unregisterExtension(it::class.java)
      }
    } else {
      val lineSeparatorWidgetFactories =
        extensionPoint.extensionList.mapNotNull { it.castOrNull<LineSeparatorWidgetFactory>() }
      if (
        extensionPoint.extensionList.none { it.id == StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL } &&
        lineSeparatorWidgetFactories.none { it.canBeEnabledOn(statusBar) }
      ) {
        val widget = statusBar.getWidget(StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL) ?: return
        val factory = LineSeparatorWidgetFactory()
        val order = LoadingOrder.readOrder("after positionWidget")
        extensionPoint.registerExtension(factory, order, widget)
      }
      val encodingPanelWidgetFactories =
        extensionPoint.extensionList.mapNotNull { it.castOrNull<EncodingPanelWidgetFactory>() }
      if (
        extensionPoint.extensionList.none { it.id == StatusBar.StandardWidgets.ENCODING_PANEL } &&
        encodingPanelWidgetFactories.none { it.canBeEnabledOn(statusBar) }
      ) {
        val widget = statusBar.getWidget(StatusBar.StandardWidgets.ENCODING_PANEL) ?: return
        val factory = EncodingPanelWidgetFactory()
        val order = LoadingOrder.readOrder("after lineSeparatorWidget, before powerStatus")
        extensionPoint.registerExtension(factory, order, widget)
      }
    }
  }
}