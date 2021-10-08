/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

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