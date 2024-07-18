/*
 * Copyright (c) 2020 IBA Group.
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

package org.zowe.explorer.config

/**
 * State for other additional settings.
 * @author Valiantsin Krus
 */
data class SettingsState(
  var isAutoSyncEnabled: Boolean = false,
  var batchSize: Int = 100
)
