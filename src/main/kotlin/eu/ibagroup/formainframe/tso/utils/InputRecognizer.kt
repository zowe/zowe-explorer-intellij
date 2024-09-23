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

import com.intellij.openapi.project.Project
import eu.ibagroup.formainframe.api.api
import eu.ibagroup.formainframe.config.connect.authToken
import eu.ibagroup.formainframe.tso.config.TSOConfigWrapper
import org.zowe.kotlinsdk.DataAPI
import io.ktor.util.*

/**
 * Utility class to be able to recognize the input command in TSO terminal
 * @param project - the current project instance
 * @param session - TSO current session for which initialize the class
 */
class InputRecognizer(
  private val project: Project?,
  private val session : TSOConfigWrapper
) {

  private var previousCommand : String = ""
  private var isProgram : Boolean = false

  var currentProgram : ProgramMessage? = null

  /**
   * Main function to recognize the command. Currently it recognizes ordinary TSO commands and REXX program execution commands
   * @param command - command to recognize
   * @return an instance of recognized message
   */
  fun recognizeMessage(command: String) : Message<*> {
    val upperCommand = command.toUpperCasePreservingASCIIRules().trim()
    if (upperCommand.isEmpty()) {
      previousCommand = upperCommand
      return OrdinaryMessage(command, session)
    }
    val parseExecStm = upperCommand.substringBefore(" ")
    if (parseExecStm == "EX" || parseExecStm == "EXEC" || previousCommand == "EX" || previousCommand == "EXEC") {
      val parseAfterExec = upperCommand.substringAfter(" ").trim()
      val parseCommaFirst = parseAfterExec.startsWith("'")
      val parseCommaSecond = parseAfterExec.substring(1).contains("'")
      if (parseCommaFirst && parseCommaSecond) {
        val parsedDataset = parseAfterExec.substringAfter("'").substringBefore("(")
        val parsedMember = parseAfterExec.substringAfter("'").substringAfter("(").substringBefore(")")
        val parsedAfterMember = parseAfterExec.substringAfter(")").trim()
        return if (parsedAfterMember == "'") {
          val response = api<DataAPI>(session.getConnectionConfig()).retrieveMemberContent(
            authorizationToken = session.getConnectionConfig().authToken,
            datasetName = parsedDataset,
            memberName = parsedMember,
          ).execute()
          val rexxComment = response.body()?.substringBefore("\n")
          val isRexx = rexxComment?.contains("REXX", ignoreCase = true)
          if (response.isSuccessful && isRexx == true) {
            isProgram = true
            previousCommand = upperCommand
            ProgramMessage(command, session, response.body())
          } else {
            previousCommand = upperCommand
            OrdinaryMessage(command, session)
          }
        } else {
          previousCommand = upperCommand
          OrdinaryMessage(command, session)
        }
      } else {
        previousCommand = upperCommand
        return OrdinaryMessage(command, session)
      }
    } else {
      previousCommand = upperCommand
      return OrdinaryMessage(command, session)
    }
  }

  /**
   * Function determines if recognized command is to be a REXX execution command
   * @return true if it is a REXX execution command. False otherwise
   */
  fun isRexxProgram() : Boolean {
    return isProgram
  }

  /**
   * Function to update the program flag in regards to program parameters list
   * @param program - if null sets the flag to false. Sets true otherwise
   */
  fun updateProgramFlag(program : ProgramMessage?) {
    isProgram = program != null
  }
}
