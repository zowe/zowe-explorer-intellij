/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.LayeredIcon
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.IconUtil
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JLabel

/** Object to store Zowe Explorer elements icons to code mapping */
object ZoweExplorerIcons {
  /**
   * Load an icon by the specified path string
   * @param path the resource path to load the icon by
   * @return the loaded [Icon] object
   */
  private fun loadIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  /**
   * Create an icon for a filter node
   * @param filterMainIcon the main icon to display as a filter background
   * @param filterText the text that indicates the kind of items to be stored under the filter
   * @return the [Icon] object, composed of the [filterMainIcon], the [filterText] and a filter icon
   */
  private fun createFilterIcon(filterMainIcon: Icon, filterText: String): Icon {
    return object : Icon {
      override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val layeredIcon = LayeredIcon(3)
          .apply {
            val dsText = IconUtil
              .textToIcon(filterText, c ?: JLabel(), JBUIScale.scale(7f))
            val filterIconSmall = IconUtil.scale(AllIcons.General.Filter, c, 0.6f)

            setIcon(filterMainIcon, 0)
            setIcon(
              filterIconSmall,
              1,
              filterMainIcon.iconWidth - filterIconSmall.iconWidth + JBUIScale.scale(2),
              0
            )
            setIcon(
              dsText,
              2,
              filterMainIcon.iconWidth - dsText.iconWidth + JBUIScale.scale(2),
              filterMainIcon.iconHeight - dsText.iconHeight + JBUIScale.scale(2)
            )
          }
        layeredIcon.paintIcon(c, g, x, y)
      }

      override fun getIconWidth(): Int = filterMainIcon.iconWidth

      override fun getIconHeight(): Int = filterMainIcon.iconHeight
    }
  }

  /**
   * Create a folder icon from the provided parameters
   * @param folderIcon the icon to display as a folder icon
   * @param folderText the folder view text to display together with the folder icon
   * @return the [Icon] object, composed of the [folderIcon] and the [folderText] underneath it
   */
  private fun createFolderIcon(folderIcon: Icon, folderText: String): Icon {
    return object : Icon {
      override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val layeredIcon = LayeredIcon(2)
          .apply {
            val folderTextIcon = IconUtil
              .textToIcon(folderText, c ?: JLabel(), JBUIScale.scale(7f))

            setIcon(folderIcon, 0)
            setIcon(
              folderTextIcon,
              1,
              folderIcon.iconWidth - folderTextIcon.iconWidth + JBUIScale.scale(2),
              folderIcon.iconHeight - folderTextIcon.iconHeight + JBUIScale.scale(2)
            )
          }
        layeredIcon.paintIcon(c, g, x, y)
      }

      override fun getIconWidth(): Int = folderIcon.iconWidth

      override fun getIconHeight(): Int = folderIcon.iconHeight
    }
  }

  @JvmField
  val zoweExplorerIcon = loadIcon("icons/explorer.svg")
  @JvmField
  val workingSetIcon = AllIcons.Actions.ShowAsTree
  @JvmField
  val datasetMask = createFilterIcon(AllIcons.Modules.TestRoot, "DS")
  @JvmField
  val libraryDataset = createFolderIcon(AllIcons.Modules.TestRoot, "PDS")
  @JvmField
  val ussFilter = createFilterIcon(AllIcons.Modules.SourceRoot, "USS")
  @JvmField
  val jesFilter = createFilterIcon(AllIcons.Modules.ExcludeRoot, "JES")
}
