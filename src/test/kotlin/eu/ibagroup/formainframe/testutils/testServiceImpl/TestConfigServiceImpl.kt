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

package eu.ibagroup.formainframe.testutils.testServiceImpl

import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.EventHandler
import io.mockk.mockk
import java.time.Duration

open class TestConfigServiceImpl : ConfigService {

  var isIsAutoSyncEnabledChanged = false
  var isBatchSizeChanged = false
  var isRateUsNotificationDelayChanged = false

  var testInstance = object : ConfigService {

    override val crudable: Crudable = mockk<Crudable>()
    override val eventHandler: EventHandler = mockk<EventHandler>()
    override val autoSaveDelay: Duration = mockk<Duration>()
    override var isAutoSyncEnabled: Boolean = true
    override var batchSize: Int = 100
    override var rateUsNotificationDelay: Long = 1L

    override fun <T : Any> getConfigDeclaration(rowClass: Class<out T>): ConfigDeclaration<T> {
      TODO("Not yet implemented")
    }

    override fun <T> registerConfigClass(clazz: Class<out T>) {
      TODO("Not yet implemented")
    }

    override fun registerAllConfigClasses() {}

    override fun getRegisteredConfigClasses(): List<Class<*>> {
      TODO("Not yet implemented")
    }

    override fun getRegisteredConfigDeclarations(): List<ConfigDeclaration<*>> {
      TODO("Not yet implemented")
    }

    override fun getState(): ConfigStateV2? {
      TODO("Not yet implemented")
    }

    override fun loadState(state: ConfigStateV2) {
      TODO("Not yet implemented")
    }

  }

  override val crudable = testInstance.crudable

  override val eventHandler = testInstance.eventHandler

  override val autoSaveDelay = testInstance.autoSaveDelay

  override var isAutoSyncEnabled = testInstance.isAutoSyncEnabled
    set(value) {
      isIsAutoSyncEnabledChanged = true
      field = value
    }

  override var batchSize = testInstance.batchSize
    set(value) {
      isBatchSizeChanged = true
      field = value
    }

  override var rateUsNotificationDelay = testInstance.rateUsNotificationDelay
    set(value) {
      isRateUsNotificationDelayChanged = true
      field = value
    }

  override fun <T : Any> getConfigDeclaration(rowClass: Class<out T>): ConfigDeclaration<T> {
    return testInstance.getConfigDeclaration(rowClass)
  }

  override fun <T> registerConfigClass(clazz: Class<out T>) {
    testInstance.registerConfigClass(clazz)
  }

  override fun registerAllConfigClasses() {
    testInstance.registerAllConfigClasses()
  }

  override fun getRegisteredConfigClasses(): List<Class<*>> {
    return testInstance.getRegisteredConfigClasses()
  }

  override fun getRegisteredConfigDeclarations(): List<ConfigDeclaration<*>> {
    return testInstance.getRegisteredConfigDeclarations()
  }

  override fun getState(): ConfigStateV2? {
    return testInstance.state
  }

  override fun loadState(state: ConfigStateV2) {
    return testInstance.loadState(state)
  }

  fun resetTestService() {
    isIsAutoSyncEnabledChanged = false
    isBatchSizeChanged = false
    isRateUsNotificationDelayChanged = false
  }

}