/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config

import com.intellij.openapi.extensions.ExtensionPointName
import org.zowe.explorer.utils.crudable.EntityWithUuid
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Factory for creating OldConfigAdapters.
 * @author Valentine Krus
 */
interface OldConfigAdapterFactory {
  fun buildAdapter(document: Document): OldConfigAdapter<*>
}

/**
 * Interface for extracting and parsing configs in old format to new one.
 * @author Valentine Krus
 */
interface OldConfigAdapter<T: EntityWithUuid> {

  companion object {
    /**
     * Adapters extension point name.
     */
    @JvmStatic
    val EP = ExtensionPointName.create<OldConfigAdapterFactory>("org.zowe.explorer.oldConfigAdapter")
  }

  /**
   * Class of config to detect and parse (ConnectionConfig, WorkingSetConfig ...)
   */
  val configClass: Class<out T>

  /**
   * Finds uuid of elements that are stored in old format.
   * @return list of uuids.
   */
  fun getOldConfigsIds(): List<String>

  /**
   * Extracts all necessary data from old format configs
   * and creates config instances in new style by this data.
   * @return list of configs (UrlConnection, WorkingSetConfig ...)
   * in new style (ConnectionConfig, FilesWorkingSetConfig).
   */
  fun castOldConfigs(): List<T>
}

/**
 * Utility function to get option value of corresponding config entity
 * (for example option "url" of entity "ConnectionConfig").
 * Options are stored in format <option name="url" value="..."/>.
 * @param name option name
 * @return option value or empty string if it was not found.
 */
fun Element.getOptionValue(name: String): String {
  return get("option").firstOrNull { it.getAttribute("name") == name }?.getAttribute("value") ?: ""
}

/**
 * Utility function to get option inside application and component tags of intellij format.
 * @param name option name (stored in option tag attribute).
 * @return option element or null if it's not exist.
 */
fun Element.getApplicationOption(name: String): Element? {
  return this["component"].firstOrNull()?.get("option")?.firstOrNull { it.getAttribute("name") == name }
}
