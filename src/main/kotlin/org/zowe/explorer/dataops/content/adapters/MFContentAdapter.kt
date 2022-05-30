/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.content.adapters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

interface MFContentAdapter {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<MFContentAdapterFactory>("org.zowe.explorer.mfContentAdapter")
  }

  fun accepts(file: VirtualFile): Boolean

  fun prepareContentToMainframe (content: ByteArray, file: VirtualFile): ByteArray

  fun adaptContentFromMainframe (content: ByteArray, file: VirtualFile): ByteArray
}
