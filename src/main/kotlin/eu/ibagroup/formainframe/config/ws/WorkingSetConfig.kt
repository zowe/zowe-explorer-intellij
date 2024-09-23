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

package eu.ibagroup.formainframe.config.ws

import eu.ibagroup.formainframe.utils.crudable.EntityWithUuid
import eu.ibagroup.formainframe.utils.crudable.annotations.Column

/**
 * Configuration class for Working Sets
 */
abstract class WorkingSetConfig : EntityWithUuid {
  @Column
  var name: String = ""

  abstract var connectionConfigUuid: String

  constructor()

  constructor(
    name: String,
    uuid: String
  ) : super(uuid) {
    this.name = name
  }

  constructor(
    name: String,
  ) : super() {
    this.name = name
  }
}
