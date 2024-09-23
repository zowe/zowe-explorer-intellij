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

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.vfs.MFVirtualFile
import org.zowe.kotlinsdk.Member
import org.zowe.kotlinsdk.XIBMDataType

/**
 * Implementation of DependentFileAttributes for working with members of the dataset.
 * @param info information about member returned by zosmf.
 * @param parentFile file that represents the dataset of current member.
 * @param contentMode mode (binary, text, its encoding) of the member content.
 * @author Valiantsin Krus
 */
data class RemoteMemberAttributes(
  override val info: Member,
  override val parentFile: MFVirtualFile,
  override var contentMode: XIBMDataType = XIBMDataType(XIBMDataType.Type.TEXT),
) : DependentFileAttributes<Member, MFVirtualFile> {

  /**
   * Member name.
   * @see FileAttributes.name
   */
  override val name
    get() = info.name

  /**
   * Always 0.
   * @see FileAttributes.length
   */
  override val length = 0L

  /**
   * Clones current attributes.
   * @return copy of current attributes.
   */
  override fun clone(): FileAttributes {
    return RemoteMemberAttributes(info.clone(), parentFile)
  }

}

/**
 * Finds the attributes of the dataset that is the parent of the current member.
 * @param dataOpsManager instance of DataOpsManager service to find attributes service in.
 */
fun RemoteMemberAttributes.getLibraryAttributes(dataOpsManager: DataOpsManager): RemoteDatasetAttributes? {
  return dataOpsManager.getAttributesService(RemoteDatasetAttributes::class.java, parentFile::class.java)
    .getAttributes(parentFile)
}
