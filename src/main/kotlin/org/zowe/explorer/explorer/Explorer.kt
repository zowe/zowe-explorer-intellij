/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationBuilder
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
import org.zowe.explorer.config.CONFIGS_CHANGED
import org.zowe.explorer.config.configCrudable
import org.zowe.explorer.config.connect.CREDENTIALS_CHANGED
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialsListener
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.utils.*
import org.zowe.explorer.utils.crudable.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write


interface ExplorerListener {
  fun onChanged(explorer: Explorer<*>, unit: ExplorerUnit)
  fun onAdded(explorer: Explorer<*>, unit: ExplorerUnit)
  fun onDeleted(explorer: Explorer<*>, unit: ExplorerUnit)
}

interface ExplorerFactory<U: WorkingSet<*>, E : Explorer<U>> {
  fun buildComponent() : E
}
@JvmField
val UNITS_CHANGED = Topic.create("unitsChanged", ExplorerListener::class.java)

interface Explorer<U: WorkingSet<*>> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<ExplorerFactory<*, *>>("org.zowe.explorer.explorer")
  }

  val units: MutableCollection<U>
  val unitClass: Class<U>

  fun disposeUnit(unit: U)

  fun isUnitPresented(unit: ExplorerUnit): Boolean

  val componentManager: ComponentManager

  val nullableProject: Project?
    get() = componentManager.castOrNull()

  fun reportThrowable(t: Throwable, project: Project?)

  fun showNotification(
    title: String,
    content: String,
    type: NotificationType = NotificationType.INFORMATION,
    project: Project?
  )

  fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?)
}

abstract class AbstractExplorerBase<U: WorkingSet<*>, UnitConfig: EntityWithUuid>: Explorer<U>, Disposable {

  val lock = ReentrantReadWriteLock()

  val disposable = Disposer.newDisposable()

  abstract val unitConfigClass: Class<UnitConfig>

  abstract fun UnitConfig.toUnit(parentDisposable: Disposable): U

  override val componentManager: Application
    get() = ApplicationManager.getApplication()

  override fun disposeUnit(unit: U) {
    configCrudable.getByUniqueKey(unitConfigClass, unit.uuid).nullable?.let {
      configCrudable.delete(it)
    }
  }

  override fun isUnitPresented(unit: ExplorerUnit): Boolean {
    return unit.`is`(unitClass) && units.contains(unit)
  }


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

  override fun reportThrowable(t: Throwable, project: Project?) {
    if (t is ProcessCanceledException) {
      return
    }
    Notification(
      EXPLORER_NOTIFICATION_GROUP_ID,
      "Error in plugin Zowe Explorer",
      t.message ?: t.toString(),
      NotificationType.ERROR
    ).let {
      Notifications.Bus.notify(it)
    }
  }

  private val reportInSlackAction = object : DumbAwareAction("Report In Slack") {
    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse("https://join.slack.com/t/openmainframeproject/shared_invite/zt-u2nc4hv8-js4clxu87h5UMb~KCfAPuQ")
    }
  }

  override fun reportThrowable(t: Throwable, unit: ExplorerUnit, project: Project?) {
    reportThrowable(t, project)
  }

  override fun dispose() {
    Disposer.dispose(disposable)
  }

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
        val found = units.find { it.connectionConfig?.uuid == uuid }
        sendTopic(UNITS_CHANGED).onChanged(this@AbstractExplorerBase, found ?: return@CredentialsListener)
      }
    })
  }
}
