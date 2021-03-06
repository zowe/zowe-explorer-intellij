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

import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.vfs.MFVirtualFile

interface DependentFileAttributes<InfoType, VFile: VirtualFile>: FileAttributes{
  val parentFile: VFile
  val info: InfoType
}
