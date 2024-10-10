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

import org.zowe.explorer.v3.getListenerEndpoint

/** Other settings event producer. Produces events for the [OtherSettingsEventListener] */
class OtherSettingsEventProducerImpl : OtherSettingsEventProducer {
  /** @see [OtherSettingsEventProducer.onOtherSettingsReload] */
  override fun onOtherSettingsReload(newSettings: OtherSettingsHolder) {
    getListenerEndpoint(OtherSettingsService.TOPIC).otherSettingsReloaded(newSettings)
  }

  /** @see [OtherSettingsEventProducer.onOtherSettingsChange] */
  override fun onOtherSettingsChange(newSettings: OtherSettingsHolder) {
    getListenerEndpoint(OtherSettingsService.TOPIC).otherSettingsChanged(newSettings)
  }
}
