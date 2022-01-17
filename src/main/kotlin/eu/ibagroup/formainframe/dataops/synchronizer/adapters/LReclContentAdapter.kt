package eu.ibagroup.formainframe.dataops.synchronizer.adapters

import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.r2z.RecordFormat

abstract class LReclContentAdapter<Attributes: FileAttributes>(
  dataOpsManager: DataOpsManager
): MFContentAdapterBase<Attributes>(dataOpsManager) {

  fun RemoteDatasetAttributes.hasVariableFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.V || recordFormat == RecordFormat.VA || recordFormat == RecordFormat.VB
  }

  fun RemoteDatasetAttributes.hasVariablePrintFormatRecords(): Boolean {
    val recordFormat = datasetInfo.recordFormat
    return recordFormat == RecordFormat.VA
  }

  protected fun transferLinesByLRecl (content: ByteArray, lrecl: Int): ByteArray {
    val contentString = String(content)
    val contentRows = contentString.split(Regex("\n|\r|\r\n"))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      if (it.length <= lrecl) {
        resultRows.add(it)
      } else {
        var nextLine = it
        while (nextLine.length > lrecl) {
          resultRows.add(nextLine.slice(IntRange(0, lrecl-1)))
          nextLine = nextLine.slice(IntRange(lrecl, nextLine.length-1))
        }
        resultRows.add(nextLine)
      }
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }

  protected fun removeFirstCharacter (content: ByteArray): ByteArray {
    val contentString = String(content)
    val contentRows = contentString.split(Regex("\n|\r|\r\n"))
    val resultRows = mutableListOf<String>()
    contentRows.forEach {
      resultRows.add(it.slice(IntRange(1, it.length-1)))
    }
    val resultContent = resultRows.joinToString("\n")
    return resultContent.toByteArray()
  }
}
