/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.explorer.Explorer

private val singletonList = mutableListOf<AbstractTreeNode<*>>()
private val any = Any()

/**
 * Specific information node, that is not related to any mainframe nodes.
 * Represents the basic interactions with it, when there is a warning, error or some other information appears in the explorer tree
 */
abstract class InfoNodeBase(
  project: Project,
  parent: ExplorerTreeNode<*>,
  explorer: Explorer<*>,
  treeStructure: ExplorerTreeStructureBase
) :
  ExplorerTreeNode<Any>(any, project, parent, explorer, treeStructure) {

  override fun isAlwaysLeaf(): Boolean {
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.addText(text, textAttributes)
  }

  protected abstract val text: String

  protected abstract val textAttributes: SimpleTextAttributes

  override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
    return singletonList
  }

}
