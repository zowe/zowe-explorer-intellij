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

package org.zowe.explorer.tso.utils

import org.zowe.explorer.tso.config.TSOConfigWrapper

/**
 * Basic class for an ordinary TSO message
 * @param command - an input command
 * @param session - an instance of the TSO session where the command was issued
 */
class OrdinaryMessage(
  command : String?,
  session : TSOConfigWrapper?)
  : Message<OrdinaryMessage> {

  override val messageClass = OrdinaryMessage::class.java

  private val currentSession = session
  private val commandToBeIssued = command
  lateinit var parameters : ArrayList<String>

  /**
   * Main function to parse the parameters
   * @return Void
   */
  override fun doParse() {
    if (currentSession != null && commandToBeIssued != null)
    parameters = ArrayList(0)
  }

}
