/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.config.*
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Factory for creating adapter for old jobs working set configs.
 */
class OldJobsWorkingSetAdapterFactory : OldConfigAdapterFactory {
  override fun buildAdapter(document: Document): OldConfigAdapter<*> {
    return OldJobsWorkingSetAdapter(document)
  }
}

/**
 * Implementation of OldConfigAdapter for jobs working set.
 * Changes the jobs filters from lowercase to uppercase.
 */
class OldJobsWorkingSetAdapter(private val document: Document) : OldConfigAdapter<JobsWorkingSetConfig> {

  /**
   * @see OldConfigAdapter.configClass
   */
  override val configClass = JobsWorkingSetConfig::class.java

  /**
   * Jobs filters can be stored in lower case.
   * That's why it is necessary to find all tags with such jobs filters.
   * @return list of jobs working set elements in old config format.
   */
  private fun getOldJobsWsElements(): List<Element> {
    return document
      .documentElement
      .getApplicationOption("jobsWorkingSets")
      ?.get("list")
      ?.firstOrNull()
      ?.get("JobsWorkingSetConfig")
      ?.filter { jobsWsElement ->
        getJobsFiltersElements(jobsWsElement)
          .any {
            val owner = it.getOptionValue("owner")
            val prefix = it.getOptionValue("prefix")
            val jobId = it.getOptionValue("jobId")
            owner != owner.uppercase() || prefix != prefix.uppercase() || jobId != jobId.uppercase()
          }
      } ?: emptyList()
  }

  /**
   * Get list of all jobs filters for jobs working set.
   * @param jobsWsElement jobs working set element.
   * @return list of jobs filters elements.
   */
  private fun getJobsFiltersElements(jobsWsElement: Element): List<Element> {
    return jobsWsElement["option"]
      .firstOrNull { it.getAttribute("name") == "jobsFilters" }
      ?.get("list")
      ?.firstOrNull()
      ?.get("JobsFilter") ?: emptyList()
  }

  /**
   * @see OldConfigAdapter.getOldConfigsIds
   */
  override fun getOldConfigsIds(): List<String> {
    return getOldJobsWsElements().map { it.getOptionValue("connectionConfigUuid") }
  }

  /**
   * @see OldConfigAdapter.castOldConfigs
   */
  override fun castOldConfigs(): List<JobsWorkingSetConfig> {
    return getOldJobsWsElements().mapNotNull { jobsWsElement ->
      val connectionConfigUuid = jobsWsElement.getOptionValue("connectionConfigUuid")
      val name = jobsWsElement.getOptionValue("name")
      val uuid = jobsWsElement.getOptionValue("uuid")

      val jobsFilters = getJobsFiltersElements(jobsWsElement)
        .map {
          JobsFilter(
            it.getOptionValue("owner").uppercase(),
            it.getOptionValue("prefix").uppercase(),
            it.getOptionValue("jobId").uppercase()
          )
        }.toSet()

      if (connectionConfigUuid.isEmpty() || name.isEmpty()) {
        null
      } else {
        JobsWorkingSetConfig(uuid, name, connectionConfigUuid, jobsFilters.toMutableList())
      }
    }
  }
}