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

package org.zowe.explorer.dataops.operations

import com.intellij.openapi.progress.ProgressIndicator
import org.zowe.explorer.api.api
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.tso.config.TSOConfigWrapper
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.explorer.dataops.operations.MessageType as MessageTypeEnum
import org.zowe.kotlinsdk.MessageType
import org.zowe.kotlinsdk.TsoApi
import org.zowe.kotlinsdk.TsoData
import org.zowe.kotlinsdk.TsoResponse
import io.ktor.util.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.zowe.kotlinsdk.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    // vad
    println("Executing TSO Operation: $operation")
    var response: Response<TsoResponse>? = null
    val state = operation.state as TSOConfigWrapper
    val tsoSessionConfig = state.getTSOSessionConfig()
    val connectionConfig = state.getConnectionConfig()
    println("connectionConfig.url: ${connectionConfig.url}")
// Configure your OkHttp client with logging interceptor
    // Trust manager that accepts all certificates
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
      override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
      override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
      override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })
// Create an SSL context with the trust manager that accepts all certificates
    val sslContext = SSLContext.getInstance("SSL").apply {
      init(null, trustAllCerts, SecureRandom())
    }
    val loggingInterceptor = HttpLoggingInterceptor { message ->
      println(message) // This will print each line of the log to standard output
    }.apply {
      level = HttpLoggingInterceptor.Level.BODY
    }
    val httpClient = OkHttpClient.Builder()
      .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
      .hostnameVerifier { _, _ -> true }  // Ignore hostname verification
      .addInterceptor(loggingInterceptor)
      .build()
    // Configure Retrofit with the custom OkHttp client
    val retrofit = Retrofit.Builder()
//      .baseUrl("https://10.25.2.69:10443") // replace with the actual base URL
      .baseUrl(connectionConfig.url) // replace with the actual base URL
      .client(httpClient)
//      .addConverterFactory(GsonConverterFactory.create())
//      .addConverterFactory(BytesConverterFactory.create())
      .addConverterFactory(GsonConverterFactory.create(gson))
//      .addConverterFactory(ScalarsConverterFactory.create())
      .build()

    val tsoApi = retrofit.create(TsoApi::class.java)
// /vad

    when (mode) {
      TsoOperationMode.START -> {

//        val state = operation.state as TSOConfigWrapper
//        val tsoSessionConfig = state.getTSOSessionConfig()

//        response = api<TsoApi>(state.getConnectionConfig())
        response = tsoApi
          // /vad
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
        //vad
//        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          // vad
//          response = api<TsoApi>(state.getConnectionConfig())
          response = tsoApi
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
        // vad
//        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          // vad
//          response = api<TsoApi>(state.getConnectionConfig())
          response = tsoApi
            .receiveMessagesFromTso(
              state.getConnectionConfig().authToken,
              servletKey = servletKey
            )
            .cancelByIndicator(progressIndicator)
            .execute()
        }
      }

      TsoOperationMode.STOP -> {
        // vad
//        val state = operation.state as TSOConfigWrapper
        val servletKey = state.getTSOResponse().servletKey
        if (servletKey != null) {
          //vad
//          response = api<TsoApi>(state.getConnectionConfig())
          response = tsoApi
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
