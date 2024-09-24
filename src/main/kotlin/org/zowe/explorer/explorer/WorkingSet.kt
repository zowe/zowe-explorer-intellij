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

package org.zowe.explorer.explorer

import com.intellij.openapi.Disposable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.ConnectionConfigBase
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.JobsFilter
import org.zowe.explorer.config.ws.UssPath

/** Interface to represent the working set base */
interface WorkingSet<Connection: ConnectionConfigBase, Mask> : ExplorerUnit<Connection>, Disposable {

  val masks: Collection<Mask>

  fun addMask(mask: Mask)

  fun removeMask(mask: Mask)

}

/** Interface to represent the files working set */
interface FilesWorkingSet : WorkingSet<ConnectionConfig, DSMask> {
  val ussPaths: Collection<UssPath>

  fun addUssPath(ussPath: UssPath)

  fun removeUssPath(ussPath: UssPath)

}

/** Interface to represent the JES working set */
interface JesWorkingSet : WorkingSet<ConnectionConfig, JobsFilter> {
  fun removeFilter(jobsFilter: JobsFilter)
}
