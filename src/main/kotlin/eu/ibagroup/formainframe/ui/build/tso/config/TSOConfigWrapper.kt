/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.ui.build.tso.config

import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionParams
import eu.ibagroup.r2z.TsoData
import eu.ibagroup.r2z.TsoResponse

/**
 * Class which is used to consolidate all the information about TSO session created
 * @param sessionParams - parameters of the TSO session created
 * @param response - an instance of TSO response. It contains necessary info about the TSO session created
 */
class TSOConfigWrapper(private var sessionParams: TSOSessionParams, private var response: TsoResponse) {

  /**
   * Getter for TSO session parameters
   */
  fun getTSOSessionParams() : TSOSessionParams {
    return sessionParams
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
    return sessionParams.connectionConfig
  }

}