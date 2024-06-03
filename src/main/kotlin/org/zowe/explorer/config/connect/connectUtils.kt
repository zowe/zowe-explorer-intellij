/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.connect

import com.intellij.openapi.components.service
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.MessageData
import org.zowe.explorer.dataops.operations.MessageType
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.ui.build.tso.TSOWindowFactory
import org.zowe.explorer.ui.build.tso.config.TSOConfigWrapper
import org.zowe.explorer.ui.build.tso.ui.TSOSessionParams
import org.zowe.kotlinsdk.TsoData


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
        val queuedMessages: MutableList<TsoData> = mutableListOf()
        queuedMessages.addAll(response.tsoData)

        // consume all the TSO messages while tsoPrompt become not null
        while (response.tsoData.last().tsoPrompt == null) {
          response = TSOWindowFactory.getTsoMessageQueue(tsoSession)
          queuedMessages.addAll(response.tsoData)
        }

        owner = tryToExtractRealOwner(queuedMessages)
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
 * Utility function extracts the owner from the messages returned from the TSO request
 * @param tsoData
 * @return USS Owner string value if tsoData contains the userID or an empty string otherwise
 */
fun tryToExtractRealOwner(tsoData: List<TsoData>) : String {
  val emptyOwner = ""
  val filteredData = tsoData.filter {
    val tsoMessage = it.tsoMessage ?: return@filter false
    val messageData = tsoMessage.data?.trim() ?: return@filter false
    messageData.isNotEmpty() && !messageData.contains("READY") && messageData.chars().count() < 9
  }.mapNotNull { it.tsoMessage?.data?.trim() }

  return if (filteredData.isNotEmpty()) filteredData[0] else emptyOwner
}

/**
 * Returns owner of particular connection config if it is not empty, or the username otherwise.
 * @param connectionConfig connection config instance.
 * @return owner of connection config.
 */
fun getOwner(connectionConfig: ConnectionConfig): String {
  return connectionConfig.owner.ifEmpty { getUsername(connectionConfig) }
}
