/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.state.config.files

/**
 * Configuration item that represents datasets mask
 * @param mask the datasets mask that would be fetched by the plug-in
 * @param volser the volume serial to search for the datasets by
 */
data class DatasetMaskConfigItem(var mask: String = "", var volser: String = "") {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DatasetMaskConfigItem) return false

    if (mask != other.mask) return false
    if (volser != other.volser) return false

    return true
  }

  override fun hashCode(): Int {
    var result = mask.hashCode()
    result = 31 * result + volser.hashCode()
    return result
  }
}
