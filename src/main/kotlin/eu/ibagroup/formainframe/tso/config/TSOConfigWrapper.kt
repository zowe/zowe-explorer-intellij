/*
 * Copyright (c) 2020-2024 IBA Group.
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

package eu.ibagroup.formainframe.tso.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import org.zowe.kotlinsdk.TsoData
import org.zowe.kotlinsdk.TsoResponse

/**
 * Class which is used to consolidate all the information about TSO session created
 * @param tsoSessionConfig - the config of the TSO session
 * @param connectionConfig - the config of the connection where the TSO session is created
 * @param response - an instance of TSO response. It contains necessary info about the TSO session created
 */
class TSOConfigWrapper(
  private var tsoSessionConfig: TSOSessionConfig,
  private var connectionConfig: ConnectionConfig,
  private var response: TsoResponse = TsoResponse()
) {

  var reconnectAttempts: Int = 0
  var unresponsive: Boolean = false

  /**
   * Getter for TSO session config
   */
  fun getTSOSessionConfig() : TSOSessionConfig {
    return tsoSessionConfig
  }

  /**
   * Getter for TSO session response from z/OSMF
   */
  fun getTSOResponse() : TsoResponse {
    return response
  }

  /**
   * Getter for TSO session response message queue
   */
  fun getTSOResponseMessageQueue() : List<TsoData> {
    return response.tsoData
  }

  /**
   * Setter for TSO session message queue. It's needed for cases when not whole response came from TSO session
   * @param tsoData - list of TSO messages to be set for TSO session
   */
  fun setTSOResponseMessageQueue(tsoData : List<TsoData>) {
    response.tsoData = tsoData
  }

  /**
   * Getter for connection config where TSO session is created
   */
  fun getConnectionConfig() : ConnectionConfig {
    return connectionConfig
  }

  /**
   * Clears reconnect attempts of this session
   */
  fun clearReconnectAttempts() {
    reconnectAttempts = 0
  }

  /**
   * Increments reconnect attempt of this session
   */
  fun incrementReconnectAttempt() {
    reconnectAttempts++
  }

  /**
   * Method marks the session as unresponsive after reconnect failure
   */
  fun markSessionUnresponsive() {
    unresponsive = true
  }

}
