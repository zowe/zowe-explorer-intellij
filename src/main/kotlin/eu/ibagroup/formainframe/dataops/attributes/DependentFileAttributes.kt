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

import com.intellij.openapi.vfs.VirtualFile

/**
 * Attributes for a files that always depend on the parent.
 * For example members always depend on dataset, spool files always depend on job.
 * @param InfoType information class whose instances returned by zosmf (e.g. Member or SpoolFile).
 * @param VFile virtual file class
 */
interface DependentFileAttributes<InfoType, VFile : VirtualFile> : FileAttributes {
  /** parent file for dependent one. */
  val parentFile: VFile

  /** Information about dependent file on mainframe. */
  val info: InfoType
}
