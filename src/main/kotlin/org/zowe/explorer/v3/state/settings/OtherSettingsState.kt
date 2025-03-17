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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.v3.state.settings

/**
 * Other settings state representation
 * @property isAutoSyncEnabled represents if the auto-synchronization is enabled
 * @property batchSize the batch size during a single fetch of the datasets
 */
class OtherSettingsState(
  override var isAutoSyncEnabled: Boolean = false,
  override var batchSize: Int = 100
) : OtherSettingsHolder
