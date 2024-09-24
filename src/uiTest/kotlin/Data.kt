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
package workingset

import auxiliary.*

//Global constants
enum class JobAction { SUBMIT, CANCEL, HOLD, RELEASE, PURGED, ERROR_PURGED}
const val PROJECT_NAME = "untitled"


object Constants {
    val ideLaunchFolder: String = System.getProperty("ideLaunchFolder")
    val forMainframePath: String = System.getProperty("forMainframePath")
    val remoteRobotUrl: String = System.getProperty("remoteRobotUrl")
    val ideaBuildVersionForTest: String = System.getProperty("ideaBuildVersionForTest")
    val ideaVersionForTest: String = System.getProperty("ideaVersionForTest")
    val robotServerForTest: String = System.getProperty("robotServerForTest")
}

//Allocation unit shorts
const val TRACKS_ALLOCATION_UNIT_SHORT = "TRK"

//Button text
const val NO_TEXT = "No"
const val OK_TEXT = "OK"
const val PROCEED_TEXT = "Proceed"
const val YES_TEXT = "Yes"
const val DELETE_TEXT = "Delete"
const val CANCEL_TEXT = "Cancel"

const val WORKING_SETS = "Working Sets"
const val JES_WORKING_SETS = "JES Working Sets"
const val CONNECTIONS = "Connections"

const val PREFIX_WORD = "Prefix"

// Datasets types
const val PO_ORG_FULL = "Partitioned (PO)"
const val PO_ORG_SHORT = "PO"
const val SEQUENTIAL_ORG_FULL = "Sequential (PS)"
const val SEQUENTIAL_ORG_SHORT = "PS"
const val POE_ORG_FULL = "Partitioned Extended (PO-E)"
//const val POE_ORG_SHORT = "POE"
const val PDS_TYPE = "PDS"

//rename dataset
const val DATASET_FOR_RENAME_PROPERTY = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255, \"migr\":false}"

//record formats
const val F_RECORD_FORMAT_SHORT = "F"
const val FB_RECORD_FORMAT_SHORT = "FB"
const val V_RECORD_FORMAT_SHORT = "V"
const val VA_RECORD_FORMAT_SHORT = "VA"
const val VB_RECORD_FORMAT_SHORT = "VB"

//action sub menu points
const val FILE_POINT_TEXT = "File"
const val DIRECTORY_POINT_TEXT = "Directory"

//action menu points
const val REFRESH_POINT_TEXT = "Refresh"
const val NEW_POINT_TEXT = "New"
const val WORKING_SET = "Working Set"
const val DATASET_POINT_TEXT = "Dataset"
const val MIGRATE_POINT_TEXT = "Migrate"
const val RECALL_POINT_TEXT = "Recall"
const val RENAME_POINT_TEXT = "Rename"
const val EDIT_POINT_TEXT = "Edit"
const val PROPERTIES_POINT_TEXT = "Properties"
const val MASK_POINT_TEXT = "Mask"
const val JOBS_FILTER = "Jobs Filter"

//JES action menu points
const val JES_WORKING_SET_POINT = "JES Working Set"
const val PURGE_JOB_POINT = "Purge Job"
const val SUBMIT_JOB_POINT = "Submit Job"

//Errors messages
const val invalidDatasetNameConstant = "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or " +
        "national (# @ $). The remaining seven characters are either alphabetic, numeric (0 - 9), national, " +
        "a hyphen (-). Name segments are separated by a period (.)"

const val enterValueInCorrectRangeFromOneMsg = "Please enter a number from 1 to 2,147,483,646"
const val enterValueInCorrectRangeFromZeroMsg = "Please enter a number from 0 to 2,147,483,646"
const val MEMBER_ALREADY_EXISTS = "ISRZ002 Member already exists - Directory already contains the specified member name."
const val RENAME_MEMBER_FAILED = "Rename member failed"
const val MEMBER_IN_USE = "Member in use"
const val INVALID_MEMBER_NAME_MESSAGE = "Member name should contain only A-Z a-z 0-9 or national characters"
const val INVALID_MEMBER_NAME_BEGINNING_MESSAGE = "Member name should start with A-Z a-z or national characters"
const val MEMBER_EMPTY_NAME_MESSAGE = "This field must not be blank"
const val DATASET_INVALID_SECTION_MESSAGE =
    "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or national (# @ \$). The remaining seven characters are either alphabetic, numeric (0 - 9), national, a hyphen (-). Name segments are separated by a period (.)"
