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

// TODO: doc
@Service
class ExplorerTreeComponentService {
  companion object {
    fun getService(): ExplorerTreeComponentService = service()
  }

  private val projectsToFilesExplorerComponents = mutableMapOf<Project, FilesExplorerComponent>()
  private val projectsToJesExplorerComponents = mutableMapOf<Project, JesExplorerComponent>()

  fun getFilesExplorerComponent(project: Project): FilesExplorerComponent {
    val projectToFilesExplorerComponent = projectsToFilesExplorerComponents[project]
    return if (projectToFilesExplorerComponent == null) {
      val filesExplorerComponent = FilesExplorerComponent(project)
      Disposer.register(project, filesExplorerComponent)
      projectsToFilesExplorerComponents[project] = filesExplorerComponent
      filesExplorerComponent
    } else projectToFilesExplorerComponent
  }

  fun getJesExplorerComponent(project: Project): JesExplorerComponent {
    val projectToJesExplorerComponent = projectsToJesExplorerComponents[project]
    return if (projectToJesExplorerComponent == null) {
      val jesExplorerComponent = JesExplorerComponent(project)
      Disposer.register(project, jesExplorerComponent)
      projectsToJesExplorerComponents[project] = jesExplorerComponent
      jesExplorerComponent
    } else projectToJesExplorerComponent
  }
}