package eu.ibagroup.formainframe.explorer.config

import eu.ibagroup.r2z.AllocationUnit
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat

enum class Presets (val type: String) {
  CUSTOM_DATASET("CUSTOM DATASET") {
    override fun initDataClass() : PresetCustomDataset {
      return PresetCustomDataset()
    }
    override fun toString(): String {
      return type
    }
  },
  SEQUENTIAL_DATASET("SEQUENTIAL DATASET") {
    override fun initDataClass() : PresetSeqDataset {
      return PresetSeqDataset()
    }
    override fun toString(): String {
      return type
    }
  },
  PDS_DATASET("PDS DATASET") {
    override fun initDataClass() : PresetPdsDataset {
      return PresetPdsDataset()
    }
    override fun toString(): String {
      return type
    }
  },
  PDS_WITH_EMPTY_MEMBER("PDS_WITH_MEMBER_DATASET") {
    override fun initDataClass() : PresetPdsDataset {
      return PresetPdsDataset()
    }
    override fun toString(): String {
      return type
    }
  },
  PDS_WITH_SAMPLE_JCL_MEMBER("PDS_WITH_JCL_MEMBER_DATASET") {
    override fun initDataClass() : PresetPdsDataset {
      return PresetPdsDataset()
    }
    override fun toString(): String {
      return type
    }
  };

  abstract fun initDataClass() : PresetType
}

interface PresetType {
  val presetCustom : PresetCustomDataset?
  val presetSeq : PresetSeqDataset?
  val presetPds : PresetPdsDataset?
}

data class PresetCustomDataset(
  val datasetOrganization : DatasetOrganization = DatasetOrganization.PS,
  val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  val primaryAllocation : Int = 0,
  val secondaryAllocation : Int = 0,
  val directoryBlocks : Int = 0,
  val recordFormat : RecordFormat = RecordFormat.FB,
  val recordLength : Int = 0,
  val blockSize : Int = 0,
  val averageBlockLength : Int = 0
) : PresetType {
  override val presetCustom: PresetCustomDataset = this
  override val presetSeq: PresetSeqDataset? = null
  override val presetPds: PresetPdsDataset? = null
}

data class PresetSeqDataset(
  val datasetOrganization : DatasetOrganization = DatasetOrganization.PS,
  val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  val primaryAllocation : Int = 10,
  val secondaryAllocation : Int = 5,
  val directoryBlocks : Int = 0,
  val recordFormat : RecordFormat = RecordFormat.FB,
  val recordLength : Int = 80,
  val blockSize : Int = 8000,
  val averageBlockLength : Int = 0
) : PresetType {
  override val presetCustom: PresetCustomDataset? = null
  override val presetSeq: PresetSeqDataset = this
  override val presetPds: PresetPdsDataset? = null
}

data class PresetPdsDataset(
  val datasetOrganization : DatasetOrganization = DatasetOrganization.PO,
  val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  val primaryAllocation : Int = 100,
  val secondaryAllocation : Int = 40,
  val directoryBlocks : Int = 10,
  val recordFormat : RecordFormat = RecordFormat.FB,
  val recordLength : Int = 80,
  val blockSize : Int = 32000,
  val averageBlockLength : Int = 0,
) : PresetType {
  override val presetCustom: PresetCustomDataset? = null
  override val presetSeq: PresetSeqDataset? = null
  override val presetPds: PresetPdsDataset = this
}

fun getSampleJclMemberContent() : String {
  return "//* THE SAMPLE JOB WHICH RUNS A REXX PGM IN TSO/E ADDRESS SPACE\n" +
      "//MYJOB    JOB MSGCLASS=A,MSGLEVEL=1,NOTIFY=&SYSUID\n" +
      "//*-------------------------------------------------------------------\n" +
      "//RUNPROG  EXEC PGM=IKJEFT01\n" +
      "//*\n" +
      "//*        RUN OUR REXX PROGRAM CALLED HELLO IN A TSO/E ENVIRONMENT\n" +
      "//*\n" +
      "//SYSPROC  DD DSN=USER.CLIST,DISP=SHR\n" +
      "//SYSEXEC  DD DSN=USER.EXEC,DISP=SHR\n" +
      "//SYSTSIN  DD *\n" +
      "   HELLO\n" +
      "//SYSTSPRT DD SYSOUT=*\n" +
      "//SYSPRINT DD SYSOUT=*\n" +
      "//*--------------------------------------------------------------------\n"
}
