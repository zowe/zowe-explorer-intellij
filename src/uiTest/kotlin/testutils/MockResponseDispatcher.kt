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

package testutils
//import auxiliary.buildFinalListDatasetJson
import auxiliary.buildListMembersJson
import auxiliary.responseDispatcher
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.junit.jupiter.api.TestInfo
import java.io.File

data class ValidationListElem(
  val name: String,
  val validator: (RecordedRequest?) -> Boolean,
  val handler: (RecordedRequest?) -> MockResponse
)

open class MockResponseDispatcher : Dispatcher() {

  private var validationList = mutableListOf<ValidationListElem>()

  private fun getResourceText(resourcePath: String): String? {
    return javaClass.classLoader.getResource(resourcePath)?.readText()
  }

  fun readMockJson(mockFilePath: String): String? {
    return getResourceText("mock/${mockFilePath}.json")
  }

  fun injectEndpoint(
    name: String,
    validator: (RecordedRequest?) -> Boolean,
    handler: (RecordedRequest?) -> MockResponse
  ) {
    validationList.add(ValidationListElem(name, validator, handler))
  }

  fun removeEndpoint(name: String) {
    validationList.removeAll { it.name == name }
  }

  fun removeAllEndpoints() {
    validationList.clear()
  }

  fun injectAllocationResultPo(
    datasetOrganization: String,
    recordFormatShort: String,
    dsName:String,
    dsOrganisationShort:String,
    recordLength:Int,
    handler: Boolean = false){

    responseDispatcher.injectEndpoint(
      "testAllocateValid${datasetOrganization}Datasets_${recordFormatShort}_restfiles",
      { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: handler },
      { MockResponse().setBody("{\"dsorg\":\"${dsOrganisationShort}\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"${recordFormatShort}\",\"blksize\":3200,\"lrecl\":${recordLength}}") }
    )

  }

  fun injectAllocationResultPds(pdsName: String, handler: Boolean = false){

    responseDispatcher.injectEndpoint(
      "allocatePds_restfiles",
      { it?.requestLine?.contains("POST /zosmf/restfiles/ds/$pdsName") ?: handler },
      { MockResponse().setBody("{\"dsorg\":\"PDS\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}") }
    )

  }

  fun injectAllocatedDatasets(datasetMask: String, body: String, datasetName:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "listAllocatedDatasets_restfiles_${datasetMask}",
      { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName}*") ?: handler },
      { MockResponse().setBody(body) }
    )
  }


  fun injectListMembers(body: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "listMembers_restfiles",
      { it?.requestLine?.contains("/member") ?: handler },
      { MockResponse().setBody(body) }
    )
  }

  fun injectListAllDatasetMembersRestfiles(pdsName: String, body: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "listAllDatasetMembers_restfiles",
      { it?.requestLine?.contains("GET /zosmf/restfiles/ds/$pdsName/member") ?: handler },
      { MockResponse().setBody(body) }
    )
  }

  fun injectDeleteDataset(datasetName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "deleteDataset_restfiles_${datasetName}",
      { it?.requestLine?.contains("DELETE /zosmf/restfiles/ds/${datasetName}") ?: handler },
      { MockResponse().setBody("{}") }
    )
  }

  fun injectTestInfo(testInfo: TestInfo,handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "${testInfo.displayName}_info",
      { it?.requestLine?.contains("zosmf/info") ?: handler },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
  }

  fun injectTestInfoRestTopology(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "${testInfo.displayName}_resttopology",
      { it?.requestLine?.contains("zosmf/resttopology/systems") ?: handler },
      { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
  }

  fun injectMigratePdsRestFiles(pdsName:String, contains: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "migratePds_restfiles",
      { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName") ?: false &&
              it?.body?.toString()?.contains(contains) ?: handler },
      { MockResponse().setResponseCode(200) }
    )
  }

  fun injectRecallPds(pdsName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
      "migratePds_restfiles",
      { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName") ?: false &&
              it?.body?.toString()?.contains("hrecall") ?: handler },
      { MockResponse().setResponseCode(200) }
    )
  }

//  internal open fun injectRenameMember(testInfo: TestInfo, datasetName: String, memberNameNew: String, memberNameOld: String, handler: Boolean = false){
//    responseDispatcher.injectEndpoint(
//      "${testInfo.displayName}_restfiles_rename_member",
//      { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberNameNew})") ?: false },
//      { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${datasetName}\",\"member\":\"${memberNameOld}\"}}") }
//    )
//  }
//  fun injectMemberList(testInfo: TestInfo, datasetName: String, listElem: List<String>, listName: String? = null){//= true){
//    val requestName = if (listName != null) {
//      "${testInfo.displayName}_restfiles_listmembers$listName"
//    } else {
//      "${testInfo.displayName}_restfiles_listmembers"
//    }
//
//    val body_headr = "{\"items\":["
//    val body_tell = "],\"returnedRows\": ${listElem.size},\"JSONversion\": 1}"
//    val memebersString = listElem.joinToString(separator = ",") { "{\"member\": \"$it\"}" }
//
//    injectEndpoint(
//      requestName,
//      { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false},
//      { MockResponse().setBody(body_headr + memebersString + body_tell)}
//    )
//  }



  override fun dispatch(request: RecordedRequest): MockResponse {
    val fileName = System.getProperty("user.dir") + "/src/uiTest/resources/request_received.txt"



    val x = validationList
      .firstOrNull { it.validator(request) }

//    if (x?.handler != null && x != null){
//      val kClass = x.handler::class
//      val properties = kClass.memberProperties
//
//      for (property in properties) {
//
//        File(fileName).appendText("=================${property.name}===========")
////        println("${property.name}: ${property.get(x.handler)}")
//      }
//    }

    File(fileName).appendText("${x?.name}\n${x?.validator}${x?.handler}\n${x?.handler}\n")
    return x
      ?.handler
      ?.let { it(request) }
      ?: MockResponse().setBody("Response is not implemented").setResponseCode(404)
  }
}

fun injectSingleMember(testInfo:TestInfo, datasetName: String, listMembersInDataset:  MutableList<String>){
  responseDispatcher.injectEndpoint(
    "${testInfo.displayName}_restfiles_listmembers",
    { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false },
    {
      MockResponse().setBody(buildListMembersJson(listMembersInDataset))
    }
  )
}
