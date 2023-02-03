/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.OptionTag
import eu.ibagroup.formainframe.utils.loadConfigClass

data class ConfigStateV2(
  @OptionTag(converter = CollectionsConverter::class)
  var collections: MutableMap<String, MutableList<*>> = mutableMapOf(),
  @OptionTag
  var settings: SettingsState = SettingsState()
)

@Suppress("UNCHECKED_CAST")
fun <T> ConfigStateV2.get(clazz: Class<out T>): MutableList<T>? {
  return collections[clazz.name] as MutableList<T>?
}

fun <T> ConfigStateV2.set(clazz: Class<out T>, collection: MutableList<out T>) {
  collections[clazz.name] = collection
}

inline fun <reified T> ConfigStateV2.get(): MutableList<T>? {
  return get(T::class.java)
}

inline fun <reified T> ConfigStateV2.set(collection: MutableList<out T>) {
  set(T::class.java, collection)
}

class CollectionsConverter: Converter<MutableMap<String, MutableList<*>>>() {
  private val gson = Gson()
  override fun toString(value: MutableMap<String, MutableList<*>>): String? {
    return gson.toJson(value)
  }

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