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

/** Base interface to provide updates on any other settings specific action happened */
interface OtherSettingsEventProducer {
  /**
   * Produce an event when an element of a specific type is reloaded
   * @param newSettings the new settings to process
   */
  fun onOtherSettingsReload(newSettings: OtherSettingsHolder)

  /**
   * Produce an event when an element of a specific type is changed
   * @param newSettings the new settings to process
   */
  fun onOtherSettingsChange(newSettings: OtherSettingsHolder)
}
