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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.EventHandler
import eu.ibagroup.formainframe.utils.crudable.annotations.Contains
import eu.ibagroup.formainframe.utils.sendTopic
import java.time.Duration

fun sendConfigServiceTopic(): EventHandler = sendTopic(CONFIGS_CHANGED)

@JvmField
val CONFIGS_CHANGED = Topic.create("configsChanged", EventHandler::class.java)

@JvmField
val CONFIGS_LOADED = Topic.create("configsLoaded", Runnable::class.java)

interface ConfigService : PersistentStateComponent<ConfigState> {

  companion object {
    @JvmStatic
    val instance: ConfigService
      get() = ApplicationManager.getApplication().getService(ConfigService::class.java)
  }

  @get:Contains(
    entities = [
      WorkingSetConfig::class,
      ConnectionConfig::class,
      UrlConnection::class
    ]
  )
  val crudable: Crudable

  val eventHandler: EventHandler

  val autoSaveDelay: Duration

}

class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfig) : Exception(
  "No username or password found for $connectionConfig"
)

val configCrudable: Crudable
  get() = ConfigService.instance.crudable
