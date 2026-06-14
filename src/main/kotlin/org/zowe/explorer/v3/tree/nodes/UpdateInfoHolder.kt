/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.ui.SimpleTextAttributes

private val EMPTY_UPDATE_INFO = PresentableNodeDescriptor.ColoredFragment(
  "",
  SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
)

/**
 * The node update info holder (e.g. date and time last refreshed)
 * @property textToPreserve the text to preserve for the node
 * @property currentUpdateInfo the current update info to display
 */
interface UpdateInfoHolder {
  val textToPreserve: List<PresentableNodeDescriptor.ColoredFragment>
  var currentUpdateInfo: PresentableNodeDescriptor.ColoredFragment?

  fun setUpdateInfo(
    presentationData: PresentationData,
    updateInfoToSet: PresentableNodeDescriptor.ColoredFragment? = null
  ) {
    presentationData.clearText()
    textToPreserve.forEach { textFragment ->
      presentationData.addText(textFragment)
    }
    currentUpdateInfo = updateInfoToSet
      ?: PresentableNodeDescriptor.ColoredFragment(
        " refreshed: ${getCurrentRefreshDateTime()}",
        SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES
      )
    presentationData.addText(currentUpdateInfo ?: EMPTY_UPDATE_INFO)
  }
}