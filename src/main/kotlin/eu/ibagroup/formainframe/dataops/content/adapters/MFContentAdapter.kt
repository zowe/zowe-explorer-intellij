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

package eu.ibagroup.formainframe.dataops.content.adapters

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile

/**
 * Interface for work with content adapters.
 * Adapters needed by the reason of fact that content on mainframe and
 * in Intellij content storage can be stored with different rules. That's
 * why it is needed to convert content from Intellij to mainframe and vice versa.
 */
interface MFContentAdapter {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<MFContentAdapterFactory>("eu.ibagroup.formainframe.mfContentAdapter")
  }

  /**
   * Checks if the current implementation of adapter can adapt content of specified file.
   * @param file file to check on possibility to adapt.
   * @return true if content of the passed file can be adapted by this implementation or false otherwise.
   */
  fun accepts(file: VirtualFile): Boolean

  /**
   * Prepares content for uploading to the mainframe using virtual file instance.
   * @param content content to adapt.
   * @param file file whose content will be adapted.
   * @return adapted content.
   */
  fun <T>prepareContentToMainframe(content: T, file: VirtualFile): T

  /**
   * Prepares content for uploading from mainframe to Intellij content storage using virtual file.
   * @param content content bytes to adapt.
   * @param file file whose content will be adapted.
   * @return adapted content bytes.
   */
  fun adaptContentFromMainframe(content: ByteArray, file: VirtualFile): ByteArray
}
