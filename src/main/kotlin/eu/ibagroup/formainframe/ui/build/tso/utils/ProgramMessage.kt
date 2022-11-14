/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.ui.build.tso.utils

import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper

/**
 * Basic class for a program TSO message
 * @param command - an input command
 * @param session - an instance of the TSO session where the command was issued
 * @param programLines - discovered lines of the program
 */
class ProgramMessage(
  command : String?,
  session : TSOConfigWrapper?,
  private val programLines : String?
  ) : Message<ProgramMessage> {

  override val messageClass = ProgramMessage::class.java

  private val commandToBeIssued = command
  private val currentSession = session
  lateinit var parameters : ArrayList<String>

  /**
   * Main function to parse the parameters
   * @return Void
   */
  override fun doParse() {
    if (currentSession != null && commandToBeIssued != null) {
      val pgmParameters = ArrayList<String>()
      val lines = programLines?.lines()
      lines?.forEach { line ->
        val isParameter = line.trim().contains("PULL", ignoreCase = true)
        if (isParameter) {
          pgmParameters.add(line.trim().substringAfter("PULL").trim())
        }
      }
      parameters = pgmParameters
    }
  }

}