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

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import org.zowe.explorer.config.connect.ConnectionConfigBase

/** UI component manager service interface */
interface UIComponentManager : Disposable {

  companion object {
    val INSTANCE = ApplicationManager.getApplication().getService(UIComponentManager::class.java)
  }

  fun getExplorerContentProviders(): List<ExplorerContentProvider<*, *>>

  fun <E : Explorer<*, *>> getExplorerContentProvider(clazz: Class<out E>): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>>?

  fun <E : Explorer<*, *>> getExplorer(clazz: Class<out E>): E

}
