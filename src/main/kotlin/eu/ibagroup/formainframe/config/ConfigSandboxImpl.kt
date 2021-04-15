package eu.ibagroup.formainframe.config

import com.intellij.openapi.application.ApplicationManager
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialService
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.ReloadableEventHandler
import eu.ibagroup.formainframe.utils.crudable.getAll
import kotlin.streams.toList

data class SandboxState(
  val configState: ConfigState = ConfigState(),
  val credentials: MutableList<Credentials> = mutableListOf()
)

class ConfigSandboxImpl : ConfigSandbox {

  private var state = SandboxState()
    get() = synchronized(stateLock) { field }
    private set(value) = synchronized(stateLock) { field = value }

  private var initialState = SandboxState()

  private val stateLock = Any()

  override fun updateState() {
    initialState = state.clone()
  }

  override fun <T : Any> apply(clazz: Class<out T>) {
    synchronized(stateLock) {
      if (isModified(clazz)) {
        classToList(clazz, state)?.let { list ->
          if (clazz.isThe<Credentials>()) {
            with(Crudable.mergeCollections(initialState.credentials, state.credentials)) {
              val credentialService = CredentialService.instance
              listOf(toAdd, toUpdate).flatten().forEach {
                credentialService.setCredentials(it.connectionConfigUuid, it.username, it.password)
              }
              toDelete.forEach {
                credentialService.clearCredentials(it.connectionConfigUuid)
              }
            }
          } else {
            configCrudable.replaceGracefully(clazz, list.stream())
          }
        }
      }
    }
  }

  override fun fetch() {
    synchronized(stateLock) {
      rollbackSandbox<ConnectionConfig>()
      rollbackSandbox<WorkingSetConfig>()
      rollbackSandbox<UrlConnection>()
      rollbackSandbox<Credentials>()
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> rollback(clazz: Class<out T>) {
    synchronized(stateLock) {
      val current = if (clazz.isThe<Credentials>()) {
        configCrudable.getAll<ConnectionConfig>().map {
          with(CredentialService.instance) {
            Credentials(it.uuid, getUsernameByKey(it.uuid) ?: "", getPasswordByKey(it.uuid) ?: "")
          }
        }.toList()
      } else {
        configCrudable.getAll(clazz).toList()
      }
      listOfNotNull(classToList(clazz, state), classToList(clazz, initialState)).forEach { list ->
        list.clear()
        list.addAll(current.mapNotNull { it?.clone(clazz) }.toMutableList() as MutableList<T>)
      }
      eventHandler.onReload(clazz)
    }
  }

  override fun <T> isModified(clazz: Class<out T>): Boolean {
    return synchronized(stateLock) {
      val initial = classToList(clazz, initialState) ?: listOf()
      val current = classToList(clazz, state) ?: listOf()
      !(initial isTheSameAs current)
    }
  }

  private val eventHandler = object : ReloadableEventHandler {
    override fun onEvent(rowClass: Class<*>, row: Any) {
      ApplicationManager.getApplication()
        .messageBus
        .syncPublisher(SandboxListener.TOPIC)
        .update(rowClass)
    }

    override fun onReload(rowClass: Class<*>) {
      ApplicationManager.getApplication()
        .messageBus
        .syncPublisher(SandboxListener.TOPIC)
        .reload(rowClass)
    }

  }


  override val crudable = makeCrudableWithoutListeners(true, { state.credentials }) { state.configState }
    .configureCrudable {
      eventHandler = this@ConfigSandboxImpl.eventHandler
    }

}

inline fun <reified T> rollbackSandbox() {
  ConfigSandbox.instance.rollback(T::class.java)
}

inline fun <reified T> isSandboxModified(): Boolean {
  return ConfigSandbox.instance.isModified(T::class.java)
}

@Suppress("UNCHECKED_CAST")
internal fun <T> classToList(clazz: Class<out T>, state: SandboxState): MutableList<T>? {
  return if (clazz.isThe<Credentials>()) {
    state.credentials as MutableList<T>
  } else {
    classToList(clazz, state.configState)
  }
}