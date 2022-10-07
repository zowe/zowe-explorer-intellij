/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws

import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.utils.crudable.EntityWithUuid
import org.zowe.explorer.utils.crudable.annotations.Column
import org.zowe.explorer.utils.crudable.annotations.ForeignKey

/**
 * Configuration class for Working Sets
 */
open class WorkingSetConfig : EntityWithUuid {
  @Column
  var name: String = ""

  @Column
  @ForeignKey(foreignClass = ConnectionConfig::class)
  var connectionConfigUuid: String = ""

  constructor()

  constructor(
    name: String,
    connectionConfigUuid: String,
    uuid: String
  ) : super(uuid) {
    this.name = name
    this.connectionConfigUuid = connectionConfigUuid
  }

  constructor(
    name: String,
    connectionConfigUuid: String
  ) : super() {
    this.name = name
    this.connectionConfigUuid = connectionConfigUuid
  }
}
