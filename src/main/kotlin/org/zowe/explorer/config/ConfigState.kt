/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.JobsWorkingSetConfig
import org.zowe.explorer.config.ws.FilesWorkingSetConfig

data class ConfigState(
  var connections: MutableList<ConnectionConfig> = mutableListOf(),
  var workingSets: MutableList<FilesWorkingSetConfig> = mutableListOf(),
  var jobsWorkingSets: MutableList<JobsWorkingSetConfig> = mutableListOf()
)
