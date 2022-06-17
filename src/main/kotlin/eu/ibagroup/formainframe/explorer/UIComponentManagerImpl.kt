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


class UIComponentManagerImpl : UIComponentManager {

  private val explorerList by lazy {
    @Suppress("UNCHECKED_CAST")
    Explorer.EP.extensionList.map {
      it.buildComponent()
    }
  }

  private val explorerContentProviderList by lazy {
    @Suppress("UNCHECKED_CAST")
    ExplorerContentProvider.EP.extensionList.map {
      it.buildComponent()
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <E : Explorer<*>> getExplorerContentProviders(): List<ExplorerContentProvider<E>> {
    return explorerContentProviderList as List<ExplorerContentProvider<E>>
  }

  @Suppress("UNCHECKED_CAST")
  override fun <E : Explorer<*>> getExplorerContentProvider(clazz: Class<out E>): ExplorerContentProvider<E> {
    return explorerContentProviderList.find { it.explorer::class.java.isAssignableFrom(clazz) } as ExplorerContentProvider<E>
  }

  @Suppress("UNCHECKED_CAST")
  override fun <E : Explorer<*>> getExplorer(clazz: Class<out E>): E {
    val explorer = explorerList.find {
      it::class.java.isAssignableFrom(clazz)
    } ?: throw IllegalArgumentException("Class $clazz is not registered as Explorer extension point")
    return explorer as E
  }


  override fun dispose() {

  }
}
