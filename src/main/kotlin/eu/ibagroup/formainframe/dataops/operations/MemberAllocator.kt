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
import eu.ibagroup.formainframe.api.apiWithBytesConverter
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.exceptions.CallException
import eu.ibagroup.formainframe.utils.cancelByIndicator
import eu.ibagroup.formainframe.utils.log
import io.ktor.util.*
import org.zowe.kotlinsdk.DataAPI

/**
 * Class which represents factory for member allocator operation runner. Defined in plugin.xml
 */
class MemberAllocatorFactory : OperationRunnerFactory {
  override fun buildComponent(dataOpsManager: DataOpsManager): Allocator<*> {
    return MemberAllocator()
  }
}

/**
 * Data class which represents member allocation operation object
 */
data class MemberAllocationOperation(
  override val request: MemberAllocationParams,
  override val connectionConfig: ConnectionConfig,
) : RemoteUnitOperation<MemberAllocationParams>

/**
 * Class which represents member allocator operation runner
 */
class MemberAllocator : Allocator<MemberAllocationOperation> {

  override val operationClass = MemberAllocationOperation::class.java

  override val log = log<MemberAllocator>()

  /**
   * Runs a member allocation operation
   * @param operation member allocation operation to be run
   * @param progressIndicator progress indicator object
   * @throws Throwable if request is nor successful
   * @return Void
   */
  override fun run(
    operation: MemberAllocationOperation,
    progressIndicator: ProgressIndicator
  ) {
    progressIndicator.checkCanceled()
    val newMemberName = operation.request.memberName.toUpperCasePreservingASCIIRules()
    val memberAllocationErrorString = "Cannot create member $newMemberName in ${operation.request.datasetName} " +
        "on ${operation.connectionConfig.name}."
    val listMembersRequest = api<DataAPI>(operation.connectionConfig).listDatasetMembers(
      authorizationToken = operation.connectionConfig.authToken,
      datasetName = operation.request.datasetName
    ).cancelByIndicator(progressIndicator).execute()
    if (listMembersRequest.isSuccessful) {
      val membersList = listMembersRequest.body()
      val duplicateMember = membersList?.items?.map { it.name.toUpperCasePreservingASCIIRules() }?.find { it == newMemberName }
      if (duplicateMember != null) {
        throw CallException(
          listMembersRequest,
          "$memberAllocationErrorString Member with name $newMemberName already exists."
        )
      } else {
        val writeRequest = apiWithBytesConverter<DataAPI>(operation.connectionConfig).writeToDatasetMember(
          authorizationToken = operation.connectionConfig.authToken,
          datasetName = operation.request.datasetName,
          memberName = newMemberName,
          content = byteArrayOf()
        ).cancelByIndicator(progressIndicator).execute()
        if (!writeRequest.isSuccessful) {
          throw CallException(
            writeRequest,
            memberAllocationErrorString
          )
        }
      }
    } else {
      throw CallException(
        listMembersRequest,
        "Cannot fetch member list for ${operation.request.datasetName}"
      )
    }
  }
}

/**
 * Data class which represents input parameters for uss allocation operation
 */
data class MemberAllocationParams(val datasetName: String, var memberName: String = "")
