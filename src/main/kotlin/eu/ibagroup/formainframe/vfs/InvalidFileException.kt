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

package eu.ibagroup.formainframe.vfs

import com.intellij.openapi.vfs.VirtualFile
import java.io.FileNotFoundException

/** Class for invalid file exception. Raises when the file is not found in the file system */
class InvalidFileException(file: VirtualFile) : FileNotFoundException(
  "${file.name} not found on fs ${file.fileSystem}"
)
