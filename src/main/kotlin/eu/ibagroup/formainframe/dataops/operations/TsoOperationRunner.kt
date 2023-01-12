/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.ui.build.tso.ui.TSOSessionParams
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.r2z.MessageType
import eu.ibagroup.r2z.TsoApi
import eu.ibagroup.r2z.TsoData
import eu.ibagroup.r2z.TsoResponse
import io.ktor.util.*
import retrofit2.Response
import java.nio.charset.Charset
import java.util.*

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
        val state = operation.state as TSOSessionParams
        response = api<TsoApi>(state.connectionConfig)
          .startTso(
            state.connectionConfig.authToken,
            proc = state.logonproc.toUpperCasePreservingASCIIRules(),
            chset = state.charset,
            cpage = state.codepage.toString(),
            rows = state.rows.toInt(),
            cols = state.cols.toInt(),
            acct = state.acct.toUpperCasePreservingASCIIRules(),
            ugrp = state.usergroup.toUpperCasePreservingASCIIRules(),
            rsize = state.region.toInt()
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
              body = TsoData(
                tsoResponse = MessageType(
                  version = "0100",
                  data = operation.message
                )
              ),
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
          val errorMsg = body.msgData.toString()
          throw CallException(response, errorMsg)
        }
      } else {
        throw CallException(response, response.message())
      }
    }
    return response?.body() ?: throw Exception("Cannot retrieve response from server.")
  }

  /**
   * Method is used to generate default Application ID needed for TSO request
   * @return random byte string for application ID
   */
  private fun generateDefaultAppId(): String {
    val array = ByteArray(8)
    Random().nextBytes(array)
    return String(array, Charset.defaultCharset())
  }

}
