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

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import eu.ibagroup.r2z.Member
import eu.ibagroup.r2z.XIBMDataType

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
