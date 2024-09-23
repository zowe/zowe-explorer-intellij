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
package eu.ibagroup.formainframe.config.connect

import com.intellij.openapi.options.BoundSearchableConfigurable
import eu.ibagroup.formainframe.config.ConfigDeclaration
import eu.ibagroup.formainframe.config.connect.ui.CollectedConfigurable
import eu.ibagroup.formainframe.utils.crudable.Crudable

/**
 * Abstract class to declare connection configs.
 * @param Connection The system (such as zosmf, cics etc.) connection class to work with (see [ConnectionConfigBase]).
 * @param crudable [Crudable] instance to get data from.
 * @author Valiantsin Krus
 */
abstract class ConnectionConfigDeclaration<Connection: ConnectionConfigBase>(crudable: Crudable)
  : ConfigDeclaration<Connection>(crudable) {

  companion object {
    /** list of available connection configurables. */
    private val connectionConfigurables = mutableListOf<BoundSearchableConfigurable>()

    /** configurable joined from list of connection configurables. */
    private val collectedConfigurable by lazy {
      CollectedConfigurable(connectionConfigurables)
    }
  }

  init {
    this.getConnectionConfigurable()?.let {
      connectionConfigurables.add(it)
    }
  }

  /**
   * Provides configurable collected from all connection configurables.
   * @return desired configurable.
   */
  final override fun getConfigurable(): BoundSearchableConfigurable {
    return collectedConfigurable
  }

  /**
   * Creates single configurable that will be joined in [ConnectionConfigDeclaration.getConfigurable].
   * @return desired configurable instance.
   */
  abstract fun getConnectionConfigurable(): BoundSearchableConfigurable?

}
