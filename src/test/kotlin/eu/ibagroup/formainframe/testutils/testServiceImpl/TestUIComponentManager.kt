/*
 * Copyright (c) 2024 IBA Group.
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

import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase
import eu.ibagroup.formainframe.explorer.Explorer
import eu.ibagroup.formainframe.explorer.ExplorerContentProvider
import eu.ibagroup.formainframe.explorer.UIComponentManager

open class TestUIComponentManager : UIComponentManager {

  /**
   * Test instance for the UIComponentManager.
   * Defines default behaviour of the service at initialization stage.
   * All the test class methods use this implementation, so it makes this easier to redefine in a test case
   */
  var testInstance: UIComponentManager = object : UIComponentManager {

    override fun getExplorerContentProviders(): List<ExplorerContentProvider<*, *>> {
      TODO("Not yet implemented")
    }

    override fun <E : Explorer<*, *>> getExplorerContentProvider(clazz: Class<out E>): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>>? {
      TODO("Not yet implemented")
    }

    override fun <E : Explorer<*, *>> getExplorer(clazz: Class<out E>): E {
      TODO("Not yet implemented")
    }

    override fun dispose() {
      TODO("Not yet implemented")
    }

  }

  override fun getExplorerContentProviders(): List<ExplorerContentProvider<*, *>> {
    return testInstance.getExplorerContentProviders()
  }

  override fun <E : Explorer<*, *>> getExplorerContentProvider(
    clazz: Class<out E>
  ): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>>? {
    return testInstance.getExplorerContentProvider(clazz)
  }

  override fun <E : Explorer<*, *>> getExplorer(clazz: Class<out E>): E {
    return testInstance.getExplorer(clazz)
  }

  override fun dispose() {
    return testInstance.dispose()
  }
}