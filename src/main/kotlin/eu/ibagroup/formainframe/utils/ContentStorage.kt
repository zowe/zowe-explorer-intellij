package eu.ibagroup.formainframe.utils

import com.intellij.util.io.storage.CapacityAllocationPolicy
import com.intellij.util.io.storage.Storage

class ContentStorage(name: String) : Storage(
  cachesDir.absoluteFile.resolve("$name.dat").toPath(),
  CapacityAllocationPolicy.FIVE_PERCENT_FOR_GROWTH
) {

  fun getLength(record: Int): Long {
    return withReadLock<Long, Throwable> { readBytes(record).size.toLong() }
  }

  fun getBytes(record: Int): ByteArray {
    return withReadLock<ByteArray, Throwable> { readBytes(record) }
  }

  fun hasRecord(record: Int): Boolean {
    return withReadLock<Boolean, Throwable> {
      myRecordsTable.getAddress(record) >= 0
    }
  }

}