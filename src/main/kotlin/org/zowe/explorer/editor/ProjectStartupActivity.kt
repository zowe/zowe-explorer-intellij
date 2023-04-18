/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory

/**
 * Project post startup activity.
 */
class ProjectStartupActivity: StartupActivity.DumbAware {

  /**
   * Implementation of [StartupActivity.runActivity].
   * Unregisters widget factories that provide status bar widgets that are overridden in the plugin.
   */
  override fun runActivity(project: Project) {
    val extensionPoint = StatusBarWidgetFactory.EP_NAME.point
    extensionPoint.extensionList.filter {
      it.id == StatusBar.StandardWidgets.ENCODING_PANEL || it.id == StatusBar.StandardWidgets.LINE_SEPARATOR_PANEL
    }.forEach {
      extensionPoint.unregisterExtension(it::class.java)
    }
  }
}