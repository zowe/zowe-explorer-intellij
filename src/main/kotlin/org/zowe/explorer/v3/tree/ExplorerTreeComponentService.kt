/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.zowe.explorer.v3.components.files.FilesExplorerComponent
import org.zowe.explorer.v3.components.jes.JesExplorerComponent
import org.zowe.explorer.v3.components.tso.TsoSessionsComponent

// TODO: doc
@Service
class ExplorerTreeComponentService {
  companion object {
    fun getService(): ExplorerTreeComponentService = service()
  }

  private val projectsToFilesExplorerComponents = mutableMapOf<Project, FilesExplorerComponent>()
  private val projectsToJesExplorerComponents = mutableMapOf<Project, JesExplorerComponent>()
  private val projectsToTsoSessionsComponents = mutableMapOf<Project, TsoSessionsComponent>()

  fun getFilesExplorerComponent(project: Project): FilesExplorerComponent {
    val savedFilesExplorerComponent = projectsToFilesExplorerComponents[project]
    return if (savedFilesExplorerComponent == null) {
      val filesExplorerComponent = FilesExplorerComponent(project)
      Disposer.register(project, filesExplorerComponent)
      projectsToFilesExplorerComponents[project] = filesExplorerComponent
      filesExplorerComponent
    } else savedFilesExplorerComponent
  }

  fun getJesExplorerComponent(project: Project): JesExplorerComponent {
    val savedJesExplorerComponent = projectsToJesExplorerComponents[project]
    return if (savedJesExplorerComponent == null) {
      val jesExplorerComponent = JesExplorerComponent(project)
      Disposer.register(project, jesExplorerComponent)
      projectsToJesExplorerComponents[project] = jesExplorerComponent
      jesExplorerComponent
    } else savedJesExplorerComponent
  }

  fun getTsoSessionsComponent(project: Project): TsoSessionsComponent {
    val savedTsoSessionsComponent = projectsToTsoSessionsComponents[project]
    return if (savedTsoSessionsComponent == null) {
      val tsoSessionsComponent = TsoSessionsComponent(project)
      Disposer.register(project, tsoSessionsComponent)
      projectsToTsoSessionsComponents[project] = tsoSessionsComponent
      tsoSessionsComponent
    } else savedTsoSessionsComponent
  }
}