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
package workingset.testutils

import okhttp3.mockwebserver.MockResponse
import auxiliary.*
import org.junit.jupiter.api.TestInfo
import workingset.*
import java.io.File

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

    val bodyHeadr = "{\"items\":["
    val bodyTell = "],\"returnedRows\": ${listElem.size},\"JSONversion\": 1}"
    val memebersString = listElem.joinToString(separator = ",") { "{\"member\": \"$it\"}" }

    responseDispatcher.injectEndpoint(
        requestName,
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: false && isFirstRequest},
        { MockResponse().setBody(bodyHeadr + memebersString + bodyTell)}
    )
}

fun injectJobList(testInfo: TestInfo, datasetName: String, listMembersInDataset: MutableList<String>, isFirstRequest: Boolean= true){//= true){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restfiles_listmembers",
        { it?.requestLine?.contains("GET /zosmf/restfiles/ds/${datasetName}/member") ?: isFirstRequest },
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

//fun injectSingleEmptySpecificMember(testInfo: TestInfo, datasetName: String, memberName:  String, isFirstRequest: Boolean = true, handler: Boolean = false){
//    responseDispatcher.injectEndpoint(
//        "${testInfo.displayName}_${memberName}in${datasetName}_restfiles",
//        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: handler && isFirstRequest },
//        { MockResponse().setBody("") }
//    )
//}

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
                ?: handler
        },
        { MockResponse().setBody(buildResponseListJson(mapListDatasets, handler)) }
    )
}
fun injectTestInfoForPdsDataset(testInfoDisplayedName: String, body: String, pdsName:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfoDisplayedName}_${pdsName}_restfiles",
        { it?.requestLine?.contains("POST /zosmf/restfiles/ds/${pdsName}") ?: handler },
        { MockResponse().setBody(body) }
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

//fun injectListAllUssFiles(
//    ussMask:  String, ussDirName: String, responseCode:Int = 201, handler: Boolean = false) {
//        responseDispatcher.injectEndpoint(
//            "allocateUssDir_restfiles",
//            { it?.requestLine?.contains("POST /zosmf/restfiles/fs$ussMask/$ussDirName") ?: handler },
//            { MockResponse().setResponseCode(responseCode) }
//        )
//}



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

fun injectInvalidCreeds(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_resttopology",
        { it?.requestLine?.contains("zosmf/resttopology/systems") ?: handler },
        { MockResponse().setResponseCode(401) }
    )
}

fun injectInvalidUrlPortInfo(testInfo: TestInfo,testPort:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_info",
        { it?.requestLine?.contains("zosmf/info") ?: handler },
        { MockResponse().setBody("Invalid URL port: \"${testPort}\"") }
    )
}

fun injectInvalidCertificate(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_info",
        { it?.requestLine?.contains("zosmf/info") ?: handler },
        { MockResponse().setBody(CERTIFICATE_ERROR) }
    )
}

fun injectInvalidInfo(testInfo: TestInfo, text:String, handler: Boolean = false): String {
    val name = "${testInfo.displayName}_info"
    responseDispatcher.injectEndpoint(
        name,
        { it?.requestLine?.contains("zosmf/info") ?: handler },
        { MockResponse().setBody(text) }
    )
    return name
}

fun injectEmptyZosmfRestfilesPath(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_second",
        { it?.requestLine?.contains("zosmf/restfiles/") ?: handler },
        { MockResponse().setBody("{}") }
    )
}
fun injectErrorFileCreating(ussMask: String, ussFileName:String,status: Int, errorText: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "CreateFileWhenDirWithTheSameNameExists_restfiles",
        { it?.requestLine?.contains("POST /zosmf/restfiles/fs${ussMask}/${ussFileName}") ?: handler },
        {
            MockResponse().setResponseCode(status)
                .setBody(errorText)
        }
    )
}

fun injectJobToSubmit(testInfo: TestInfo,jobName: String, jobStatus: JobStatus, body: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restjobs",
        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs") ?: handler },
        {
            MockResponse().setBody(body)
                .setBody(setBodyJobSubmit(jobName, jobStatus))
        }
    )
}

