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

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EncodingPanelWidgetFactory
import com.intellij.openapi.wm.impl.status.LineSeparatorWidgetFactory

/**
 * Project open event listener.
 */
class ProjectOpenListener : ProjectManagerListener {

  /**
   * Implementation of [ProjectManagerListener.projectOpened].
   * Unregisters widget factories in the status bar that are overridden in the plugin.
   * @see ProjectManagerListener.projectOpened
   */
  override fun projectOpened(project: Project) {
    val extensionPoint = StatusBarWidgetFactory.EP_NAME.point
    invokeLater {
      extensionPoint.unregisterExtension(EncodingPanelWidgetFactory::class.java)
      extensionPoint.unregisterExtension(LineSeparatorWidgetFactory::class.java)
    }
    super.projectOpened(project)
  }
}