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

/** Interface to represent an object that would carry other settings */
interface OtherSettingsHolder {
  val rateUsNotificationDelay: Long
    get() = 5 * 60 * 1000L
  val autoSaveDelay: Long
    get() = 5L

  var isAutoSyncEnabled: Boolean
  var batchSize: Int
}
