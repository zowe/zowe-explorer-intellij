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

package eu.ibagroup.formainframe.explorer.config

import org.zowe.kotlinsdk.AllocationUnit
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.RecordFormat

/**
 * Enum class represents possible preset choices for a dataset
 * @param type - a string representation of a chosen preset
 */
enum class Presets(private val type: String) {
  CUSTOM_DATASET("Custom Dataset"),
  SEQUENTIAL_DATASET("Sequential Dataset"),
  PDS_DATASET("PDS Dataset"),
  PDS_WITH_EMPTY_MEMBER("PDS with empty member Dataset"),
  PDS_WITH_SAMPLE_JCL_MEMBER("PDS with sample JCL member Dataset");

  override fun toString(): String {
    return type
  }

  companion object {
    /**
     * Data class initialization for a chosen preset
     */
    fun initDataClass(preset: Presets): PresetType {
      return when (preset) {
        CUSTOM_DATASET -> PresetCustomDataset()
        SEQUENTIAL_DATASET -> PresetSeqDataset()
        else -> PresetPdsDataset()
      }
    }
  }
}

/**
 * Interface which represents a preset default fields for a chosen data class
 */
interface PresetType {
  val datasetOrganization : DatasetOrganization
  val spaceUnit : AllocationUnit
  val primaryAllocation : Int
  val secondaryAllocation : Int
  val directoryBlocks : Int
  val recordFormat : RecordFormat
  val recordLength : Int
  val blockSize : Int
  val averageBlockLength : Int
}

/**
 * Data class which represents a custom dataset
 */
data class PresetCustomDataset(
  override val datasetOrganization : DatasetOrganization = DatasetOrganization.PS,
  override val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  override val primaryAllocation : Int = 0,
  override val secondaryAllocation : Int = 0,
  override val directoryBlocks : Int = 0,
  override val recordFormat : RecordFormat = RecordFormat.FB,
  override val recordLength : Int = 0,
  override val blockSize : Int = 0,
  override val averageBlockLength : Int = 0
) : PresetType

/**
 * Data class which represents a sequential dataset
 */
data class PresetSeqDataset(
  override val datasetOrganization : DatasetOrganization = DatasetOrganization.PS,
  override val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  override val primaryAllocation : Int = 10,
  override val secondaryAllocation : Int = 5,
  override val directoryBlocks : Int = 0,
  override val recordFormat : RecordFormat = RecordFormat.FB,
  override val recordLength : Int = 80,
  override val blockSize : Int = 8000,
  override val averageBlockLength : Int = 0
) : PresetType

/**
 * Data class which represents a PDS dataset
 */
data class PresetPdsDataset(
  override val datasetOrganization : DatasetOrganization = DatasetOrganization.PO,
  override val spaceUnit : AllocationUnit = AllocationUnit.TRK,
  override val primaryAllocation : Int = 100,
  override val secondaryAllocation : Int = 40,
  override val directoryBlocks : Int = 10,
  override val recordFormat : RecordFormat = RecordFormat.FB,
  override val recordLength : Int = 80,
  override val blockSize : Int = 32000,
  override val averageBlockLength : Int = 0,
) : PresetType

/**
 * Open function to get the content of the default JCL member
 */
fun getSampleJclMemberContent(defaultUser: String) : String {
  return "//* THE SAMPLE JOB WHICH DO NOTHING\n" +
      "//MYJOB    JOB MSGCLASS=A,MSGLEVEL=1,NOTIFY=${defaultUser}\n" +
      "//*-------------------------------------------------------------------\n" +
      "//RUN  EXEC PGM=IEFBR14\n" +
      "//*\n" +
      "//SYSTSPRT DD SYSOUT=*\n" +
      "//SYSPRINT DD SYSOUT=*\n" +
      "//*--------------------------------------------------------------------\n"
}
