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

package eu.ibagroup.formainframe.tso.utils

import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import io.ktor.util.*

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
        val possibleParm = line.trim().contains("PULL", ignoreCase = true) && line.trim().contains("/*")
        if (possibleParm) {
          val parsedParm = line.trim().substringAfter("/*").substringBefore("*/")
          if (!parsedParm.contains("PULL", ignoreCase = true))
            pgmParameters.add(line.trim().toUpperCasePreservingASCIIRules().substringAfter("PULL").trim())
        } else if (line.trim().contains("PULL", ignoreCase = true)) {
          pgmParameters.add(line.trim().toUpperCasePreservingASCIIRules().substringAfter("PULL").trim())
        }
      }
      parameters = pgmParameters
    }
  }

}
