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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.swing.Icon

// TODO: doc
open class RefreshInfoNodeDescriptor(
  displayName: String,
  tooltip: String,
  icon: Icon?,
  var refreshInfo: String = ""
) : ExplorerTreeNodeDescriptor(displayName, tooltip, icon, false, false) {
  fun getCurrentRefreshDateTime(): String {
    return DateTimeFormatter
      .ofPattern("dd MMM YYYY HH:mm:ss", Locale.ENGLISH)
      .withZone(ZoneId.systemDefault())
      .format(LocalDateTime.now())
      .uppercase(Locale.getDefault())
  }

  override fun updateNode(presentationData: PresentationData) {
    super.updateNode(presentationData)
    if (refreshInfo.isNotEmpty()) {
      presentationData.addText(" $refreshInfo", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES)
    }
  }
}
