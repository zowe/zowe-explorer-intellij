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
 * Factory for creating adapter for old JES working set configs.
 */
class OldJesWorkingSetAdapterFactory : OldConfigAdapterFactory {
  override fun buildAdapter(document: Document): OldConfigAdapter<*> {
    return OldJesWorkingSetAdapter(document)
  }
}

/**
 * Implementation of OldConfigAdapter for JES working set.
 * Changes the jobs filters from lowercase to uppercase.
 */
class OldJesWorkingSetAdapter(private val document: Document) : OldConfigAdapter<JesWorkingSetConfig> {

  /**
   * @see OldConfigAdapter.configClass
   */
  override val configClass = JesWorkingSetConfig::class.java

  /**
   * Jobs filters can be stored in lower case.
   * That's why it is necessary to find all tags with such jobs filters.
   * @return list of JES working set elements in old config format.
   */
  private fun getOldJesWsElements(): List<Element> {
    return document
      .documentElement
      .getApplicationOption("jesWorkingSets")
      ?.get("list")
      ?.firstOrNull()
      ?.get("JesWorkingSetConfig")
      ?.filter { jesWsElement ->
        getJobsFiltersElements(jesWsElement)
          .any {
            val owner = it.getOptionValue("owner")
            val prefix = it.getOptionValue("prefix")
            val jobId = it.getOptionValue("jobId")
            owner != owner.uppercase() || prefix != prefix.uppercase() || jobId != jobId.uppercase()
          }
      } ?: emptyList()
  }

  /**
   * Get list of all jobs filters for JES working set.
   * @param jesWsElement JES working set element.
   * @return list of jobs filters elements.
   */
  private fun getJobsFiltersElements(jesWsElement: Element): List<Element> {
    return jesWsElement["option"]
      .firstOrNull { it.getAttribute("name") == "jobsFilters" }
      ?.get("list")
      ?.firstOrNull()
      ?.get("JobsFilter") ?: emptyList()
  }

  /**
   * @see OldConfigAdapter.getOldConfigsIds
   */
  override fun getOldConfigsIds(): List<String> {
    return getOldJesWsElements().map { it.getOptionValue("connectionConfigUuid") }
  }

  /**
   * @see OldConfigAdapter.castOldConfigs
   */
  override fun castOldConfigs(): List<JesWorkingSetConfig> {
    return getOldJesWsElements().mapNotNull { jesWsElement ->
      val connectionConfigUuid = jesWsElement.getOptionValue("connectionConfigUuid")
      val name = jesWsElement.getOptionValue("name")
      val uuid = jesWsElement.getOptionValue("uuid")

      val jobsFilters = getJobsFiltersElements(jesWsElement)
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
        JesWorkingSetConfig(uuid, name, connectionConfigUuid, jobsFilters.toMutableList())
      }
    }
  }
}