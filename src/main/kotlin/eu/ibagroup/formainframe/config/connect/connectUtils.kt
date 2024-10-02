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

package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.diagnostic.logger
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.ui.zosmf.ConnectionDialog
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.dataops.operations.TsoOperation
import eu.ibagroup.formainframe.dataops.operations.TsoOperationMode
import eu.ibagroup.formainframe.tso.TSOWindowFactory
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.tso.config.ui.TSOSessionDialogState
import org.zowe.kotlinsdk.*
import org.zowe.kotlinsdk.annotations.ZVersion

const val WHO_AM_I_CMD: String = "oshell whoami"
val LOGGER = logger<ConnectionDialog>()

/**
 * Sends TSO request "oshell whoami", with which it receives the name of the real user (owner) of the system.
 * @return owner name if retrieved or empty string if owner cannot be retrieved, null if z/OS version is not supported.
 */
fun whoAmI(connectionConfig: ConnectionConfig): String? {
  val zVersion = connectionConfig.zVersion
  var owner: String? = null
  if (zVersion < ZVersion.ZOS_2_4) {
    val state = TSOSessionDialogState()
    state.connectionConfigUuid = connectionConfig.uuid
    val emptyOwner = ""
    val queuedMessages: MutableList<TsoData> = mutableListOf()
    runCatching {
      val tsoStartResponse = DataOpsManager.getService().performOperation(
        TsoOperation(
          TSOConfigWrapper(state.tsoSessionConfig, connectionConfig),
          TsoOperationMode.START
        )
      )
      if (tsoStartResponse.servletKey?.isNotEmpty() == true) {
        val tsoSession = TSOConfigWrapper(state.tsoSessionConfig, connectionConfig, tsoStartResponse)
        while (tsoSession.getTSOResponseMessageQueue().last().tsoPrompt == null) {
          val response = TSOWindowFactory.getTsoMessageQueue(tsoSession)
          tsoSession.setTSOResponseMessageQueue(response.tsoData)
        }
        var sendCommandResponse = DataOpsManager.getService().performOperation(
          TsoOperation(
            state = tsoSession,
            mode = TsoOperationMode.SEND_MESSAGE,
            messageType = MessageType.TSO_RESPONSE,
            messageData = MessageData.DATA_DATA,
            message = WHO_AM_I_CMD
          )
        )
        queuedMessages.addAll(sendCommandResponse.tsoData)
        // consume all the TSO messages while tsoPrompt become not null
        while (sendCommandResponse.tsoData.last().tsoPrompt == null) {
          sendCommandResponse = TSOWindowFactory.getTsoMessageQueue(tsoSession)
          queuedMessages.addAll(sendCommandResponse.tsoData)
        }
        DataOpsManager.getService().performOperation(
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
        tsoCmd = WHO_AM_I_CMD,
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
fun tryToExtractRealOwner(tsoData: List<TsoData>): String {
  val emptyOwner = ""
  val filteredData = tsoData
    .filter {
      val tsoMessage = it.tsoMessage ?: return@filter false
      val messageData = tsoMessage.data?.trim() ?: return@filter false
      messageData.isNotEmpty()
        && !messageData.contains("READY")
        && messageData.chars().count() <= USER_OR_OWNER_SYMBOLS_MAX_SIZE
    }
    .mapNotNull { it.tsoMessage?.data?.trim() }

  return if (filteredData.isNotEmpty()) filteredData[0] else emptyOwner
}

/**
 * Utility function extracts the owner from the messages returned from the enhanced TSO request
 * @param tsoData
 * @return USS Owner string value if tsoData contains the userID or an empty string otherwise
 */
fun tryToExtractRealOwnerEnhanced(tsoData: List<TsoCmdResult>): String {
  val emptyOwner = ""
  val filteredData = tsoData
    .filter {
      val tsoMessage = it.message?.trim() ?: return@filter false
      tsoMessage.isNotEmpty()
        && !tsoMessage.contains("READY")
        && tsoMessage.chars().count() <= USER_OR_OWNER_SYMBOLS_MAX_SIZE
    }
    .mapNotNull { it.message?.trim() }

  return if (filteredData.isNotEmpty()) filteredData[0] else emptyOwner
}
