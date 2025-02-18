/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Uladzislau Kalesnikau
 */

package tests.utils

/** To represent used indexes for datasets */
private var dsIdx = 0

/** Dataset record format */
enum class RecFM {
  F, FB, V, VA, VB, U
}

/** Class to represent both full and short versions of dataset organizations */
data class DsOrgItem(val full: String, val short: String)

/** Dataset organization */
enum class DsOrg(val value: DsOrgItem) {
  PS(DsOrgItem(full="Physical Sequential (PS)", short="PS")),
  PO(DsOrgItem(full="Partitioned Organization (PO)", short="PO")),
  POE(DsOrgItem(full="Partitioned Data Set Extended (PO-E)", short="PO-E"))
}

/** Dataset allocation unit */
enum class AllocUnit {
  TRK, CYL
}

/** Allocate dataset parameters class for the dataset parameters transmission through UI tests */
data class AllocateDatasetParams(
  val name: String,
  val preset: String,
  val dsOrg: DsOrg,
  val unit: AllocUnit,
  val recfm: RecFM,
  val memberName: String? = null,
  val primAlloc: String? = null,
  val secAlloc: String? = null,
  val blksz: String? = null,
  val avgBlkLen: String? = null,
  val dirBlock: String? = null,
  val lrecl: String? = null
)

/**
 * Calculate the next dataset HLQ last element basing on the provided [prefix] and already used indexes ([dsIdx]).
 * @return an 8 character string as a result
 */
fun calcDsHlqLastElem(prefix: String): String {
  require(prefix.length < 8) { "Prefix must be less than 8 characters" }
  val remainingLength = 8 - prefix.length
  val formattedIndex = dsIdx.toString().padStart(remainingLength, '0')
  dsIdx++
  return prefix + formattedIndex.takeLast(remainingLength)
}
