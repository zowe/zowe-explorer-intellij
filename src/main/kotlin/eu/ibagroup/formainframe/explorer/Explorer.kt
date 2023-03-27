/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.Topic
import eu.ibagroup.formainframe.config.CONFIGS_CHANGED
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.CREDENTIALS_CHANGED
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.config.connect.CredentialsListener
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.anyEventAdaptor
import eu.ibagroup.formainframe.utils.crudable.eventAdapter
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Listener for reactive display of performed changes with units.
 * @author Kiril Branavitski
 * @author Viktar Mushtsin
 */
interface ExplorerListener{
  /**
   * Handler for update event for units.
   * @param explorer explorer units of which was updated.
   * @param unit unit that was updated.
   */
  fun <Connection: ConnectionConfigBase> onChanged(explorer: Explorer<Connection, *>, unit: ExplorerUnit<Connection>)

  /**
   * Handler for create event for units.
   * @param explorer explorer units of which was created.
   * @param unit unit that was created.
   */
  fun <Connection: ConnectionConfigBase> onAdded(explorer: Explorer<Connection, *>, unit: ExplorerUnit<Connection>)

  /**
   * Handler for delete event for units.
   * @param explorer explorer units of which was deleted.
   * @param unit unit that was deleted.
   */
  fun <Connection: ConnectionConfigBase> onDeleted(explorer: Explorer<Connection, *>, unit: ExplorerUnit<Connection>)
}

/**
 * Factory for registering explorer in intellij platform.
 * @author Kiril Branavitski
 * @author Viktar Mushtsin
 */
interface ExplorerFactory<Connection: ConnectionConfigBase, U : WorkingSet<Connection, *>, E : Explorer<Connection, U>> {
  fun buildComponent(): E
}

/**
 * Intellij topic for explorer listener
 * @see ExplorerListener
 */
@JvmField
val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)

/**
 * Abstract class for working with explorer logical representation.
 * @author Viktar Mushtsin
 */
interface Explorer<Connection: ConnectionConfigBase, U : WorkingSet<Connection, *>> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ExplorerFactory<ConnectionConfig, *, *>>("eu.ibagroup.formainframe.explorer")
  }

  val units: MutableCollection<U>
  val unitClass: Class<U>

  fun disposeUnit(unit: U)

  fun isUnitPresented(unit: ExplorerUnit<Connection>): Boolean

  val componentManager: ComponentManager

  val nullableProject: Project?
    get() = componentManager.castOrNull()

  /**
   * Shows error notification.
   * @param t throwable from which to get title and text of notification message.
   * @param project project for which to show notification.
   */
  fun reportThrowable(t: Throwable, project: Project?)

  /**
   * Shows any type of notification.
   * @param title title of the notification message.
   * @param content text of notification content.
   * @param type notification type (information, error, warning).
   * @param project project for which to show notification.
   */
  fun showNotification(
    title: String,
    content: String,
    type: NotificationType = NotificationType.INFORMATION,
    project: Project?
  )

  /**
   * Does exactly the same, as report throwable but can use additional info of the unit.
   * @param t throwable from which to get title and text of notification message.
   * @param unit unit which can be used for showing additional information in notification.
   * @param project project for which to show notification.
   */
  fun reportThrowable(t: Throwable, unit: ExplorerUnit<Connection>, project: Project?)
}

/**
 * Base explorer implementation.
 * @author Viktar Mushtsin
 * @author Kiril Branavitski
 * @author Valiantsin Krus
 */
abstract class AbstractExplorerBase<Connection: ConnectionConfigBase, U : WorkingSet<Connection, *>, UnitConfig : EntityWithUuid>
  : Explorer<Connection, U>, Disposable {

  val lock = ReentrantReadWriteLock()

  val disposable = Disposer.newDisposable()

  abstract val unitConfigClass: Class<UnitConfig>

  abstract fun UnitConfig.toUnit(parentDisposable: Disposable): U

  override val componentManager: Application
    get() = ApplicationManager.getApplication()

  /**
   * Removes unit from configuration crudable.
   * @param unit unit to remove.
   */
  override fun disposeUnit(unit: U) {
    configCrudable.getByUniqueKey(unitConfigClass, unit.uuid).nullable?.let {
      configCrudable.delete(it)
    }
  }

  /**
   * Checks if explorer has specified unit.
   * @param unit unit to find.
   * @return true if unit was found and false otherwise.
   */
  override fun isUnitPresented(unit: ExplorerUnit<Connection>): Boolean {
    return unit.`is`(unitClass) && units.contains(unit)
  }


  /** @see Explorer.showNotification */
  override fun showNotification(title: String, content: String, type: NotificationType, project: Project?) {
    Notification(
      EXPLORER_NOTIFICATION_GROUP_ID,
      title,
      content,
      type
    ).let {
      Notifications.Bus.notify(it, project)
    }
  }

  /** @see Explorer.reportThrowable */
  override fun reportThrowable(t: Throwable, project: Project?) {
    if (t is ProcessCanceledException) {
      return
    }
    Notification(
      EXPLORER_NOTIFICATION_GROUP_ID,
      "Error in plugin For Mainframe",
      t.message ?: t.toString(),
      NotificationType.ERROR
    ).let {
      Notifications.Bus.notify(it)
    }
  }

  /** Function to open a slack channel of plugin support. */
  private val reportInSlackAction = object : DumbAwareAction("Report In Slack") {
    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse("https://join.slack.com/t/iba-mainframe-tools/shared_invite/zt-pejbtt4j-l7KuizvDedJSCxwGtxmMBg")
    }
  }

  /** @see Explorer.reportThrowable */
  override fun reportThrowable(t: Throwable, unit: ExplorerUnit<Connection>, project: Project?) {
    reportThrowable(t, project)
  }

  /** Disposes the explorer. */
  override fun dispose() {
    Disposer.dispose(disposable)
  }

  /**
   * Function for initializing explorer.
   * Initialization includes registering disposable and subscribing on CONFIGS_CHANGED
   * for reactive processing of data updates on UI.
   */
  fun doInit() {
    Disposer.register(service<DataOpsManager>(), disposable)
    subscribe(CONFIGS_CHANGED, disposable, eventAdapter(unitConfigClass) {
      onDelete { unit ->
        lock.write {
          val found = units.find { it.uuid == unit.uuid } ?: return@onDelete
          Disposer.dispose(found)
          units.remove(found)
          sendTopic(UNITS_CHANGED).onDeleted(this@AbstractExplorerBase, found)
        }
      }
      onAdd { unit ->
        lock.write {
          val added = unit.toUnit(disposable)
          units.add(added)
          sendTopic(UNITS_CHANGED).onAdded(this@AbstractExplorerBase, added)
        }
      }
      onUpdate { _, unitConfig ->
        lock.read {
          val found = units.find { it.uuid == unitConfig.uuid }
          sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, found ?: return@onUpdate)
        }
      }
    })
    subscribe(CONFIGS_CHANGED, disposable, anyEventAdaptor<ConnectionConfig> {
      lock.read {
        units.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, it)
        }
      }
    })
    subscribe(CREDENTIALS_CHANGED, disposable, CredentialsListener { uuid ->
      lock.read {
        units.filter { it.connectionConfig?.uuid == uuid }.forEach {
          sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, it)
        }
      }
    })
  }
}
