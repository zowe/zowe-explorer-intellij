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

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.SpoolFile
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Implementation of DependentFileAttributes for working with spool files of the job.
 * @param info information about spool file returned by zosmf.
 * @param parentFile file that represents the job of current spool file.
 * @param contentMode mode (binary, text, its encoding) of the spool file content.
 * @author Valiantsin Krus
 */
data class RemoteSpoolFileAttributes(
  override val info: SpoolFile,
  override val parentFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)
) : DependentFileAttributes<SpoolFile, MFVirtualFile> {

  /**
   *  DDNAME for the data set creation.
   *  @see FileAttributes.name
   */
  override val name: String
    get() = info.ddName

  /**
   * Always 0.
   * @see FileAttributes.length
   */
  override val length: Long
    get() = 0L

  /**
   * Clones current attributes.
   * @return copy of current attributes.
   */
  override fun clone(): FileAttributes {
    return RemoteSpoolFileAttributes(info.clone(), parentFile)
  }

}
