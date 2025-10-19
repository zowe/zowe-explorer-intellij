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

// TODO: doc
interface UpdateInfoHolder {
  val textToPreserve: List<PresentableNodeDescriptor.ColoredFragment>

  fun setUpdateInfo(presentationData: PresentationData) {
    presentationData.clearText()
    textToPreserve.forEach { textFragment ->
      presentationData.addText(textFragment)
    }
    presentationData.addText(" refreshed: ${getCurrentRefreshDateTime()}", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
  }
}