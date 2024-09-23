/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import eu.ibagroup.formainframe.explorer.ExplorerViewSettings

/** Base explorer tree structure provider. Allows to implement explorers as in the project tree view */
abstract class ExplorerTreeStructureProvider : TreeStructureProvider {

  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings?
  ): MutableCollection<AbstractTreeNode<*>> {
    if (parent is ExplorerTreeNode<*, *> && settings is ExplorerViewSettings) {
      val castedChildren = children.filterIsInstance<ExplorerTreeNode<*, *>>()
      if (castedChildren.size == children.size) {
        @Suppress("UNCHECKED_CAST")
        return modifyOurs(parent, castedChildren, settings) as MutableCollection<AbstractTreeNode<*>>
      }
    }
    return children
  }

  protected abstract fun modifyOurs(
    parent: ExplorerTreeNode<*, *>,
    children: Collection<ExplorerTreeNode<*, *>>,
    settings: ExplorerViewSettings
  ): MutableCollection<ExplorerTreeNode<*, *>>

}
