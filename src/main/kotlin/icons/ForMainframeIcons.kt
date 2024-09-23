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

package icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import javax.swing.Icon

/** Object to load icons to use them in the plugin */
object ForMainframeIcons {

  /** Get icon by its relative path. The function will search for the icon in resources folder */
  private fun getIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  @JvmField
  val ExplorerToolbarIcon = getIcon("icons/toolWindowLogo.svg")

  @JvmField
  val JclDirectory = getIcon("icons/jclDir.svg")

  @JvmField
  val DatasetMask = getIcon("icons/datasetMask.svg")

  @JvmField
  val MemberIcon = IconUtil.addText(AllIcons.FileTypes.Any_type, "MEM")
}
