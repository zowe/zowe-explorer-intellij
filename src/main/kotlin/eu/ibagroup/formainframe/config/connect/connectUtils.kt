/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.ui.build.tso.TSOWindowFactory
import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionParams


/**
 * Sends TSO request "oshell whoami", with which it receives the name of the real user (owner) of the system.
 * @return owner name if retrieved or null otherwise.
 */
fun whoAmI(connectionConfig: ConnectionConfig): String? {
  var owner: String? = null
  val tsoSessionParams = TSOSessionParams(connectionConfig)
  runCatching {
    val tsoResponse = service<DataOpsManager>().performOperation(
      TsoOperation(tsoSessionParams, TsoOperationMode.START)
    )
    if (tsoResponse.servletKey?.isNotEmpty() == true) {
      val tsoSession = TSOConfigWrapper(tsoSessionParams, tsoResponse)
      while (tsoSession.getTSOResponseMessageQueue().last().tsoPrompt == null) {
        val response = TSOWindowFactory.getTsoMessageQueue(tsoSession)
        tsoSession.setTSOResponseMessageQueue(response.tsoData)
      }
      runCatching {
        service<DataOpsManager>().performOperation(
          TsoOperation(
            state = tsoSession,
            mode = TsoOperationMode.SEND_MESSAGE,
            messageType = MessageType.TSO_MESSAGE,
            messageData = MessageData.DATA_DATA,
            message = "oshell whoami"
          )
        )
      }.onSuccess {
        var response = it
        response.tsoData.last().tsoMessage?.data?.let { data -> owner = data.trim() }
        while (response.tsoData.last().tsoPrompt == null) {
          response = TSOWindowFactory.getTsoMessageQueue(tsoSession)
        }
      }
      service<DataOpsManager>().performOperation(
        TsoOperation(
          state = tsoSession,
          mode = TsoOperationMode.STOP
        )
      )
    }
  }
  return owner
}

/**
 * Returns owner of particular connection config if it is not empty, or the username otherwise.
 * @param connectionConfig connection config instance.
 * @return owner of connection config.
 */
fun getOwner(connectionConfig: ConnectionConfig): String {
  return connectionConfig.owner.ifEmpty { getUsername(connectionConfig) }
}
