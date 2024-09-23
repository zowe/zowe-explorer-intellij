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

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import eu.ibagroup.formainframe.dataops.operations.MessageType as MessageTypeEnum
import org.zowe.kotlinsdk.MessageType
import org.zowe.kotlinsdk.TsoApi
import org.zowe.kotlinsdk.TsoData
import org.zowe.kotlinsdk.TsoResponse
import io.ktor.util.*
import retrofit2.Response

/**
 * Factory class which represents a TSO operation runner. Defined in plugin.xml
 */
class TsoOperationRunnerFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): OperationRunner<*, *> {
    return TsoOperationRunner()
  }
}

/**
 * Base instance class which is built by factory during runtime
 */
class TsoOperationRunner : OperationRunner<TsoOperation, TsoResponse> {
  override val operationClass = TsoOperation::class.java
  override val resultClass = TsoResponse::class.java
  override val log = log<TsoOperationRunner>()

  /**
   * Method determines if an operation can run
   */
  override fun canRun(operation: TsoOperation) = true

  /**
   * Method serves as main entry point performing an operation
   * @param operation - an operation to be run
   * @param progressIndicator - progress indicator instance
   * @return an instance of TsoResponse
   */
  override fun run(operation: TsoOperation, progressIndicator: ProgressIndicator): TsoResponse {
    val mode = operation.mode
    var response: Response<TsoResponse>? = null
    when (mode) {
      TsoOperationMode.START -> {
        val state = operation.state as TSOConfigWrapper
        val tsoSessionConfig = state.getTSOSessionConfig()
        response = api<TsoApi>(state.getConnectionConfig())
          .startTso(
            state.getConnectionConfig().authToken,
            proc = tsoSessionConfig.logonProcedure.toUpperCasePreservingASCIIRules(),
            chset = tsoSessionConfig.charset,
            cpage = tsoSessionConfig.codepage.toString(),
            rows = tsoSessionConfig.rows,
            cols = tsoSessionConfig.columns,
            acct = tsoSessionConfig.accountNumber?.toUpperCasePreservingASCIIRules(),
            ugrp = tsoSessionConfig.userGroup?.toUpperCasePreservingASCIIRules(),
            rsize = tsoSessionConfig.regionSize
          )
          .cancelByIndicator(progressIndicator)
          .execute()
      }

      TsoOperationMode.SEND_MESSAGE -> {
        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          response = api<TsoApi>(state.getConnectionConfig())
            .sendMessageToTso(
              state.getConnectionConfig().authToken,
              body = createTsoData(operation),
              servletKey = servletKey
            )
            .cancelByIndicator(progressIndicator)
            .execute()
        }
      }

      TsoOperationMode.GET_MESSAGES -> {
        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          response = api<TsoApi>(state.getConnectionConfig())
            .receiveMessagesFromTso(
              state.getConnectionConfig().authToken,
              servletKey = servletKey
            )
            .cancelByIndicator(progressIndicator)
            .execute()
        }
      }

      TsoOperationMode.STOP -> {
        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          response = api<TsoApi>(state.getConnectionConfig())
            .endTso(
              state.getConnectionConfig().authToken,
              servletKey = servletKey
            )
            .cancelByIndicator(progressIndicator)
            .execute()
        }
      }
    }
    if (response != null) {
      val body = response.body()
      if (body != null) {
        if (!response.isSuccessful || body.msgData.isNotEmpty()) {
          var errorMsg = ""
          for (msg in body.msgData) {
            errorMsg += msg.messageText + "\n"
          }
          if (errorMsg.isNotEmpty()) {
            throw Exception(errorMsg)
          } else {
            throw CallException(response, response.message())
          }
        }
      } else {
        throw CallException(response, response.message())
      }
    }
    return response?.body() ?: throw Exception("Cannot retrieve response from server.")
  }

  /**
   * Create TsoData object depending on the specified message type
   * @throws Exception if message type not specified
   */
  private fun createTsoData(operation: TsoOperation): TsoData {
    return when (operation.messageType) {
      MessageTypeEnum.TSO_MESSAGE -> TsoData(
        tsoMessage = createMessageType(operation)
      )

      MessageTypeEnum.TSO_PROMPT -> TsoData(
        tsoPrompt = createMessageType(operation)
      )

      MessageTypeEnum.TSO_RESPONSE -> TsoData(
        tsoResponse = createMessageType(operation)
      )

      null -> throw Exception("Message type not specified")
    }
  }

  /**
   * Create MessageType object depending on the specified message data
   * @throws Exception if message data not specified
   */
  private fun createMessageType(operation: TsoOperation): MessageType {
    return when (operation.messageData) {
      MessageData.DATA_DATA -> MessageType(
        version = "0100",
        data = operation.message
      )

      MessageData.DATA_HIDDEN -> MessageType(
        version = "0100",
        hidden = operation.message
      )

      MessageData.DATA_ACTION -> MessageType(
        version = "0100",
        action = operation.message
      )

      null -> throw Exception("Message data not specified")
    }
  }

}
