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

package org.zowe.explorer.v3.state.settings

/**
 * Other settings event listener base interface.
 * Provides basic functionality to implement for modification listeners
 */
interface OtherSettingsEventListener {
  /**
   * Handle the [OtherSettingsEventProducer.onOtherSettingsReload] event
   * @param newSettings the new settings to apply
   */
  fun otherSettingsReloaded(newSettings: OtherSettingsHolder)

  /**
   * Handle the [OtherSettingsEventProducer.onOtherSettingsChange] event
   * @param newSettings the new state to apply
   */
  fun otherSettingsChanged(newSettings: OtherSettingsHolder)
}
