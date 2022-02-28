/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ForMainframeIcons {

  private fun getIcon(path: String): Icon {
    return IconLoader.getIcon(path, this::class.java)
  }

  @JvmField
  val ExplorerToolbarIcon = getIcon("icons/explorer.svg")

  @JvmField
  val JclDirectory = getIcon("icons/jclDir.svg")
}
