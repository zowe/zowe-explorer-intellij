package eu.ibagroup.formainframe.dataops.allocation

import eu.ibagroup.r2z.AllocationUnit
import eu.ibagroup.r2z.CreateDataset
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat

data class DatasetAllocationParams(
  var datasetName: String = "",
  val allocationParameters: CreateDataset = CreateDataset(
    allocationUnit = AllocationUnit.TRK,
    primaryAllocation = 0,
    secondaryAllocation = 0,
    recordFormat = RecordFormat.FB,
    datasetOrganization = DatasetOrganization.PS
  )
)