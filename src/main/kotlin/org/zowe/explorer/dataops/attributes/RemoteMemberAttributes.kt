/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.dataops.attributes

import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.utils.clone
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.XIBMDataType

data class RemoteMemberAttributes(
  override val info: Member,
  override val parentFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT),
) : DependentFileAttributes<Member, MFVirtualFile> {

  override val name
    get() = info.name

  override val length = 0L


  override fun clone(): FileAttributes {
    return RemoteMemberAttributes(info.clone(), parentFile)
  }

  override val isCopyPossible
    get() = true

}

fun RemoteMemberAttributes.getLibraryAttributes(dataOpsManager: DataOpsManager): RemoteDatasetAttributes? {
  return dataOpsManager.getAttributesService(RemoteDatasetAttributes::class.java, parentFile::class.java)
    .getAttributes(parentFile)
}
