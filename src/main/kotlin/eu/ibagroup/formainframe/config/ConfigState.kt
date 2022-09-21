/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig

/**
 * State of all configs for plugin. It includes lists of connections, working sets and masks, other settings.
 * @author Viktar Mushtsin, Kiril Branavitski, Valiantsin Krus
 */
data class ConfigState(
  var connections: MutableList<ConnectionConfig> = mutableListOf(),
  var workingSets: MutableList<FilesWorkingSetConfig> = mutableListOf(),
  var jobsWorkingSets: MutableList<JobsWorkingSetConfig> = mutableListOf(),
  var settings: SettingsState = SettingsState()
)

/**
 * State for other additional settings.
 * @author Valiantsin Krus
 */
data class SettingsState(
  var isAutoSyncEnabled: Boolean = false,
  var batchSize: Int = 100
)