const val DATASET_NAME_LENGTH_MESSAGE = "Dataset name cannot exceed 44 characters"
//const val ERROR_IN_PLUGIN = "Error in plugin For Mainframe"
const val DATA_SET_RENAME_FAILED_MSG = "Data set rename failed"
//const val NO_ITEMS_FOUND_MSG = "No items found"
const val IDENTICAL_MASKS_MESSAGE = "You cannot add several identical masks to table"
const val EMPTY_DATASET_MESSAGE = "You are going to create a Working Set that doesn't fetch anything"
const val INVALID_URL_PORT = "Invalid URL port: \"%s\""
const val UNIQUE_WORKING_SET_NAME = "You must provide unique working set name. Working Set %s already exists."
const val UNIQUE_JOB_FILTER = "Job Filter with provided data already exists."
const val DUBLICETA_MEMBER_NAME_ERROR = "Field value matches the previous one"


const val UNIQUE_MASK = "You must provide unique mask in working set. Working Set \"%s\" already has mask - %s"

const val FILE_NAME_LENGTH_MESSAGE = "Filename must not exceed 255 characters."
const val FILE_RESRVED_SYMBOL_MESSAGE = "Filename must not contain reserved '/' symbol."
//const val FILE_ALREADY_EXIST_MESSAGE = "The specified file already exists"
const val MUST_PROVIDE_CONNECTION_MESSAGE = "You are going to create a Working Set that doesn't fetch anything"
var hostUnknowableError = "Этот хост неизвестен (%s)"
var duplicateConnectionNameError = "You must provide unique connection name. Connection %s already exists."
var INVALID_CREEDS_ERROR = "Credentials are not valid"
var CERTIFICATE_ERROR = "Unable to find valid certification path to requested "
var INVALID_URL_ERROR = "Please provide a valid URL to z/OSMF. Example: https://myhost.com:10443"
val UNABLE_FIND_VALID_CERTIFICATE = "Unable to find valid certification path to requested target"
val EXIST_DEPENDED_WS_ERROR = "The following Files working sets use selected connections:%s."
val EXIST_DEPENDED_JWS_ERROR = "The following JES working sets use selected connections:%s."

const val ABSENT_ERROR_MSG = "Failed to find 'ComponentFixture' by '//div[@class='LinkLabel']'"

const val NOTHING_TO_SHOW_MSG = "Nothing to show"

//Migrate options:
const val HMIGRATE_MIGRATE_OPTIONS = "hmigrate"
//const val HRECALL_MIGRATE_OPTIONS = "hrecall"




//bad alloc params cases
data class InvalidAllocate(
   val wsName: String,
   val datasetName: String,
   val datasetOrganization: String,
   val allocationUnit: String,
   val primaryAllocation: Int,
   val secondaryAllocation: Int,
   val directory: Int,
   val recordFormat: String,
   val recordLength: Int,
   val blockSize: Int,
   val averageBlockLength: Int,
   val message: String
)

val invalidDatasetNameParams = InvalidAllocate(
    "", "A23456789.A", PO_ORG_FULL, "TRK", 10, 1,
    1,    "FB", 80, 3200, 0, invalidDatasetNameConstant
)
val invalidPrimaryAllocationParams = InvalidAllocate(
    "", "A23.A23", PO_ORG_FULL, "TRK", -2, 0, 1,
            "FB", 80, 3200, 0, enterValueInCorrectRangeFromOneMsg)

val invalidDirectoryParams = InvalidAllocate(
            "", "A23.A23", PO_ORG_FULL, "TRK", 10, 0, 0,
            "FB", 80, 3200, 0,enterValueInCorrectRangeFromOneMsg
        )
val invalidRecordLengthParams = InvalidAllocate(
            "", "A23.A23", PO_ORG_FULL, "TRK", 10, 0, 1,
            "FB", 0, 3200, 0,enterValueInCorrectRangeFromOneMsg
        )
val invalidSecondaryAllocationParams = InvalidAllocate(
            "", "A23.A23", PO_ORG_FULL, "TRK", 10, -10, 1,
            "FB", 80, 3200, 0, enterValueInCorrectRangeFromZeroMsg
        )
