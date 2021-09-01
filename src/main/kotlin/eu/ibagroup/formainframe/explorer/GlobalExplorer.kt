package eu.ibagroup.formainframe.explorer

import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationBuilder
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdaptor
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.stream.Collectors
import kotlin.concurrent.read
import kotlin.concurrent.write

const val EXPLORER_NOTIFICATION_GROUP_ID = "eu.ibagroup.formainframe.explorer.ExplorerNotificationGroup"

class GlobalExplorerFactory : ExplorerFactory<FilesWorkingSet, GlobalExplorer> {
  override fun buildComponent(): GlobalExplorer = GlobalExplorer()
}

class GlobalExplorer : AbstractExplorerBase<FilesWorkingSet, WorkingSetConfig>() {


  override fun WorkingSetConfig.toUnit(parentDisposable: Disposable): GlobalWorkingSet {
    return GlobalWorkingSet(
      uuid = uuid,
      globalExplorer = this@GlobalExplorer,
      workingSetConfigProvider = { configCrudable.getByUniqueKey(it) },
      parentDisposable = parentDisposable
    )
  }

  override val unitClass = FilesWorkingSet::class.java
  override val unitConfigClass = WorkingSetConfig::class.java

  init {
    doInit()
  }

}
