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


/** UI component manager service implementation. Is used to work with the UI components through it */
class UIComponentManagerImpl : UIComponentManager {

  private val explorerList by lazy {
    Explorer.EP.extensionList.map {
      it.buildComponent()
    }
  }

  private val explorerContentProviderList by lazy {
    ExplorerContentProvider.EP.extensionList.map {
      it.buildComponent()
    }
  }

  override fun getExplorerContentProviders(): List<ExplorerContentProvider<*, *>> {
    return explorerContentProviderList
  }

  @Suppress("UNCHECKED_CAST")
  override fun <E : Explorer<*, *>> getExplorerContentProvider(clazz: Class<out E>): ExplorerContentProvider<*, out E>? {
    return explorerContentProviderList.firstOrNull { it.explorer::class.java.isAssignableFrom(clazz) } as ExplorerContentProvider<*, out E>?
  }

  @Suppress("UNCHECKED_CAST")
  override fun <E : Explorer<*, *>> getExplorer(clazz: Class<out E>): E {
    val explorer = explorerList.find {
      it::class.java.isAssignableFrom(clazz)
    } ?: throw IllegalArgumentException("Class $clazz is not registered as Explorer extension point")
    return explorer as E
  }


  override fun dispose() {

  }
}
