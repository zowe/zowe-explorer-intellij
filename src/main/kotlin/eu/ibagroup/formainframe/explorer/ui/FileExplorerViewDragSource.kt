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

import com.intellij.ide.IdeBundle
import com.intellij.ide.PsiCopyPasteManager
import com.intellij.ide.dnd.DnDAction
import com.intellij.ide.dnd.DnDDragStartBean
import com.intellij.ide.dnd.DnDSource
import com.intellij.ide.dnd.TransferableWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.psi.PsiElement
import com.intellij.ui.render.RenderingUtil
import com.intellij.ui.tabs.impl.SingleHeightTabs
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBUI
import eu.ibagroup.formainframe.utils.log
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.image.BufferedImage
import java.io.File
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private val log = log<FileExplorerViewDragSource>()


/** Drag source representation */
class FileExplorerViewDragSource(
  private val myTree: Tree,
  private val mySelectedNodesDataProvider: () -> List<NodeData<*>>,
  private val cutCopyPredicate: (NodeData<*>) -> Boolean,
  private val copyPasteSupport: FileExplorerView.ExplorerCopyPasteSupport
) : DnDSource {

  override fun canStartDragging(action: DnDAction?, dragOrigin: Point): Boolean {
    val nodes = mySelectedNodesDataProvider()
    return nodes.all(cutCopyPredicate)
  }

  override fun createDraggedImage(
    action: DnDAction?,
    dragOrigin: Point?,
    bean: DnDDragStartBean
  ): com.intellij.openapi.util.Pair<Image, Point>? {
    val paths: Array<TreePath> = myTree.selectionPaths ?: return null
    val toRender = mySelectedNodesDataProvider().map {
      val coloredTextToDisplay = it.node.presentation.coloredText.firstOrNull()?.text
      if (coloredTextToDisplay == null) {
        log.warn("Tree node for file '${it.file?.name}' doesn't contain colored text.")
      }
      Pair(coloredTextToDisplay ?: "ERROR", it.node.icon)
    }

    var count = 0
    val panel = JPanel(VerticalFlowLayout(0, 0))
    val maxItemsToShow = if (toRender.size < 20) toRender.size else 10
    for (pair in toRender) {

      val fileLabel: JLabel = DragImageLabel(myTree, pair.first, pair.second)
      panel.add(fileLabel)
      count++
      if (count > maxItemsToShow) {
        panel.add(
          DragImageLabel(
            myTree,
            IdeBundle.message("label.more.files", paths.size - maxItemsToShow),
            EmptyIcon.ICON_16
          )
        )
        break
      }
    }
    panel.size = panel.preferredSize
    panel.doLayout()

    val image = ImageUtil.createImage(panel.width, panel.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = image.graphics as Graphics2D
    panel.paint(g2)
    g2.dispose()

    return com.intellij.openapi.util.Pair(image, Point())
  }

  class ExplorerTransferableWrapper(val myTree: Tree) : TransferableWrapper {
    override fun asFileList(): List<File>? {
      return PsiCopyPasteManager.asFileList(psiElements)
    }

    override fun getTreePaths(): Array<TreePath>? {
      return myTree.selectionPaths
    }

    override fun getTreeNodes(): Array<TreeNode> {
      return emptyArray()
    }

    override fun getPsiElements(): Array<PsiElement> {
      return emptyArray()
    }
  }

  override fun startDragging(action: DnDAction?, dragOrigin: Point): DnDDragStartBean {
    copyPasteSupport.registerDropTargetInProjectViewIfNeeded()
    return DnDDragStartBean(ExplorerTransferableWrapper(myTree))
  }

}

class DragImageLabel(myTree: Tree, text: String, icon: Icon? = null) : JLabel(text) {
  init {
    this.icon = icon
    foreground = RenderingUtil.getForeground(myTree, true)
    background = RenderingUtil.getBackground(myTree, true)
    border = EmptyBorder(JBUI.CurrentTheme.EditorTabs.tabInsets())
  }

  override fun getPreferredSize(): Dimension? {
    val size = super.getPreferredSize()
    size.height = JBUI.scale(SingleHeightTabs.UNSCALED_PREF_HEIGHT)
    return size
  }
}
