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

package eu.ibagroup.formainframe.utils

import com.intellij.util.io.storage.CapacityAllocationPolicy
import com.intellij.util.io.storage.Storage

/**
 * Class to represent and handle content storage
 * @param name the storage name
 */
class ContentStorage(name: String) : Storage(
  cachesDir.absoluteFile.resolve("$name.dat").toPath(),
  CapacityAllocationPolicy.FIVE_PERCENT_FOR_GROWTH
) {

  /**
   * Get content storage record bytes length
   * @param record record ID in storage
   */
  fun getLength(record: Int): Long {
    return withReadLock<Long, Throwable> { readBytes(record).size.toLong() }
  }

  /**
   * Get content storage record bytes
   * @param record record ID in storage
   */
  fun getBytes(record: Int): ByteArray {
    return withReadLock<ByteArray, Throwable> { readBytes(record) }
  }

  /**
   * Check if the record exist in content storage
   * @param record record ID to search for
   */
  fun hasRecord(record: Int): Boolean {
    return withReadLock<Boolean, Throwable> {
      myRecordsTable.getAddress(record) >= 0
    }
  }

}
