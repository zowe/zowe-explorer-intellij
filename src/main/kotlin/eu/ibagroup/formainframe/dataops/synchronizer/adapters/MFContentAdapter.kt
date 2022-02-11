/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

interface MFContentAdapter {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<MFContentAdapterFactory>("eu.ibagroup.formainframe.mfContentAdapter")
  }

  fun accepts(file: VirtualFile): Boolean

  fun performAdaptingToMainframe (content: ByteArray, file: VirtualFile): ByteArray

  fun performAdaptingFromMainframe (content: ByteArray, file: VirtualFile): ByteArray
}