fun injectJobToCanceled(testInfo: TestInfo,jobName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_cancel",
        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: handler },
        {
            MockResponse().setBody("{\"request\":\"CANCEL\",\"version\":\"2.0\"}")
                .setBody(setBodyJobCancelled(jobName))
        }
    )
}

fun injectJobToRelease(testInfo: TestInfo,jobName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_release",
        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: handler },
        {
            MockResponse().setBody("{\"request\":\"RELEASE\",\"version\":\"2.0\"}")
                .setBody(setBodyJobHoldOrReleased(jobName))
        }
    )
}

fun injectJobLogHttp(testInfo: TestInfo,jobName: String, fileName: String, rc: String="CC 0000", handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_log_after_action",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files HTTP") ?: handler },
        { MockResponse().setBody(
            replaceInJson(fileName,
                mapOf(
                Pair("hostName", mockServer.hostName),
                Pair("port", mockServer.port.toString()),
                Pair("jobName", jobName),
                Pair("retCode", rc),
                Pair("jobStatus", JobStatus.OUTPUT.name))
            )
        )
        }
    )
}

fun injectJobLog2Records(testInfo: TestInfo,jobName: String, filePath: String, fileDirAndName:String,  handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_cancel_files2",
        {
            it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380/files/2/records")
                ?: handler
        },
        {
            MockResponse().setBody(
                File(filePath + fileDirAndName).readText(
                    Charsets.UTF_8
                )
            )
        }
    )
}

fun injectJobLogCanceledJobNotHttp(testInfo: TestInfo,jobName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_cancel_files3",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: handler },
        { MockResponse().setBody(setBodyJobCancelled(jobName)) }
    )
}

fun injectJobLogHoldOrReleased(testInfo: TestInfo, jobName: String, action: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_hold",
        { it?.requestLine?.contains("PUT /zosmf/restjobs/jobs/$jobName/JOB07380 ") ?: handler },
        {
            MockResponse().setBody("{\"request\":\"$action\",\"version\":\"2.0\"}")
                .setBody(setBodyJobHoldOrReleased(jobName))
        }
    )
}


fun injectJobLogHoldOrReleasedJobNotHttp(testInfo: TestInfo,jobName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_cancel_files3",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: handler },
        //TODO setBodyJobSubmit fun called for released functionality. need recheck it in app and
        //modify
        { MockResponse().setBody(setBodyJobSubmit(jobName, JobStatus.INPUT)) }
    )
}

fun injectJobLogHoldOrReleasedJobNotHttpAsSubmit(testInfo: TestInfo,jobName: String, fileName:String, rc: String = "CC 0000", handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_release_files3",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/JOB07380?") ?: handler },
        { MockResponse().setBody(replaceInJson(fileName,
            mapOf(
                Pair("hostName", mockServer.hostName),
                Pair("port", mockServer.port.toString()),
                Pair("jobName", jobName),
                Pair("retCode", rc),
                Pair("USER", ZOS_USERID),
                Pair("jobStatus", JobStatus.OUTPUT.name))))}
    )
}

fun injectMemberFromFile(testInfo: TestInfo, datasetName: String, memberName: String, filePath: String,fileName:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_fill_${memberName}_restfiles",
        { it?.requestLine?.contains("PUT /zosmf/restfiles/ds/${datasetName}(${memberName})") ?: handler },
        { MockResponse().setBody(File(filePath + fileName).readText(Charsets.UTF_8)) }
    )
}

fun injectJobDetailsFromJson(testInfo: TestInfo, jobName: String, jobId: String, rc: String= RC_0000, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restjobs_files",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/$jobId/files HTTP") ?: handler },
        { MockResponse().setBody(replaceInJson("getSpoolFiles",
            mapOf(Pair("hostName", mockServer.hostName),
                Pair("port", mockServer.port.toString()),
                Pair("jobName", jobName),
                Pair("retCode",rc),
                Pair("jobStatus", JobStatus.OUTPUT.name)))) }
    )

    }fun injectJobDetailsFromJsonNotHttp(testInfo: TestInfo, jobName: String, jsonName: String, jobStatus: JobStatus, rc: String = RC_0000,handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restjobs_files3",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/$UNIVERSAL_JOB_ID?") ?: handler },
        { MockResponse().setBody(replaceInJson(jsonName,
            mapOf(Pair("hostName", mockServer.hostName),
                Pair("port", mockServer.port.toString()),
                Pair("jobName", jobName), Pair("retCode", rc),
                Pair("jobStatus", jobStatus.name)))) }
    )
}


