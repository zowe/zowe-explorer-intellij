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

import eu.ibagroup.formainframe.config.ConfigSandbox
import eu.ibagroup.formainframe.utils.crudable.Crudable
import io.mockk.mockk

open class TestConfigSandboxImpl : ConfigSandbox {

  var testInstance = object : ConfigSandbox {

    override val crudable: Crudable
      get() = TODO("Not yet implemented")

    override fun <T> registerConfigClass(clazz: Class<out T>) {
      TODO("Not yet implemented")
    }

    override fun updateState() {
      TODO("Not yet implemented")
    }

    override fun <T : Any> apply(clazz: Class<out T>) {
      TODO("Not yet implemented")
    }

    override fun fetch() {
      TODO("Not yet implemented")
    }

    override fun <T> rollback(clazz: Class<out T>) {
      TODO("Not yet implemented")
    }

    override fun <T> isModified(clazz: Class<out T>): Boolean {
      TODO("Not yet implemented")
    }

  }

  override val crudable = mockk<Crudable>()

  override fun <T> registerConfigClass(clazz: Class<out T>) {
    testInstance.registerConfigClass(clazz)
  }

  override fun updateState() {
    testInstance.updateState()
  }

  override fun <T : Any> apply(clazz: Class<out T>) {
    testInstance.apply(clazz)
  }

  override fun fetch() {
    testInstance.fetch()
  }

  override fun <T> rollback(clazz: Class<out T>) {
    testInstance.rollback(clazz)
  }

  override fun <T> isModified(clazz: Class<out T>): Boolean {
    return testInstance.isModified(clazz)
  }

}