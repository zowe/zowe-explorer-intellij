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

package org.zowe.explorer.v3.icons

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

// TODO: doc
object ZoweExplorerIcons {
  private fun loadIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  @JvmField
  val zoweExplorerIcon = loadIcon("icons/explorer.svg")
  @JvmField
  val workingSetIcon = AllIcons.Actions.ShowAsTree
  @JvmField
  val datasetMask = loadIcon("icons/datasetMask.svg")
}
