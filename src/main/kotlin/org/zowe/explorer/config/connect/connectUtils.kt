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
import com.intellij.openapi.diagnostic.logger
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialog
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.operations.MessageData
import org.zowe.explorer.dataops.operations.MessageType
import org.zowe.explorer.dataops.operations.TsoOperation
import org.zowe.explorer.dataops.operations.TsoOperationMode
import org.zowe.explorer.ui.build.tso.TSOWindowFactory
import org.zowe.explorer.ui.build.tso.config.TSOConfigWrapper
import org.zowe.explorer.ui.build.tso.ui.TSOSessionParams
import org.zowe.kotlinsdk.*
import org.zowe.kotlinsdk.annotations.ZVersion

const val USER_OR_OWNER_SYMBOLS_MAX_SIZE: Int = 8
const val WHO_AM_I: String = "oshell whoami"
val LOGGER = logger<ConnectionDialog>()

/**
 * Sends TSO request "oshell whoami", with which it receives the name of the real user (owner) of the system.
 * @return owner name if retrieved or empty string if owner cannot be retrieved, null if z/OS version is not supported.
 */
fun whoAmI(connectionConfig: ConnectionConfig): String? {
  val zVersion = connectionConfig.zVersion
  val tsoSessionParams = TSOSessionParams(connectionConfig)
  var owner: String? = null
  if (zVersion < ZVersion.ZOS_2_4) {
    val emptyOwner = ""
    val queuedMessages: MutableList<TsoData> = mutableListOf()
    runCatching {
      val tsoStartResponse = service<DataOpsManager>().performOperation(
        TsoOperation(tsoSessionParams, TsoOperationMode.START)
      )
      if (tsoStartResponse.servletKey?.isNotEmpty() == true) {
        val tsoSession = TSOConfigWrapper(tsoSessionParams, tsoStartResponse)
        while (tsoSession.getTSOResponseMessageQueue().last().tsoPrompt == null) {
          val response = TSOWindowFactory.getTsoMessageQueue(tsoSession)
          tsoSession.setTSOResponseMessageQueue(response.tsoData)
        }
        var sendCommandResponse = service<DataOpsManager>().performOperation(
          TsoOperation(
            state = tsoSession,
            mode = TsoOperationMode.SEND_MESSAGE,
            messageType = MessageType.TSO_RESPONSE,
            messageData = MessageData.DATA_DATA,
            message = WHO_AM_I
          )
        )
        queuedMessages.addAll(sendCommandResponse.tsoData)
        // consume all the TSO messages while tsoPrompt become not null
        while (sendCommandResponse.tsoData.last().tsoPrompt == null) {
          sendCommandResponse = TSOWindowFactory.getTsoMessageQueue(tsoSession)
          queuedMessages.addAll(sendCommandResponse.tsoData)
        }
        service<DataOpsManager>().performOperation(
          TsoOperation(
            state = tsoSession,
            mode = TsoOperationMode.STOP
          )
        )
      }
    }.onSuccess {
      LOGGER.info("whoAmI: queuedMessages = $queuedMessages")
      owner = tryToExtractRealOwner(queuedMessages)
    }.onFailure {
      LOGGER.info("whoAmI call failed.", it)
      owner = emptyOwner
    }
  } else {
    owner = executeWhoAmIEnhanced(connectionConfig)
  }
  return owner
}

/**
 * Function executes whoAmI command for z/OS versions > 2.3.
 * Execution of the command does not require starting/stop of the TSO session every time,
 * so it's much easier to maintain and troubleshoot entire request
 * @param connectionConfig
 * @return String value of the extracted owner
 */
fun executeWhoAmIEnhanced(connectionConfig: ConnectionConfig): String? {
  var owner: String? = null
  val emptyOwner = ""
  val cmdResponse: MutableList<TsoCmdResult> = mutableListOf()
  runCatching {
    val newTsoResponse = api<TsoApi>(connectionConfig).executeTsoCommand(
      authorizationToken = connectionConfig.authToken,
      body = TsoCmdRequestBody(
        tsoCmd = WHO_AM_I,
        cmdState = TsoCmdState.STATELESS
      )
    ).execute()
    if (newTsoResponse.isSuccessful) {
      newTsoResponse.body()?.let { cmdResponse.addAll(it.cmdResponse) }
    }
  }.onSuccess {
    LOGGER.info("whoAmIEnhanced: cmdResponse = $cmdResponse")
    owner = tryToExtractRealOwnerEnhanced(cmdResponse)
  }.onFailure {
    LOGGER.info("whoAmIEnhanced call failed.", it)
    owner = emptyOwner
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
    messageData.isNotEmpty() && !messageData.contains("READY") && messageData.chars().count() <= USER_OR_OWNER_SYMBOLS_MAX_SIZE
  }.mapNotNull { it.tsoMessage?.data?.trim() }

  return if (filteredData.isNotEmpty()) filteredData[0] else emptyOwner
}

/**
 * Utility function extracts the owner from the messages returned from the enhanced TSO request
 * @param tsoData
 * @return USS Owner string value if tsoData contains the userID or an empty string otherwise
 */
fun tryToExtractRealOwnerEnhanced(tsoData: List<TsoCmdResult>) : String {
  val emptyOwner = ""
  val filteredData = tsoData.filter {
    val tsoMessage = it.message?.trim() ?: return@filter false
    tsoMessage.isNotEmpty() && !tsoMessage.contains("READY") && tsoMessage.chars().count() <= USER_OR_OWNER_SYMBOLS_MAX_SIZE
  }.mapNotNull { it.message?.trim() }

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

/**
 * Function tries to extract owner from the connection config.
 * If whoAmI function failed for some reason it could contain empty "" or error string inside owner variable.
 * If it is such a case then return username of the connection config.
 * @return username of the connection config or extracted owner
 */
fun tryToExtractOwnerFromConfig(connectionConfig: ConnectionConfig): String {
  val possibleOwner = connectionConfig.owner
  return if (possibleOwner.isEmpty() || possibleOwner.chars().count() > USER_OR_OWNER_SYMBOLS_MAX_SIZE)
    getUsername(connectionConfig)
  else possibleOwner
}