val invalidBlockSizeParams = InvalidAllocate(
            "", "A23.A23", PO_ORG_FULL, "TRK", 10, 0, 1,
            "FB", 80, -1, 0, enterValueInCorrectRangeFromZeroMsg
        )
val invalidAverageBlockLengthParams = InvalidAllocate(
            "", "A23.A23", PO_ORG_FULL, "TRK", 10, 0, 1,
            "FB", 80, 3200, -1, enterValueInCorrectRangeFromZeroMsg
        )

val invalidAllocateScenarios = mapOf(
    Pair("invalidDatasetNameParams", invalidDatasetNameParams),
    Pair("invalidPrimaryAllocationParams", invalidPrimaryAllocationParams),
    Pair("invalidDirectoryParams", invalidDirectoryParams),
    Pair("invalidRecordLengthParams", invalidRecordLengthParams),
    Pair("invalidSecondaryAllocationParams", invalidSecondaryAllocationParams),
    Pair("invalidBlockSizeParams", invalidBlockSizeParams),
    Pair("invalidAverageBlockLengthParams", invalidAverageBlockLengthParams),
)

//rename members constants

const val TOO_LONG_MEMBER_NAME = "123456789"
const val INVALID_NAME_VIA_CONTEXT_MENU = "@*"
const val INVALID_INVALID_FIRST_SYMBOL = "**"
const val EMPTY_STRING = ""


//rename dataset name
const val DATA_SET_RENAME_FAILED = "Data set rename failed"


val incorrectRenameMember = mapOf(
    Pair(TOO_LONG_MEMBER_NAME, MEMBER_NAME_LENGTH_MESSAGE),
    Pair(INVALID_NAME_VIA_CONTEXT_MENU, INVALID_MEMBER_NAME_MESSAGE),
    Pair(INVALID_INVALID_FIRST_SYMBOL, INVALID_MEMBER_NAME_BEGINNING_MESSAGE),
    Pair(EMPTY_STRING, MEMBER_EMPTY_NAME_MESSAGE),
)

//member's property
const val NO_MEMBERS = "{\"items\":[],\"returnedRows\":0,\"totalRows\":0,\"moreRows\":null,\"JSONversion\":1}"

//dialog names
const val RENAME_MEMBER_NAME = "Rename Member"
const val RENAME_DATASET_NAME = "Rename Dataset"
const val DELETION_OF_DS_MASK = "Deletion of DS Mask %s"
const val DELETION_OF_USS_PATH_ROOT = "Deletion of Uss Path Root %s"


//RC STRINGS

const val RC_8 = "8.0"
const val RC_4 = "4.0"
const val RC_8_TEXT = "EDC5051I An error occurred when renaming a file."


//ws names
const val WS_NAME_1 = "WS1"
const val WS_NAME_2 = "WS2"
const val WS_NAME_3 = "WS3"
const val WS_NAME_4 = "WS4"
const val WS_NAME_5 = "WS5"
const val WS_NAME_6 = "WS6"
const val WS_NAME_7 = "WS7"
const val WS_NAME_8 = "WS8"
const val WS_NAME_9 = "WS9"
const val WS_NAME_10 = "WS10"
val B_200 = "B".repeat(200)
const val B_200_CONST = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
const val SPEC_SYMBOLS = "B12#$%^&*"

//members content
const val EMPTY_MEMBER_CONTENT = ""
const val SHORT_MEMBER_CONTENT = "content"

//members name
const val MEMBER_NAME_PATTERN = "MEMBER"
const val MEMBER_NAME_1 = "MEMBER"+"1"
const val MEMBER_NAME_2 = "MEMBER"+"2"

//mask types:
const val ZOS_MASK = "z/OS"
const val USS_MASK = "USS"


//masks/mask types combo

val ZOSMF_WORD = "ZOSMFAD"
val zosUserDatasetMask = "$ZOS_USERID.*".uppercase()
val zosUserDatasetMaskDoubleStar = "$ZOS_USERID.**".uppercase()
val ussMask = "/u/${ZOS_USERID.uppercase()}"
const val defaultNewUssMask = "/etc/ssh"
val singleMask = Pair("$ZOS_USERID.*", "z/OS")
val singleUssMask = Pair(ussMask, USS_MASK)
val validZOSMasks = listOf(
    "$ZOS_USERID.*", "$ZOS_USERID.**", "$ZOS_USERID.@#%", "$ZOS_USERID.@#%.*", "Q.*", "WWW.*", maskWithLength44,
    ZOS_USERID
)
val validUSSMasks = listOf("/u", "/etc/ssh", ussMask)

