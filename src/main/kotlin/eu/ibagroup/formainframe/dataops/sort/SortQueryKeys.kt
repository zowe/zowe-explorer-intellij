/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.dataops.sort

/**
 * Enum class represents the sorting keys which is currently enabled for particular Node
 */
enum class SortQueryKeys(private val sortType: String) {
  FILE_NAME("uss_file_name"),
  FILE_TYPE("uss_type"),
  FILE_MODIFICATION_DATE("uss_modification_date"),
  JOB_NAME("Job Name"),
  JOB_CREATION_DATE("Job Creation Date"),
  JOB_COMPLETION_DATE("Job Completion Date"),
  JOB_STATUS("Job Status"),
  JOB_OWNER("Job Owner"),
  JOB_ID("Job ID"),
  ASCENDING("Ascending"),
  DESCENDING("Descending");

  override fun toString(): String {
    return sortType
  }
}

val typedSortKeys : List<SortQueryKeys> by lazy {
  return@lazy listOf(SortQueryKeys.FILE_NAME, SortQueryKeys.FILE_TYPE, SortQueryKeys.FILE_MODIFICATION_DATE, SortQueryKeys.JOB_NAME, SortQueryKeys.JOB_ID, SortQueryKeys.JOB_OWNER, SortQueryKeys.JOB_STATUS, SortQueryKeys.JOB_CREATION_DATE, SortQueryKeys.JOB_COMPLETION_DATE)
}

val orderingSortKeys : List<SortQueryKeys> by lazy {
  return@lazy listOf(SortQueryKeys.ASCENDING, SortQueryKeys.DESCENDING)
}