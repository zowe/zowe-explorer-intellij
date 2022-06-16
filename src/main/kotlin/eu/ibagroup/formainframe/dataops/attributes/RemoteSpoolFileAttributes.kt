/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.attributes

import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.SpoolFile
import eu.ibagroup.r2z.XIBMDataType

data class RemoteSpoolFileAttributes(
  override val info: SpoolFile,
  override val parentFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT)
) : DependentFileAttributes<SpoolFile, MFVirtualFile> {
  override val name: String
    get() = info.ddName
  override val length: Long
    get() = 0L

  override fun clone(): FileAttributes {
    return RemoteSpoolFileAttributes(info.clone(), parentFile)
  }

}
