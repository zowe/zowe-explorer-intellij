/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.tree.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.SimpleTextAttributes
import org.zowe.explorer.v3.state.config.ConnectionConfigRelated
import javax.swing.Icon

// TODO: doc
open class FileFetcherNodeDescriptor(
  displayName: String,
  tooltip: String,
  icon: Icon,
  var fetchedFilesCount: Int = 0,
  override var connectionConfigUuid: String
) : RefreshInfoNodeDescriptor(displayName, tooltip, icon),
  ConnectionConfigRelated,
  Traversable
{
  override val path: List<String>
    get() = TODO("Not yet implemented")

  override fun updateNode(presentationData: PresentationData) {
    presentationData.tooltip = tooltip
    presentationData.addText(displayName, SimpleTextAttributes.REGULAR_ATTRIBUTES)

    val fetchedFilesInfo = if (fetchedFilesCount != 0) "$fetchedFilesCount file(s)" else ""
    val additionalInfo = listOf(fetchedFilesInfo, refreshInfo).filterNot { it.isEmpty() }.joinToString(", ")
    if (additionalInfo.isNotEmpty()) {
      presentationData.addText(" $additionalInfo", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }
  }
}
