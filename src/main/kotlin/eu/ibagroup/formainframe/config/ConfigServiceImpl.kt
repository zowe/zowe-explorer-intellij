package eu.ibagroup.formainframe.config

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.rd.util.UUID
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.Credentials
import eu.ibagroup.formainframe.config.connect.UrlConnection
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.castOrNull
import eu.ibagroup.formainframe.utils.crudable.*
import eu.ibagroup.formainframe.utils.runIfTrue
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

@State(
  name = "by.iba.connector.services.ConfigService",
  storages = [Storage(value = "iba_connector_config.xml", exportable = true)]
)
class ConfigServiceImpl : ConfigService {

  companion object {
    private val myState = ConfigState()
    private val loaded = AtomicBoolean(false)
  }

  override fun getState(): ConfigState {
    return myState
  }

  override fun loadState(state: ConfigState) {
    XmlSerializerUtil.copyBean(state, myState)
  }

  override val eventHandler = ConfigEventHandler()

  override val autoSaveDelay: Duration = Duration.ofSeconds(5)

  override val crudable = makeCrudableWithoutListeners(false) { myState }
    .configureCrudable {
      eventHandler = this@ConfigServiceImpl.eventHandler
    }
}

internal abstract class ClassCaseSwitcher<R> {

  abstract fun onConnectionConfig(): R

  abstract fun onZOSMFUrlConnection(): R

  abstract fun onWorkingSetConfig(): R

  open fun onCredentials(): R {
    return onElse()
  }

  abstract fun onElse(): R

  operator fun invoke(clazz: Class<*>): R {
    return when (clazz) {
      ConnectionConfig::class.java -> onConnectionConfig()
      UrlConnection::class.java -> onZOSMFUrlConnection()
      WorkingSetConfig::class.java -> onWorkingSetConfig()
      Credentials::class.java -> onCredentials()
      else -> onElse()
    }
  }

}

private class FilterSwitcher(
  private val crudable: Crudable,
  private val row: Any
) : ClassCaseSwitcher<Long>() {
  override fun onConnectionConfig(): Long {
    return crudable.getByColumnLambda(row as ConnectionConfig) { it.name }.count()
  }

  override fun onZOSMFUrlConnection(): Long {
    return if (row is UrlConnection) {
      crudable.find<UrlConnection> { it.url == row.url && it.isAllowSelfSigned == row.isAllowSelfSigned }.count()
    } else {
      Long.MAX_VALUE
    }
  }

  override fun onWorkingSetConfig(): Long {
    return crudable.getByColumnLambda(row as WorkingSetConfig) { it.name }.count()
  }

  override fun onCredentials(): Long {
    return 0
  }

  override fun onElse(): Long {
    return -1
  }
}

private class UpdateFilterSwitcher(
  crudable: Crudable,
  private val currentRow: Any,
  private val updatingRow: Any
) : ClassCaseSwitcher<Boolean>() {
  private val filterSwitcher = FilterSwitcher(crudable, updatingRow)

  override fun onConnectionConfig(): Boolean {
    return if (currentRow is ConnectionConfig && updatingRow is ConnectionConfig) {
      filterSwitcher.onConnectionConfig() == 0L || updatingRow.name == currentRow.name
    } else false
  }

  override fun onZOSMFUrlConnection(): Boolean {
    return if (currentRow is UrlConnection && updatingRow is UrlConnection) {
      filterSwitcher.onZOSMFUrlConnection() == 0L || updatingRow.url == currentRow.url
    } else false
  }

  override fun onWorkingSetConfig(): Boolean {
    return if (currentRow is WorkingSetConfig && updatingRow is WorkingSetConfig) {
      filterSwitcher.onWorkingSetConfig() == 0L || updatingRow.name == currentRow.name
    } else false
  }

  override fun onCredentials(): Boolean {
    return true
  }

  override fun onElse(): Boolean {
    return false
  }

}

internal fun makeCrudableWithoutListeners(
  withCredentials: Boolean,
  credentialsGetter: () -> MutableList<Credentials> = { mutableListOf() },
  stateGetter: () -> ConfigState,
): Crudable {
  return CrudableListsBuilder {
    object : ClassCaseSwitcher<MutableList<*>?>() {
      override fun onConnectionConfig(): MutableList<*> {
        return stateGetter().connections
      }

      override fun onZOSMFUrlConnection(): MutableList<*> {
        return stateGetter().urls
      }

      override fun onWorkingSetConfig(): MutableList<*> {
        return stateGetter().workingSets
      }

      override fun onCredentials(): MutableList<*>? {
        return withCredentials.runIfTrue {
          credentialsGetter()
        }
      }

      override fun onElse(): MutableList<*>? {
        return null
      }
    }(it)
  }.withNextUuidProvider {
    UUID.randomUUID().toString()
  }.apply crudable@{
    addFilter = AddFilter { rowClass, row ->
      FilterSwitcher(this, row)(rowClass) == 0L
    }
    updateFilter = UpdateFilter { clazz, currentRow, updatingRow ->
      UpdateFilterSwitcher(this, currentRow, updatingRow)(clazz)
    }
  }.let {
    ConcurrentCrudable(it, SimpleReadWriteAdapter())
  }
}

internal inline fun Crudable.configureCrudable(block: CrudableLists.() -> Unit): Crudable {
  this.castOrNull<ConcurrentCrudable>()?.apply {
    wrappingCrudable.castOrNull<CrudableLists>()?.apply(block)
  }
  return this
}

@Suppress("UNCHECKED_CAST")
internal fun <T> classToList(clazz: Class<out T>, state: ConfigState): MutableList<T>? {
  return object : ClassCaseSwitcher<MutableList<T>?>() {
    override fun onConnectionConfig(): MutableList<T> {
      return state.connections as MutableList<T>
    }

    override fun onZOSMFUrlConnection(): MutableList<T> {
      return state.urls as MutableList<T>
    }

    override fun onWorkingSetConfig(): MutableList<T> {
      return state.workingSets as MutableList<T>
    }

    override fun onElse(): MutableList<T>? {
      return null
    }

  }(clazz as Class<*>)
}