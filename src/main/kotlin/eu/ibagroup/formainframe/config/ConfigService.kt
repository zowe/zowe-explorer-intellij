package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.JobsWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.EventHandler
import eu.ibagroup.formainframe.utils.crudable.annotations.Contains
import eu.ibagroup.formainframe.utils.sendTopic
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

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
      FilesWorkingSetConfig::class,
      ConnectionConfig::class,
      JobsWorkingSetConfig::class
    ]
  )
  val crudable: Crudable

  val eventHandler: EventHandler

  val autoSaveDelay: Duration

  var isAutoSyncEnabled: AtomicBoolean

}

class CredentialsNotFoundForConnection(val connectionConfig: ConnectionConfig) : Exception(
  "No username or password found for $connectionConfig"
)

val configCrudable: Crudable
  get() = ConfigService.instance.crudable
