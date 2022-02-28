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

import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDNativeTarget
import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.ide.dnd.TransferableWrapper
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.ui.awt.RelativeRectangle
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ArrayUtilRt
import org.zowe.explorer.explorer.Explorer
import java.awt.Rectangle
import javax.swing.tree.TreePath


class GlobalExplorerViewDropTarget(
  val myTree: Tree,
  val explorer: Explorer<*>,
  private val copyPasteSupport: GlobalFileExplorerView.ExplorerCopyPasteSupport
) : DnDNativeTarget {

  override fun drop(event: DnDEvent) {
    val sourcesTargetBounds = getSourcesTargetAndBounds(event) ?: return
    val pasteProvider = copyPasteSupport.getPasteProvider(listOf(sourcesTargetBounds.second))
    val cutProvider = copyPasteSupport.getCutProvider(sourcesTargetBounds.first?.toList() ?: listOf())
    if (cutProvider.isCutEnabled(DataContext.EMPTY_CONTEXT)) {
      cutProvider.performCut(DataContext.EMPTY_CONTEXT)
    }
    if (pasteProvider.isPastePossible(DataContext.EMPTY_CONTEXT)) {
      pasteProvider.performPaste(DataContext.EMPTY_CONTEXT)
    }
  }

  override fun update(event: DnDEvent): Boolean {
    val sourcesTargetBounds = getSourcesTargetAndBounds(event) ?: return false
    val sources = sourcesTargetBounds.first ?: return false
    if (ArrayUtilRt.find(sources, sourcesTargetBounds.second) != -1 || !FileCopyPasteUtil.isFileListFlavorAvailable(event)) {
      return false
    }

    val pasteEnabled = copyPasteSupport.isPastePossibleFromPath(listOf(sourcesTargetBounds.second), sources.toList())
    event.isDropPossible = pasteEnabled
    if (pasteEnabled) {
      event.setHighlighting(
        RelativeRectangle(myTree, sourcesTargetBounds.third),
        DnDEvent.DropTargetHighlightingType.RECTANGLE
      )
    }
    return false
  }

  private fun getSourcesTargetAndBounds(event: DnDEvent): Triple<Array<TreePath?>?, TreePath, Rectangle>? {
    event.setDropPossible(false, "")

    val point = event.point ?: return null

    val target: TreePath = myTree.getClosestPathForLocation(point.x, point.y) ?: return null

    val bounds = myTree.getPathBounds(target)
    if (bounds == null || bounds.y > point.y || point.y >= bounds.y + bounds.height) return null

    val sources = getSourcePaths(event.attachedObject)
    return Triple(sources, target, bounds)
  }

  private fun getSourcePaths(transferData: Any): Array<TreePath?>? {
    val wrapper = if (transferData is TransferableWrapper) transferData else null
    return wrapper?.treePaths
  }
}
