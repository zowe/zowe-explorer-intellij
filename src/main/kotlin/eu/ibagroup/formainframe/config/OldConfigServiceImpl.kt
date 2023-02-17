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

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Implementation of [OldConfigService] to read old configs from old file with configs.
 * @author Valiantsin Krus.
 */
@State(
  name = "by.iba.connector.services.ConfigService",
  storages = [Storage(value = "iba_connector_config.xml", exportable = true)]
)
class OldConfigServiceImpl: OldConfigService {

  companion object {
    private val myState: ConfigState = ConfigState()
  }

  override fun getState(): ConfigState {
    return myState
  }

  /**
   * Load current config state
   * @param state the state to load
   */
  override fun loadState(state: ConfigState) {
    XmlSerializerUtil.copyBean(state, myState)
  }
}