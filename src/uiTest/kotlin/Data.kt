/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset

import auxiliary.*

//Global constants
const val PROJECT_NAME = "untitled"
const val REMOTE_ROBOT_URL = "http://127.0.0.1"

//Allocation unit shorts
const val TRACKS_ALLOCATION_UNIT_SHORT = "TRK"

//Button text
const val NO_TEXT = "No"
const val OK_TEXT = "OK"
const val YES_TEXT = "Yes"
const val DELETE_TEXT = "Delete"
const val CANCEL_TEXT = "Cancel"

// Datasets types
const val PO_ORG_FULL = "Partitioned (PO)"
const val PO_ORG_SHORT = "PO"
const val SEQUENTIAL_ORG_FULL = "Sequential (PS)"
const val SEQUENTIAL_ORG_SHORT = "PS"
const val POE_ORG_FULL = "Partitioned Extended (PO-E)"
const val POE_ORG_SHORT = "POE"
const val PDS_TYPE = "PDS"

//rename dataset
const val DATASET_FOR_RENAME_PROPERTY = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255, \"migr\":false}"

//record formats
const val F_RECORD_FORMAT_SHORT = "F"
const val FB_RECORD_FORMAT_SHORT = "FB"
const val V_RECORD_FORMAT_SHORT = "V"
const val VA_RECORD_FORMAT_SHORT = "VA"
const val VB_RECORD_FORMAT_SHORT = "VB"

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

//Errors messages
const val invalidDatasetNameConstant = "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or " +
        "national (# @ $). The remaining seven characters are either alphabetic, numeric (0 - 9), national, " +
        "a hyphen (-). Name segments are separated by a period (.)"
const val numberGreaterThanOneMsg = "Enter a number greater than or equal to 1"
const val enterValueInCorrectRangeFromOneMsg = "Please enter a number from 1 to 2,147,483,646"
const val enterValueInCorrectRangeFromZeroMsg = "Please enter a number from 0 to 2,147,483,646"
const val enterPositiveNumberMsg = "Enter a positive number"
const val MEMBER_ALREADY_EXISTS = "Member already exists"
const val RENAME_MEMBER_FAILED = "Rename member failed"
const val MEMBER_IN_USE = "Member in use"
const val INVALID_MEMBER_NAME_MESSAGE = "Member name should contain only A-Z a-z 0-9 or national characters"
const val INVALID_MEMBER_NAME_BEGINNING_MESSAGE = "Member name should start with A-Z a-z or national characters"
const val MEMBER_EMPTY_NAME_MESSAGE = "This field must not be blank"
const val DATASET_INVALID_SECTION_MESSAGE =
    "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or national (# @ \$). The remaining seven characters are either alphabetic, numeric (0 - 9), national, a hyphen (-). Name segments are separated by a period (.)"
const val DATASET_NAME_LENGTH_MESSAGE = "Dataset name cannot exceed 44 characters"
const val ERROR_IN_PLUGIN = "Error in plugin For Mainframe"
const val DATA_SET_RENAME_FAILED_MSG = "Data set rename failed"
const val NO_ITEMS_FOUND_MSG = "No items found"
const val IDENTICAL_MASKS_MESSAGE = "You cannot add several identical masks to table"
const val EMPTY_DATASET_MESSAGE = "You are going to create a Working Set that doesn't fetch anything"
const val INVALID_URL_PORT = "Invalid URL port: \"%s\""
const val UNIQUE_WORKING_SET_NAME = "You must provide unique working set name. Working Set %s already exists."
const val UNIQUE_MASK = "You must provide unique mask in working set. Working Set \"%s\" already has mask - %s"

//Migrate options:
const val HMIGRATE_MIGRATE_OPTIONS = "hmigrate"
const val HRECALL_MIGRATE_OPTIONS = "hrecall"


//dialog text\patterns

const val datasetHasBeenCreated = "Dataset %s Has Been Created  "


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
const val RC_8_TEXT = "EDC5051I An error occurred when renaming a file."


//ws names
const val WS_NAME_WS_1 = "WS1"
const val WS_NAME_WS_2 = "WS2"
const val WS_NAME_WS_3 = "WS3"
const val WS_NAME_WS_4 = "WS4"
const val WS_NAME_WS_5 = "WS5"
const val WS_NAME_WS_6 = "WS6"
const val WS_NAME_WS_7 = "WS7"
const val WS_NAME_WS_8 = "WS8"
const val WS_NAME_WS_9 = "WS9"
const val WS_NAME_WS_10 = "WS10"


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

val zosUserDatasetMask = "$ZOS_USERID.*".uppercase()
val zosUserDatasetMaskDoubleStar = "$ZOS_USERID.**".uppercase()
const val ussMask = "/u/$ZOS_USERID"
const val defaultNewUssMask = "/etc/ssh"
val singleMask = Pair("$ZOS_USERID.*", "z/OS")
val singleUssMask = Pair(ussMask, "USS")
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
const val PORT_104431 = "104431"
const val PORT_104431_AND_1 = "1044311"


//dialogs
const val EDIT_WORKING_SET = "Edit Working Set"
const val CREATE_MASK_DIALOG = "Create Mask"
const val ADD_WORKING_SET_DIALOG = "Add Working Set"
const val RENAME_DATASET_MASK_DIALOG = "Edit Mask"
const val ALLOCATE_DATASET_DIALOG = "Allocate Dataset"
