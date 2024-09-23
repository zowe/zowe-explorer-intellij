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

package eu.ibagroup.formainframe.config

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import eu.ibagroup.formainframe.utils.loadConfigClass

/**
 * State of all configs for plugin v2. It includes collections of config classes and other settings.
 * @author Viktar Mushtsin, Kiril Branavitski, Valiantsin Krus
 */
data class ConfigStateV2(
  @OptionTag(converter = CollectionsConverter::class)
  var collections: MutableMap<String, MutableList<*>> = mutableMapOf(),
  @OptionTag
  var settings: SettingsState = SettingsState()
)

/**
 * Gets collection from [ConfigStateV2] by class.
 * @param clazz class for which to get config collection.
 * @return collection if class is registered or null otherwise.
 */
@Suppress("UNCHECKED_CAST")
fun <T> ConfigStateV2.get(clazz: Class<out T>): MutableList<T>? {
  return collections[clazz.name] as MutableList<T>?
}

/**
 * Updates or adds collection for passed classes.
 * @param clazz class for which to add config collection.
 * @param collection config collection to register for specified class.
 */
fun <T> ConfigStateV2.set(clazz: Class<out T>, collection: MutableList<out T>) {
  collections[clazz.name] = collection
}

/** Inlined version of [ConfigStateV2.get]*/
inline fun <reified T> ConfigStateV2.get(): MutableList<T>? {
  return get(T::class.java)
}

/** Inlined version of [ConfigStateV2.set]*/
inline fun <reified T> ConfigStateV2.set(collection: MutableList<out T>) {
  set(T::class.java, collection)
}

/**
 * It is converter that will transform collections from and to xml value.
 * It is necessary because intellij uses xml data format that is not suitable
 * to store more complex data with unknown (on compilation step) type.
 * That's why configs will be stored as a json inside xml. This json will be
 * converted.
 * @author Valiantsin Krus
 */
class CollectionsConverter: Converter<MutableMap<String, MutableList<*>>>() {
  private val gson = Gson()

  /** Converts collections to json string. */
  override fun toString(value: MutableMap<String, MutableList<*>>): String? {
    return gson.toJson(value)
  }

  /** Parses json string to collections data */
  override fun fromString(value: String): MutableMap<String, MutableList<*>> {
    val mapType = object: TypeToken<Map<String, List<JsonElement>>>() {}.type
    val partiallyDeserialized: Map<String, List<JsonElement>> = gson.fromJson(value, mapType)
    val resultMap = mutableMapOf<String, MutableList<*>>()
    partiallyDeserialized.keys.forEach { key ->
      runCatching {
        val configClass = loadConfigClass(key) ?: throw Exception()
        val configList = partiallyDeserialized[key]?.map { gson.fromJson(it, configClass) }
        if (!configList.isNullOrEmpty()) {
          resultMap[key] = configList.toMutableList()
        }
      }
    }
    return resultMap
  }
}
