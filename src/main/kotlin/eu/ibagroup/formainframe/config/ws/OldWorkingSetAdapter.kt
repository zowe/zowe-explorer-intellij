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
import org.jetbrains.annotations.ApiStatus
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Factory for creating adapter for old working set configs.
 * @author Valentine Krus
 */
class OldWorkingSetAdapterFactory : OldConfigAdapterFactory {
  override fun buildAdapter(document: Document): OldConfigAdapter<*> {
    return OldWorkingSetAdapter(document)
  }
}

/**
 * Implementation of OldConfigAdapter for working sets.
 * Changes in new format:<br>
 * 1) name of WorkingSetConfig element changed to FilesWorkingSetConfig.
 * @see OldConfigAdapter
 * @author Valentine Krus
 */
@ApiStatus.ScheduledForRemoval(inVersion = "0.6")
class OldWorkingSetAdapter(private val document: Document) : OldConfigAdapter<FilesWorkingSetConfig> {

  /**
   * @see OldConfigAdapter.configClass
   */
  override val configClass = FilesWorkingSetConfig::class.java

  /**
   * Files working sets was stored as just WorkingSetConfig.
   * That's why it is necessary to find all this tag elements
   * and to create FilesWorkingSetConfig instances based on
   * their data.
   * @return list of working set elements in old config format
   */
  private fun getOldWsElements(): List<Element> {
    return document
      .documentElement
      .getApplicationOption("workingSets")
      ?.get("list")
      ?.firstOrNull()
      ?.get("WorkingSetConfig")
      ?: emptyList()
  }

  /**
   * @see OldConfigAdapter.getOldConfigsIds
   */
  override fun getOldConfigsIds(): List<String> {
    return getOldWsElements().map { it.getOptionValue("uuid") }
  }

  /**
   * @see OldConfigAdapter.castOldConfigs
   */
  override fun castOldConfigs(): List<FilesWorkingSetConfig> {
    return getOldWsElements().mapNotNull { wsConfigElement ->
      val connectionConfigUuid = wsConfigElement.getOptionValue("connectionConfigUuid")
      val name = wsConfigElement.getOptionValue("name")
      val uuid = wsConfigElement.getOptionValue("uuid")

      val dsMasks = wsConfigElement["option"]
        .firstOrNull { it.getAttribute("name") == "dsMasks" }
        ?.get("list")
        ?.firstOrNull()
        ?.get("DSMask")
        ?.mapNotNull {
          if (it.getOptionValue("mask") == "") null else DSMask(it.getOptionValue("mask"), mutableListOf())
        } ?: emptyList()

      val ussPaths = wsConfigElement["option"]
        .firstOrNull { it.getAttribute("name") == "ussPaths" }
        ?.get("list")
        ?.firstOrNull()
        ?.get("UssPath")
        ?.mapNotNull {
          if (it.getOptionValue("path") == "") null else UssPath(it.getOptionValue("path"))
        } ?: emptyList()

      if (connectionConfigUuid.isEmpty() || name.isEmpty() || uuid.isEmpty()) {
        null
      } else {
        FilesWorkingSetConfig(uuid, name, connectionConfigUuid, dsMasks.toMutableList(), ussPaths.toMutableList())
      }
    }
  }

}
