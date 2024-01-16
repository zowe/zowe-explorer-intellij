/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset.testutils

import okhttp3.mockwebserver.MockResponse
import auxiliary.*
import org.junit.jupiter.api.TestInfo
import workingset.DATA_SET_RENAME_FAILED

//import testutils.MockResponseDispatcher

fun injectRenameDataset(testInfo: TestInfo, dsFinalName: String, dsName: String){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_rename_ds",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${dsFinalName}") ?: false },
        { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${dsName}\"}}") }
    )
}

fun injectRenameMember(testInfo: TestInfo, datasetName: String, memberNameNew: String, memberNameOld: String, handler: Boolean=false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_rename_member",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberNameNew})") ?: handler },
        { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${datasetName}\",\"member\":\"${memberNameOld}\"}}") }
    )
}

fun injectRenameMemberUnsuccessful(testInfo: TestInfo, datasetName: String, memberNameNew: String, memberNameOld: String, code: Int, rcCode: String, errorDetail:String, handler: Boolean=false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_rename_member",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberNameOld})") ?: handler },
        {
            MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${datasetName}\",\"member\":\"${memberNameNew}\"}}")
                .setResponseCode(code)
                .setBody("{\"category\":\"4.0\",\"message\":\"Rename member failed\",\"rc\":$rcCode,\"details\":[\"ISRZ002 $errorDetail - Directory already contains the specified member name.\"],\"reason\":\"0.0\"}")
        }
    )
}

fun injectRenameDatasetUnsuccessful(testInfo: TestInfo, dsFinalName: String, anotherDsName: String,  code: Int, rcCode: String, errorDetail:String, handler: Boolean=false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_rename_ds",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${anotherDsName}") ?: handler },
        {
            MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${dsFinalName}\"}}")
                .setResponseCode(code)
                .setBody("{\"category\":\"1.0\",\"message\":\"$DATA_SET_RENAME_FAILED\",\"rc\":\"$rcCode\",\"details\":[\"$errorDetail\"],\"reason\":\"6.0\"}")
        }
    )
}


fun injectMemberList(testInfo: TestInfo, datasetName: String, listElem: List<String>, listName: String? = null, isFirstRequest: Boolean= true){//= true){
    val requestName = if (listName != null) {
        "${testInfo.displayName}_restfiles_listmembers$listName"
    } else {
        "${testInfo.displayName}_restfiles_listmembers"
    }

    val body_headr = "{\"items\":["
    val body_tell = "],\"returnedRows\": ${listElem.size},\"JSONversion\": 1}"
    val memebersString = listElem.joinToString(separator = ",") { "{\"member\": \"$it\"}" }

    responseDispatcher.injectEndpoint(
        requestName,
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false && isFirstRequest},
        { MockResponse().setBody(body_headr + memebersString + body_tell)}
    )
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
fun injectSingleSpecificMember(pdsName: String, memberName:  String, responseCode: Int=204){
    responseDispatcher.injectEndpoint(
        "allocateDatasetMember_restfiles",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/$pdsName($memberName)") ?: false },
        { MockResponse().setResponseCode(responseCode) }
    )
}

fun injectListAllAllocatedDatasets(datasetMask: String, mapListDatasets: MutableMap<String, String>, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "listAllAllocatedDatasets_restfiles",
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetMask}*") ?: handler },
        { MockResponse().setBody(buildFinalListDatasetJson(mapListDatasets)) }
    )
}

fun injectListEmptyData(testInfo:TestInfo, isDataset: Boolean=true, handler: Boolean = false){
    val pathType = if (isDataset) "ds?dslevel=" else "fs?path"
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles",
        { it?.requestLine?.contains("GET /zosmf/restfiles/${pathType}") ?: handler },
        { MockResponse().setBody("{}") }
    )
}

fun injectListAllAllocatedDatasetsWithContents(testInfo:TestInfo,datasetName: String, mapListDatasets: MutableMap<String, String>, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles",
        {
            it?.requestLine?.contains("GET /zosmf/restfiles/ds?dslevel=${datasetName}")
                ?: false
        },
        { MockResponse().setBody(buildResponseListJson(mapListDatasets, handler)) }
    )
}

fun injectMemberContent(testInfo: TestInfo,datasetName:String, memberName:String, memberContent: String="", handler: Boolean = false){
    responseDispatcher.injectEndpoint(
    "${testInfo.displayName}_restfiles_getmember",
    { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}($memberName)") ?: handler },
    { MockResponse().setBody(memberContent) }
    )
}

fun injectPsDatasetContent(testInfo: TestInfo, datasetName:String, psContent:String="", handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_getcontent",
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds/-(TESTVOL)/$datasetName") ?: handler },
        { MockResponse().setBody(psContent) }
    )
}

fun injectAllocateUssFile(ussMaskName:String, ussFileName:String, responseCode: Int = 201, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "allocateUssFile_restfiles",
        { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMaskName/$ussFileName") ?: handler },
        { MockResponse().setResponseCode(responseCode) }
    )
}
fun injectListAllUssFiles(mapListUssFiles:  Map<String, String>, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "listAllUssFiles_restfiles",
        { it?.requestLine?.contains("GET /zosmf/restfiles/fs?path") ?: handler },
        { MockResponse().setBody(buildResponseListJson(mapListUssFiles, true)) }
    )
}

fun injectTestInfoRestTopology(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_resttopology",
        { it?.requestLine?.contains("zosmf/resttopology/systems") ?: handler },
        { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
}


fun injectTestInfo(testInfo: TestInfo,handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_info",
        { it?.requestLine?.contains("zosmf/info") ?: handler },
        { MockResponse().setBody(responseDispatcher.readMockJson("infoResponse") ?: "") }
    )
}

fun injectInvalidUrlPortInfo(testInfo: TestInfo,testPort:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_info",
        { it?.requestLine?.contains("zosmf/info") ?: handler },
        { MockResponse().setBody("Invalid URL port: \"${testPort}\"") }
    )
}

fun injectEmptyZosmfRestfilesPath(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_second",
        { it?.requestLine?.contains("zosmf/restfiles/") ?: handler },
        { MockResponse().setBody("{}") }
    )
}
//class InjectDispatcher: MockResponseDispatcher()  {
//
//
//    fun injectAllocationResultPo(
//        recordFormatShort: String,
//        dsName:String,
//        dsOrganisationShort:String,
//        recordLength:Int,
//        handler: Boolean = false){
//
//        responseDispatcher.injectEndpoint(
//            "testAllocateValid${dsOrganisationShort}Datasets_${recordFormatShort}_restfiles",
//            { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${dsName}") ?: handler },
//            { MockResponse().setBody("{\"dsorg\":\"${dsOrganisationShort}\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":1,\"recfm\":\"${recordFormatShort}\",\"blksize\":3200,\"lrecl\":${recordLength}}") }
//        )
//
//    }
//    fun injectRenameMember(testInfo: TestInfo, datasetName: String, memberNameNew: String, memberNameOld: String, handler: Boolean=false){
//        responseDispatcher.injectEndpoint(
//            "${testInfo.displayName}_restfiles_rename_member",
//            { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberNameNew})") ?: false },
//            { MockResponse().setBody("{\"request\":\"rename\",\"from-dataset\":{\"dsn\":\"${datasetName}\",\"member\":\"${memberNameOld}\"}}") }
//        )
//    }
//}