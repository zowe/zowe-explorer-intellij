/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import org.zowe.explorer.v3.impl.files.tree.FilesExplorerComponent
import org.zowe.explorer.v3.impl.files.tree.nodes.FilesExplorerRelated
import org.zowe.explorer.v3.impl.jes.tree.JesExplorerComponent
import org.zowe.explorer.v3.impl.jes.tree.nodes.JesExplorerRelated
import org.zowe.explorer.v3.impl.tso.tree.TsoSessionsComponent
import org.zowe.explorer.v3.impl.vault.tree.SecureVaultComponent
import org.zowe.explorer.v3.impl.vault.tree.nodes.SecureVaultRelated
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode

/**
 * Explorer tree component service to work with explorer trees. It is an application-level service
 * that contains information about the initialized components for respective projects. Provides functionality to handle
 * tree actions application-wide
 */
@Service
class ExplorerTreeComponentService {
  companion object {
    const val TOOL_WINDOW_ID = "Zowe Explorer (v3)"
    fun getService(): ExplorerTreeComponentService = service()
  }

  private val projectsToFilesExplorerComponents = mutableMapOf<Project, FilesExplorerComponent>()
  private val projectsToJesExplorerComponents = mutableMapOf<Project, JesExplorerComponent>()
  private val projectsToTsoSessionsComponents = mutableMapOf<Project, TsoSessionsComponent>()
  private val projectsToSecureVaultComponents = mutableMapOf<Project, SecureVaultComponent>()

  /**
   * Returns the cached component for the [project] or, on first access, constructs it
   * via [factory], registers it for disposal, runs its post-construction initialization
   * and caches it.
   *
   * [ExplorerTreeComponent.postConstruct] is deliberately invoked here rather than from
   * the component's constructor: it relies on abstract members that subclass property
   * initializers only assign after the base constructor has completed
   * @param project the project the component belongs to
   * @param cache the per-component-type cache to look up and store the component in
   * @param factory creates a new component instance when none is cached yet
   */
  private fun <T : ExplorerTreeComponent> getOrCreateComponent(
    project: Project,
    cache: MutableMap<Project, T>,
    factory: (Project) -> T
  ): T {
    return cache.getOrPut(project) {
      val component = factory(project)
      Disposer.register(project, component)
      component.postConstruct()
      component
    }
  }

  fun getFilesExplorerComponent(project: Project): FilesExplorerComponent {
    return getOrCreateComponent(project, projectsToFilesExplorerComponents, ::FilesExplorerComponent)
  }

  fun getJesExplorerComponent(project: Project): JesExplorerComponent {
    return getOrCreateComponent(project, projectsToJesExplorerComponents, ::JesExplorerComponent)
  }

  fun getTsoSessionsComponent(project: Project): TsoSessionsComponent {
    return getOrCreateComponent(project, projectsToTsoSessionsComponents, ::TsoSessionsComponent)
  }

  fun getSecureVaultComponent(project: Project): SecureVaultComponent {
    return getOrCreateComponent(project, projectsToSecureVaultComponents, ::SecureVaultComponent)
  }

  fun getActiveExplorerComponent(project: Project): ExplorerTreeComponent {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
    val selectedDisplayName = toolWindow?.contentManager?.selectedContent?.displayName
    return when (selectedDisplayName) {
      FilesExplorerComponent.FILES_EXPLORER_COMPONENT_NAME -> getFilesExplorerComponent(project)
      JesExplorerComponent.JES_EXPLORER_COMPONENT_NAME -> getJesExplorerComponent(project)
      TsoSessionsComponent.TSO_SESSIONS_COMPONENT_NAME -> getTsoSessionsComponent(project)
      SecureVaultComponent.SECURE_VAULT_COMPONENT_NAME -> getSecureVaultComponent(project)
      else -> throw Exception("There is no focused component in '${TOOL_WINDOW_ID}' for project '${project.name}'")
    }
  }

  fun getExplorerComponentForNode(node: ExplorerTreeNode): ExplorerTreeComponent {
    return when (node.nodeDescriptor) {
      is FilesExplorerRelated -> getFilesExplorerComponent(node.project)
      is JesExplorerRelated -> getJesExplorerComponent(node.project)
      is SecureVaultRelated -> getSecureVaultComponent(node.project)
      else -> throw Exception("Incorrect node to return explorer component for: ${node.javaClass}. Most probably you forgot to associate it with ${FilesExplorerRelated::class.java.simpleName} or ${JesExplorerRelated::class.java.simpleName}")
    }
  }

  /**
   * Invalidate node in the respective project, together with its children nodes
   * @param node the node to invalidate
   * @throws Exception when the node is not related to any of the supported components of the plugin
   */
  fun invalidateNode(node: ExplorerTreeNode) {
    when (node.nodeDescriptor) {
      is FilesExplorerRelated -> {
        val filesExplorerComponent = getFilesExplorerComponent(node.project)
        filesExplorerComponent.invalidateNode(node, true)
      }

      is JesExplorerRelated -> {
        val jesExplorerComponent = getJesExplorerComponent(node.project)
        jesExplorerComponent.invalidateNode(node, true)
      }

      is SecureVaultRelated -> {
        val secureVaultComponent = getSecureVaultComponent(node.project)
        secureVaultComponent.invalidateNode(node, true)
      }

      else -> throw Exception("Invalid node type ${node.javaClass}, impossible to invalidate")
    }
  }
}
