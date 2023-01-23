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

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EncodingPanelWidgetFactory
import com.intellij.openapi.wm.impl.status.LineSeparatorWidgetFactory

/**
 * Project post startup activity.
 */
class ProjectStartupActivity : StartupActivity, DumbAware {

  /**
   * Implementation of [StartupActivity.runActivity].
   * Unregisters widget factories in the status bar that are overridden in the plugin.
   */
  override fun runActivity(project: Project) {
    val extensionPoint = StatusBarWidgetFactory.EP_NAME.point
    extensionPoint.unregisterExtension(EncodingPanelWidgetFactory::class.java)
    extensionPoint.unregisterExtension(LineSeparatorWidgetFactory::class.java)
  }
}