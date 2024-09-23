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

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column

/**
 * Abstract class to implement any connection config that could be integrated in plugin.
 */
abstract class ConnectionConfigBase : EntityWithUuid {
  /** Connection name. Should be annotated with [Column] in implementation. */
  abstract var name: String

  /** Connection url. Could be annotated with [Column] in implementation. */
  abstract val url: String

  constructor()

  constructor(uuid: String): super(uuid)

}
