/*
 * This is property of IBA Group
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.jobs.JobsFilter
import eu.ibagroup.formainframe.config.jobs.JobsWorkingSetConfig
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.sendTopic
import eu.ibagroup.formainframe.utils.subscribe
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read
import kotlin.concurrent.write


class JesExplorerFactory : ExplorerFactory<JesWorkingSet, JesExplorer> {
  override fun buildComponent(): JesExplorer = JesExplorer()
}

class JesExplorer : AbstractExplorerBase<JesWorkingSet, JobsWorkingSetConfig>() {
  override val unitClass = JesWorkingSet::class.java
  override val unitConfigClass = JobsWorkingSetConfig::class.java

  override fun JobsWorkingSetConfig.toUnit(parentDisposable: Disposable): JesWorkingSet {
    return GlobalJesWorkingSet(
      uuid = uuid,
      globalExplorer = this@JesExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

}

class JobsWorkingSetUnit(
  override val explorer: Explorer<*>,
  val jobsWorkingSetConfig: JobsWorkingSetConfig
) : ExplorerUnit {
  override val name = jobsWorkingSetConfig.name
  override val uuid = jobsWorkingSetConfig.uuid
  override val connectionConfig: ConnectionConfig? = configCrudable.getByUniqueKey(jobsWorkingSetConfig.connectionConfigUuid)
}

class JesFilterUnit(
  override val explorer: Explorer<*>,
  val jobsFilter: JobsFilter
) : ExplorerUnit {
  override val name: String = jobsFilter.toString()
  override val uuid: String = ""
  override val connectionConfig: ConnectionConfig? = configCrudable.getByUniqueKey(""/*jobsFilter.connectionConfigUuid*/)
}
