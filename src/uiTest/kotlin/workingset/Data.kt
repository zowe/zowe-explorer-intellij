package workingset

//BUTTON TEXT
const val NO_TEXT = "No"
const val OK_TEXT = "OK"
const val CANCEL_TEXT = "Cancel"

// Datasets types
const val PARTITION_NAME_FULL = "Partitioned (PO)"
const val PARTITION_NAME_SHORT = "PO"
const val SEQUENTIAL_NAME_FULL = "Sequential (PS)"
const val SEQUENTIAL_NAME_SHORT = "PS"
const val PARTITIONED_EXTENDED_NAME_FULL = "Partitioned Extended (PO-E)"
const val PARTITIONED_EXTENDED_NAME_SHORT = "POE"

//rename dataset
const val DATASET_FOR_RENAME_PROPERTY = "{\"dsorg\":\"PO\",\"alcunit\":\"TRK\",\"primary\":10,\"secondary\":1,\"dirblk\":2,\"recfm\":\"VB\",\"blksize\":6120,\"lrecl\":255}"

//record formats
const val F_RECORD_FORMAT_SHORT = "F"
const val FB_RECORD_FORMAT_SHORT = "FB"
const val V_RECORD_FORMAT_SHORT = "V"
const val VA_RECORD_FORMAT_SHORT = "VA"
const val VB_RECORD_FORMAT_SHORT = "VB"

//action menu points
const val REFRESH_POINT_TEXT = "Refresh"
const val NEW_POINT_TEXT = "New"
const val DATASET_POINT_TEXT = "Dataset"
const val MIGRATE_POINT_TEXT="Migrate"

//Errors messages
val invalidDatasetNameConstant = "Each name segment (qualifier) is 1 to 8 characters, the first of which must be alphabetic (A to Z) or " +
        "national (# @ $). The remaining seven characters are either alphabetic, numeric (0 - 9), national, " +
        "a hyphen (-). Name segments are separated by a period (.)"
val numberGreaterThanOneMsg = "Enter a number greater than or equal to 1"
val enterPositiveNumberMsg = "Enter a positive number"

//Migrate options:
const val HMIGRATE_MIGRATE_OPTIONS = "hmigrate"
const val HRECALL_MIGRATE_OPTIONS = "hrecall"


//dialog text\patterns

val datasetHasBeenCreated = "Dataset %s Has Been Created  "


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
        "", "A23456789.A", PARTITION_NAME_FULL, "TRK", 10, 1,
        1,    "FB", 80, 3200, 0, invalidDatasetNameConstant
)
val invalidPrimaryAllocationParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", -2, 0, 1,
        "FB", 80, 3200, 0, numberGreaterThanOneMsg)

val invalidDirectoryParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", 10, 0, 0,
        "FB", 80, 3200, 0,numberGreaterThanOneMsg
)
val invalidRecordLengthParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", 10, 0, 1,
        "FB", 0, 3200, 0,numberGreaterThanOneMsg
)
val invalidSecondaryAllocationParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", 10, -10, 1,
        "FB", 80, 3200, 0, enterPositiveNumberMsg
)
val invalidBlockSizeParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", 10, 0, 1,
        "FB", 80, -1, 0, enterPositiveNumberMsg
)
val invalidAverageBlockLengthParams = InvalidAllocate(
        "", "A23.A23", PARTITION_NAME_FULL, "TRK", 10, 0, 1,
        "FB", 80, 3200, -1, enterPositiveNumberMsg
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
