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

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.vfs.MFVirtualFile

/**
 * Content adapter that doesn't adapt content of the file.
 * @param dataOpsManager instance of DataOpsManager service.
 * @author Valiantsin Krus
 */
class DefaultContentAdapter(dataOpsManager: DataOpsManager) : MFContentAdapterBase<FileAttributes>(dataOpsManager) {

  override val vFileClass = MFVirtualFile::class.java
  override val attributesClass = FileAttributes::class.java

  /**
   * Passes content to mainframe with no changes.
   * @see MFContentAdapterBase.adaptContentToMainframe
   */
  override fun <T>adaptContentToMainframe(content: T, attributes: FileAttributes): T = content

  /**
   * Passes content from mainframe to content storage with no changes.
   */
  override fun adaptContentFromMainframe(content: ByteArray, attributes: FileAttributes): ByteArray = content
}