fun injectSpoolTextFromFile(testInfo: TestInfo, jobName: String, jobId: String, filePath: String, fileName:String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restjobs_files2",
        { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/$jobName/$jobId/files/2/records") ?: handler },
        { MockResponse().setBody(File(filePath + fileName).readText(Charsets.UTF_8)) }
    )
}
fun injectEmptySpoolFiles(testInfo: TestInfo, jobName: String, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
            "${testInfo.displayName}_restjobs3",
            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/${jobName}/JOB07380/files/") ?: handler },
            { MockResponse().setBody("").setResponseCode(200) }
    )
}

fun injectEmptyJobList(testInfo: TestInfo, handler: Boolean = false){
    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_restjobs",
        { it?.requestLine?.contains("/zosmf/restjobs/jobs") ?: handler },
        { MockResponse().setBody("[]") }
    )
}

fun injectSaccessedPurge(testInfo: TestInfo,jobName:String, handler: Boolean = false): String {
    val name = "${testInfo.displayName}_delete_${jobName}"
    responseDispatcher.injectEndpoint(
        name,
        { it?.requestLine?.contains("DELETE /zosmf/restjobs/jobs/${jobName}/JOB07380") ?: handler},
        { MockResponse().setBody("{}").setBody(setBodyJobPurged(jobName)) }
    )
    return name
}
fun injectFailPurge(testInfo: TestInfo, jobName:String, handler: Boolean = false): String {
    val name = "${testInfo.displayName}_delete_${jobName}"
    responseDispatcher.injectEndpoint(
        name,
        { it?.requestLine?.contains("DELETE /zosmf/restjobs/jobs/${jobName}/JOB07380") ?: handler },
        { MockResponse().setBody("{}").setResponseCode(400) }
    )
    return name
}

fun injectOwnerPrefixJobDetails(testInfo: TestInfo, jobName:String, owner: String, rc: String=RC_0000, handler: Boolean = false): String {
    val name = "${testInfo.displayName}_${jobName}_${owner}_restjobs"

    responseDispatcher.injectEndpoint(
        name,
        {
            it?.requestLine?.contains("/zosmf/restjobs/jobs?owner=$owner&prefix=$jobName")
            ?: handler
        },
        { MockResponse().setBody(replaceInJson(
            FILE_NAME_GET_JOB,
            mapOf(
                Pair("hostName", mockServer.hostName),
                Pair("port", mockServer.port.toString()),
                Pair("jobName", jobName),
                Pair("USER", owner),
                Pair("retCode", rc),
                Pair("jobStatus", "OUTPUT")))).setResponseCode(200) }
        )
    return name
}
fun injectOwnerPrefixJobNoDetails(testInfo: TestInfo, jobName:String, owner: String, handler: Boolean = false): String {
    val name = "${testInfo.displayName}_${jobName}_${owner}_restjobs_empt"

    responseDispatcher.injectEndpoint(
        "${testInfo.displayName}_${jobName}_restjobs2",
        {
            it?.requestLine?.contains("/zosmf/restjobs/jobs?owner=$owner&prefix=${jobName}")
            ?: handler
        },
        { MockResponse().setBody("[]") }
    )
    return name
}

//fun injectSpoolFileContent(testInfo: TestInfo, jobName:String, spoolFileName: String, handler: Boolean = false): String {
//    val name = "${testInfo.displayName}_${jobName}"
//    responseDispatcher.injectEndpoint(
//            "listSpoolFiles_restfiles",
//            { it?.requestLine?.contains("GET /zosmf/restjobs/jobs/") ?: false },
//            { MockResponse().setBody(
//                    replaceInJson("getSingleSpoolFile",
//                            mapOf(Pair("jobName", jobName), Pair("spoolFileName", spoolFileName)))) })
//    return name
//}
