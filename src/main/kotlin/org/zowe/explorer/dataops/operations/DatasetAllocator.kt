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
import org.zowe.explorer.api.apiWithBytesConverter
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.authToken
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.explorer.config.Presets
import org.zowe.explorer.explorer.config.getSampleJclMemberContent
import org.zowe.explorer.utils.cancelByIndicator
import org.zowe.explorer.utils.log
import org.zowe.kotlinsdk.*

/**
 * Class which represents factory for dataset allocator operation runner. Defined in plugin.xml
 */
class DatasetAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return DatasetAllocator()
  }
}

/**
 * Data class which represents dataset allocation operation object
 */
data class DatasetAllocationOperation(
  override val request: DatasetAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<DatasetAllocationParams>

/**
 * Class which represents dataset allocator operation runner
 */
class DatasetAllocator : Allocator<DatasetAllocationOperation> {

  /**
   * Runs a dataset allocation operation
   * @param operation - dataset allocation operation to be run
   * @param progressIndicator - progress indicator object
   * @throws CallException if request is nor successful
   * @return Void
   */
  override fun run(
    operation: DatasetAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()


//    val datasetResponse: Response<Void> =
//    if (operation.request.presets == Presets.VSAM_KSDS_DATASET) {
//      sendVSAMCreationRequest(operation.request.datasetName, operation.connectionConfig.authToken)
//    } else {
      val datasetResponse = api<DataAPI>(operation.connectionConfig).createDataset(
        authorizationToken = operation.connectionConfig.authToken,
        datasetName = operation.request.datasetName,
        body = operation.request.allocationParameters
      ).cancelByIndicator(progressIndicator).execute()
//    }
//    val tsoResponse = api<TsoApi>(operation.connectionConfig).startTso(
//    val datasetResponse = api<TsoApi>(operation.connectionConfig).startTso(
//      authorizationToken = operation.connectionConfig.authToken,
//      proc = "DBSPROCC",   // TSO procedure
//      chset = "697",           // Charset
//      cpage = "1047",           // Codepage
//      rows = 24,                 // Number of rows in session
//      cols = 80,                 // Number of columns in session
//      acct = "DEFAULT",     // Optional account number
//      system = "SYS2"     // Optional system name
//    ).execute()

//    println(datasetResponse)

//    val servletKey = tsoResponse.body()?.servletKey
//    val servletKey = datasetResponse.body()?.servletKey

//    val tsoIdcams = TsoData(tsoMessage = MessageType("tso idcams"))
//
//    api<TsoApi>(operation.connectionConfig).sendMessageToTso(
//      authorizationToken = operation.connectionConfig.authToken,
//      body = tsoIdcams,
//      servletKey = servletKey!!,
//      readReply = true
//    ).execute()
//
//    val tsoData = TsoData(tsoMessage = MessageType(
//      "DEFINE CLUSTER (NAME(YANK.TEST.VSAM.KSDS1) INDEXED KEYS(8 0) RECORDSIZE(80 80) TRACKS(10 5) VOLUMES(D5USR1)) " +
//          "DATA(NAME(YANK.TEST.VSAM.KSDS1.DATA)) " +
//          "INDEX(NAME(YANK.TEST.VSAM.KSDS1.INDEX))")
//    )
//
//    api<TsoApi>(operation.connectionConfig).sendMessageToTso(
//      authorizationToken = operation.connectionConfig.authToken,
//      body = tsoData,
//      servletKey = servletKey,
//      readReply = true  // This will wait for a response after sending
//    ).execute()
//
//    val datasetResponse = api<TsoApi>(operation.connectionConfig).receiveMessagesFromTso(
//      authorizationToken = operation.connectionConfig.authToken,
//      servletKey = servletKey
//    ).execute()
//
//    api<TsoApi>(operation.connectionConfig).endTso(
//      authorizationToken = operation.connectionConfig.authToken,
//      servletKey = servletKey,
//      tsoForceCancel = false   // Use LOGOFF (or true to CANCEL)
//    ).execute()

      if (!datasetResponse.isSuccessful) {
      throw CallException(
        datasetResponse,
        "Cannot allocate dataset ${operation.request.datasetName} on ${operation.connectionConfig.name}"
      )
    } else {
      if (operation.request.presets != Presets.CUSTOM_DATASET
        && operation.request.presets != Presets.SEQUENTIAL_DATASET
        && operation.request.presets != Presets.PDS_DATASET
        && operation.request.presets != Presets.PDSE_DATASET
        && operation.request.presets != Presets.VSAM_KSDS_DATASET
      ) {
        // Allocate member
        var throwable: Throwable? = null
        runCatching {
          val contentToWrite = if (
            operation.request.presets == Presets.PDS_WITH_EMPTY_MEMBER
            || operation.request.presets == Presets.PDSE_WITH_EMPTY_MEMBER
          ) {
            byteArrayOf()
          } else {
            getSampleJclMemberContent(CredentialService.getUsername(operation.connectionConfig))
              .encodeToByteArray()
          }
          val memberResponse = apiWithBytesConverter<DataAPI>(operation.connectionConfig).writeToDatasetMember(
            authorizationToken = operation.connectionConfig.authToken,
            datasetName = operation.request.datasetName,
            memberName = operation.request.memberName,
            content = contentToWrite
          ).cancelByIndicator(progressIndicator).execute()
          if (!memberResponse.isSuccessful) {
            throwable = CallException(
              memberResponse,
              "Cannot create sample member ${operation.request.memberName} in ${operation.request.datasetName} " +
                "on ${operation.connectionConfig.name}"
            )
            throw throwable as CallException
          }
        }
          .onFailure {
            // Suppressed as the compiler does not correctly recognize the throwable state change
            @Suppress("KotlinConstantConditions")
            if (throwable != null) throw Throwable(cause = throwable)
            else throw Exception("Error allocating a new sample member ${operation.request.memberName}")
          }
      }
    }
  }

  override val operationClass = DatasetAllocationOperation::class.java

  override val log = log<DatasetAllocator>()

}

//private fun sendVSAMCreationRequest(datasetName: String, authorizationToken: String): Response {
//  // Construct the JSON body with datasetName
//  val requestBodyJson = JSONObject()
//  requestBodyJson.put("input", listOf(
//    "DEFINE CLUSTER (NAME($datasetName) INDEXED KEYS(8 0) RECORDSIZE(80 80) TRACKS(10 5) VOLUMES(D5USR1)) -",
//    "DATA(NAME($datasetName.DATA)) -",
//    "INDEX(NAME($datasetName.INDEX))"
//  ))
//  requestBodyJson.put("JSONversion", 1)
//
//  // Create request body from the JSON string
//  val mediaType = "application/json".toMediaType()
//  val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
//
//  // Build the HTTP PUT request
//  val request = Request.Builder()
//    .url("https://10.25.2.69:10443/zosmf/restfiles/ams")
//    .put(requestBody)  // This is a PUT request
//    .addHeader("Content-Type", "application/json")
//    .addHeader("Authorization", authorizationToken)  // Add the Authorization token
//    .build()
//
//  // OkHttp client to send the request
//  val client = OkHttpClient()
//
//  // Send the request and get the response
//  return client.newCall(request).execute()  // This sends the request synchronously
//}

/**
 * Data class which represents input parameters for dataset allocation operation
 * @param datasetName - dataset name
 * @param errorMessage - error message
 * @param allocationParameters - instance of CreateDataset object with allocation parameters
 */
data class DatasetAllocationParams(
  var presets: Presets = Presets.CUSTOM_DATASET,
  var datasetName: String = "",
  var memberName: String = "",
  var errorMessage: String = "",
  val allocationParameters: CreateDataset = CreateDataset(
    allocationUnit = AllocationUnit.TRK,
    primaryAllocation = 1,
    secondaryAllocation = 0,
    recordFormat = RecordFormat.FB,
    datasetOrganization = DatasetOrganization.PS,
    recordLength = 80
  )
)