//todo add mask *.* when bug is fixed
val maskMessageMap = mapOf(
    "1$ZOS_USERID.*" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.{!" to ENTER_VALID_DS_MASK_MESSAGE,
    "$ZOS_USERID.A23456789.*" to QUALIFIER_ONE_TO_EIGHT,
    "$ZOS_USERID." to ENTER_VALID_DS_MASK_MESSAGE,
    maskWithLength45 to "Dataset mask length must not exceed 44 characters",
    "$ZOS_USERID.***" to "Invalid asterisks in the qualifier"
)

//ports
const val PORT_10443 = "10443"
const val PORT_104431 = "104431"
const val INVALID_PORT_104431 = PORT_104431
val invalidPort104431 = "Invalid URL port: \"${PORT_104431}\""
const val PORT_104431_AND_1 = "1044311"


//dialogs
const val EDIT_WORKING_SET = "Edit Working Set"
const val CREATE_FILE_UNDER = "Create File under %s"
const val CREATE_DIRECTORY_UNDER = "Create Directory under %s"
const val CREATE_MASK_DIALOG = "Create Mask"
const val ADD_WORKING_SET_DIALOG = "Add Working Set"
const val ADD_CONNECTION_DIALOG = "Add Connection"
const val EDIT_CONNECTION_DIALOG = "Edit Connection"
const val ADD_JES_WORKING_SET_DIALOG = "Add JES Working Set"
const val JOB_PROPERTIES_DIALOG = "Job Properties"
const val CREATE_JOBS_FILTER_DIALOG = "Create Jobs Filter"
const val EDIT_JOBS_FILTER_DIALOG = "Edit Jobs Filter"
const val RENAME_DATASET_MASK_DIALOG = "Edit Mask"
const val ALLOCATE_DATASET_DIALOG = "Allocate Dataset"
const val SETTING_DIALOG = "Setting"


//dialog names
const val ADD_DIALOG_NAME = "Add Connection Dialog"

//permissions
val RWE_TYPES_PERMISSION = "READ_WRITE_EXECUTE"
val R_PERMISSION = "READ"
val RW_TYPES_PERMISSION = "READ_WRITE"

//uss tests contsants
const val USS_FILE_NAME = "testFile"
const val USS_DIR_NAME = "testFolder"

//jobs constants
val RC_0000 = "CC 0000"
var holdWord = "HOLD"
val SPOOL_FILE_CONTENT = "mock/getSpoolFileContentRC00.txt"
val jobMemberCombo = "%s (%s)"

val jobCancelNotification = "%s: %s has been cancelled"
val jobHoldNotification = "%s: %s has been held"
val jobReleseNotification = "%s: %s has been released"
val jobSubmitNotification = "Job %s has been submitted"
val jobPurgeNotification = "%s: %s has been purged"
val jobErrorPurgeNotification = "Error purging %s: %s"

val jobsPanelTabName = "//'%s(%s)'"

val jobExecutedConsoleText = "JOB %s(%s) EXECUTED"
val jobOwnerConsoleText = "OWNER: %s"
val jobReturnCodeConsoleText = "RETURN CODE: %s"

val buttonCancelActionName = "Cancel Job ()"
val buttonHoldActionName = "Hold Job ()"
val buttonReleaseActionName = "Release Job ()"

val UNIVERSAL_JOB_ID = "JOB07380"

val validJobsFilters = listOf(
    Triple("*", "ZOSID", ""),
    Triple("TEST1", "ZOSID", ""),
    Triple("", "", "JOB01234"),
    Triple("TEST*", "ZOSID", ""),
    Triple("TEST**", "ZOSID", ""),
    Triple("TEST***", "ZOSID", ""),
    Triple("TEST1", "ZOSID*", ""),
    Triple("TEST1", "ZOSID**", ""),
    Triple("TEST1", "ZOSID***", ""),
    Triple("TEST***", "ZOSID***", "")
)
val deletedFilters = listOf(
    Triple("TEST1", "ZOSID*", ""),
    Triple("", "", "JOB01234"),
    Triple("TEST*", "ZOSID", ""),
    Triple("TEST**", "ZOSID", ""),
    Triple("TEST***", "ZOSID", ""),
    Triple("TEST1", "ZOSID**", ""),
    Triple("TEST1", "ZOSID***", ""),
    Triple("TEST1", "ZOSID", ""),
    Triple("TEST***", "ZOSID***", "")
)


val invalidJobsFiltersMap = mapOf(
    Pair(Triple("123456789", ZOS_USERID, ""), 1) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("123456789", "A23456789", ""), 1) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("*", "A23456789", ""), 2) to TEXT_FIELD_LENGTH_MESSAGE,
    Pair(Triple("", "", "A23456789"), 3) to JOB_ID_LENGTH_MESSAGE,
    Pair(Triple("", "", "A2"), 3) to JOB_ID_LENGTH_MESSAGE,
    Pair(Triple("@", ZOS_USERID, ""), 1) to TEXT_FIELD_CONTAIN_MESSAGE,
    Pair(Triple("*", "@", ""), 2) to TEXT_FIELD_CONTAIN_MESSAGE,
    Pair(Triple("", "", "@@@@@@@@"), 3) to JOBID_CONTAIN_MESSAGE,
    Pair(Triple("*", ZOS_USERID, "JOB45678"), 1) to PREFIX_OWNER_JOBID_MESSAGE,
    Pair(Triple("*", "", "JOB45678"), 1) to PREFIX_OWNER_JOBID_MESSAGE,
    Pair(Triple("", ZOS_USERID, "JOB45678"), 2) to PREFIX_OWNER_JOBID_MESSAGE
)

