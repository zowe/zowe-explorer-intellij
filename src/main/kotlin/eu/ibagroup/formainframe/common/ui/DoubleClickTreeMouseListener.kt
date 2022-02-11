/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.common.ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.util.EditSourceOnDoubleClickHandler
import java.awt.event.MouseEvent
import javax.swing.JTree
import javax.swing.tree.TreePath

class DoubleClickTreeMouseListener(
  private val tree: JTree,
  private val onDoubleClick: JTree.(TreePath) -> Unit
) : EditSourceOnDoubleClickHandler.TreeMouseListener(tree) {

  override fun processDoubleClick(e: MouseEvent, dataContext: DataContext, treePath: TreePath) {
    super.processDoubleClick(e, dataContext, treePath)
    onDoubleClick(tree, treePath)
  }

}
