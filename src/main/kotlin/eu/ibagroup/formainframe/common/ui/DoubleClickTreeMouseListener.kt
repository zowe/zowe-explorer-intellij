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

package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.util.EditSourceOnDoubleClickHandler
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.TreePath

/**
 * Class which represents a double click event on unique tree object in explorer
 */
class DoubleClickTreeMouseListener(
  private val tree: JTree,
  private val onDoubleClick: JTree.(TreePath) -> Unit
) : EditSourceOnDoubleClickHandler.TreeMouseListener(tree) {

  /**
   * Overloaded method to process double click event. Always called when double click is performed on unique tree object
   * @param e - mouse event
   * @param dataContext - data context
   * @param treePath - unique tree path to the desired object
   * @return Void
   */
  override fun processDoubleClick(e: MouseEvent, dataContext: DataContext, treePath: TreePath) {
    super.processDoubleClick(e, dataContext, treePath)
    onDoubleClick(tree, treePath)
  }

}
