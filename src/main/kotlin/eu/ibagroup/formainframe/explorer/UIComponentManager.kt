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

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import eu.ibagroup.formainframe.config.connect.ConnectionConfigBase

/** UI component manager service interface */
interface UIComponentManager : Disposable {

  companion object {
    @JvmStatic
    fun getService(): UIComponentManager = service()
  }

  fun getExplorerContentProviders(): List<ExplorerContentProvider<*, *>>

  fun <E : Explorer<*, *>> getExplorerContentProvider(clazz: Class<out E>): ExplorerContentProvider<out ConnectionConfigBase, out Explorer<*, *>>?

  fun <E : Explorer<*, *>> getExplorer(clazz: Class<out E>): E

}
