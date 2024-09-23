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
package eu.ibagroup.formainframe.dataops.operations.mover.names

import com.intellij.openapi.vfs.VirtualFile

/**
 * Implementation of [IndexedNameResolver] that is used by default (if no one other name resolver was found)
 * It just adds _(<index>) to the end of the file name before extension.
 * @author Valiantsin Krus
 */
class DefaultNameResolver: IndexedNameResolver() {
  override fun accepts(source: VirtualFile, destination: VirtualFile): Boolean {
    return true
  }

  override fun resolveNameWithIndex(source: VirtualFile, destination: VirtualFile, index: Int?): String {
    val sourceName = source.name
    return if (index == null) {
      sourceName
    } else {
      val extension = if (sourceName.contains(".")) sourceName.substringAfterLast(".") else null
      val newNameWithoutExtension = "${sourceName.substringBeforeLast(".")}_(${index})"
      if (extension != null) "$newNameWithoutExtension.$extension" else newNameWithoutExtension
    }
  }

}