val filterAllAndZos = Triple("*", ZOS_USERID, "")
val filterAllAndZosAlt = Triple("*", ZOS_USERID+"*", "")

val prefixAndOwnerPattern = "PREFIX=%s OWNER=%s".uppercase()
val jobIdPattern = "JobID=%s"

val FILE_DATASET_NAME_JOB_NAME = "{\"file\":\"//'%s(%s)'\"}"

//Explorer data
val FILE_EXPLORER_W = "File Explorer"
val JES_EXPLORER_W = "JES Explorer"
val LOADING_TEXT = "loading…"
val ERROR_TEXT = "Error"


// ws name constants
val jwsNameV1 = "JWS1"
val jwsNameV2 = "JWS2"
val jwsNameV3 = "JWS3"
val jwsNameV4 = "JWS4"
val jwsNameV5 = "JWS5"
val jwsNameV6 = "JWS6"
val jwsNameV7 = "JWS7"
val jwsNameV8 = "JWS8"
val jwsNameV9 = "JWS9"
val jwsNameV10 = "JWS10"
val jwsNameV11 = "JWS11"
val jwsNameV12 = "JWS12"
val jwsNameV13 = "JWS13"
val jwsNameV14 = "JWS14"
val jwsNameV15 = "JWS15"
val jwsNameV16 = "JWS16"
val jwsNameV17 = "JWS17"
val jwsNameV18 = "JWS18"
val jwsNameV19 = "JWS19"
val jwsNameV20 = "JWS20"

//files with job content:
val FILE_NAME_GET_SPOOL = "getSpoolFiles"
val FILE_NAME_GET_STATUS = "getStatus"
val FILE_NAME_GET_JOB = "getJob"
val GET_SINGLE_SPOOL_FILE = "getSingleSpoolFile"

//spool fields
val spoolDataTabParams = listOf(
    "record format",
    "byte content",
    "recocrd count",
    "record url",
    "length",
    "subsys",
)

val spoolGeneralTabParams = listOf(
    "jobId",
    "jobName",
    "jobcorrelator",
    "class",
    "id",
    "dd",
    "step",
    "process",
)

val jobDataTabParams = listOf(
    "phase",
    "phaseName",
    "url",
    "filesUrl",
    "executor",
    "reasonNotRunning",
    "submitted",
    "startTime",
    "endTime",
)

val jobGeneralTabParams = listOf(
    "jobId",
    "jobName",
    "subsystem",
    "owner",
    "status",
    "jobType",
    "jobClass",
    "returnCode",
    "correlator",
)
