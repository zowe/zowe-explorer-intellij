/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3.state.config.migration

import org.zowe.explorer.v3.state.config.Config
import org.zowe.explorer.v3.state.config.files.FilesWorkingSetConfig

/**
 * Template version of the working set config v1 as an example of the migration mechanism
 * @property wsName the name of the working set as the field to convert
 */
class WorkingSetConfigV1(
  uuid: String = EMPTY_ID,
  var wsName: String = "",
) : OldConfig(uuid) {
  override fun convertToNextVersion(): Config {
    return FilesWorkingSetConfig(uuid, name = wsName)
  }
}
