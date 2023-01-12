/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.ui.build

import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.terminal.TerminalExecutionConsole
import com.jediterm.terminal.TerminalKeyEncoder
import eu.ibagroup.formainframe.ui.build.tso.utils.InputRecognizer
import eu.ibagroup.formainframe.ui.build.tso.utils.ProgramMessage
import java.awt.event.KeyEvent
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.LinkedList
import java.util.Queue

/**
 * Base class to accept user input from TSO terminal console view
 */
class TerminalCommandReceiver(terminalConsole: TerminalExecutionConsole) {
  private var typedCommand = ""
  private var textAfterCursor = ""
  private var cursorPosition = 0
  private var needToWaitForCommandInput = false
  private var commandsInQueue: Queue<String> = LinkedList()
  private var expectParameters = false
  var initialized = false

  private var onCommandEntered: (String) -> Unit = {}
  private val processInput = object : ByteArrayOutputStream() {

    val terminalKeyEncoder = TerminalKeyEncoder()
    val delCode = terminalKeyEncoder.getCode(KeyEvent.VK_DELETE, 0)
    val rightCode = terminalKeyEncoder.getCode(KeyEvent.VK_RIGHT, 0)
    val leftCode = terminalKeyEncoder.getCode(KeyEvent.VK_LEFT, 0)
    val backspaceCode = terminalKeyEncoder.getCode(KeyEvent.VK_BACK_SPACE, 0)

    /**
     * Typing listener for every user interaction with the keyboard. It prints the byte array typed by user to console
     * in case the user interacts with the keyboard.
     * Note: Added possibility to recognize the command to be submitted - TSO ordinary command or execute REXX program command which expects parameters
     */
    override fun write(b: ByteArray) {
      super.write(b)

      if (!needToWaitForCommandInput) return

      if (b.contentEquals("\n".toByteArray()) || b.contentEquals("\r".toByteArray())) {
        processHandler.notifyTextAvailable("\n", ProcessOutputType.STDOUT)
        val enteredCommand = typedCommand
        stopWaitingCommand()
        val command = inputRecognizer.recognizeMessage(enteredCommand)
        val currentProgram = inputRecognizer.currentProgram
        if (inputRecognizer.currentProgram == null && inputRecognizer.isRexxProgram()) {
          inputRecognizer.currentProgram = command as ProgramMessage
          command.doParse()
          expectParameters = command.parameters.isNotEmpty()
        }
        if (expectParameters && currentProgram != null) {
          if (currentProgram.parameters.isNotEmpty()) {
            currentProgram.parameters.removeAt(0)
            if (currentProgram.parameters.isEmpty()) {
              expectParameters = false
              inputRecognizer.currentProgram = null
              inputRecognizer.updateProgramFlag(null)
            }
          } else {
            expectParameters = false
            inputRecognizer.currentProgram = null
            inputRecognizer.updateProgramFlag(null)
          }
        }
        onCommandEntered(enteredCommand)
      } else if (b.contentEquals(backspaceCode)) {
        if (typedCommand == "") return
        processHandler.notifyTextAvailable(
          "\b \b${textAfterCursor} ${"\b".repeat(textAfterCursor.length + 1)}",
          ProcessOutputType.STDOUT
        )
        typedCommand = typedCommand.substring(0, cursorPosition - 1) + typedCommand.substring(cursorPosition)
        --cursorPosition
      } else if (b.contentEquals(delCode)) {
        if (textAfterCursor == "") return
        textAfterCursor = textAfterCursor.substring(1)
        typedCommand = typedCommand.substring(0, cursorPosition) + typedCommand.substring(cursorPosition + 1)
        processHandler.notifyTextAvailable(
          "$textAfterCursor ${"\b".repeat(textAfterCursor.length + 1)}",
          ProcessOutputType.STDOUT
        )
      } else if (b.contentEquals(leftCode)) {
        if (cursorPosition == 0) return
        textAfterCursor = typedCommand.substring(--cursorPosition)
        processHandler.notifyTextAvailable("\b", ProcessOutputType.STDOUT)
      } else if (b.contentEquals(rightCode)) {
        if (cursorPosition == typedCommand.length) return
        val charToType = textAfterCursor.first()
        textAfterCursor = if (++cursorPosition >= typedCommand.length) "" else typedCommand.substring(cursorPosition)
        processHandler.notifyTextAvailable(charToType.toString(), ProcessOutputType.STDOUT)
      } else {
        val regexToCheckSymbol = Regex("[ \\n\\r!@#\$^%&*()_+=|<>,.;'\"/?\\\\{}\\[\\]~-]")
        if (!b.all { it.toInt().toChar().isLetterOrDigit() || it.toInt().toChar().toString().contains(regexToCheckSymbol) }) return
        val input = typedCommand.substring(0, cursorPosition) + String(b) + typedCommand.substring(cursorPosition)
        if (input.contains(Regex("[\n\r]"))) {
          val tokens = input.split(Regex("[\n\r]")).toMutableList()
          val enteredCommand = tokens[0]
          tokens.removeAt(0)
          commandsInQueue.addAll(tokens)
          val partBeforeNewString = String(b).split(Regex("[\n\r]"))[0]
          val spaceNeeded = if (textAfterCursor.length > partBeforeNewString.length) textAfterCursor.length - partBeforeNewString.length else 0
          processHandler.notifyTextAvailable(partBeforeNewString + " ".repeat(spaceNeeded) + "\n\r", ProcessOutputType.STDOUT)
          stopWaitingCommand()
          onCommandEntered(enteredCommand)
        } else {
          typedCommand = input
          cursorPosition += b.size
          processHandler.notifyTextAvailable(
            String(b) + textAfterCursor + "\b".repeat(textAfterCursor.length),
            ProcessOutputType.STDOUT
          )
        }
      }
    }
  }

  lateinit var inputRecognizer: InputRecognizer

  val processHandler: ProcessHandler

  /**
   * Base process handler object initialization. It will accept the user input commands and process theirs output
   */
  init {
    processHandler = object : NopProcessHandler() {
      /**
       * Override getProcessInput() method to process the user input from the keyboard
       */
      override fun getProcessInput(): OutputStream {
        return this@TerminalCommandReceiver.processInput
      }
    }
    terminalConsole.withConvertLfToCrlfForNonPtyProcess(true)
    terminalConsole.attachToProcess(processHandler)
  }

  /**
   * Called when command is submitted. Clean up the entered command for follow up user input
   */
  private fun cleanCommand() {
    this.typedCommand = ""
    this.textAfterCursor = ""
    this.cursorPosition = 0
  }

  /**
   * Called for every user interaction with the keyboard.
   */
  fun waitForCommandInput () {
    waitForCommandInput(onCommandEntered)
  }

  fun waitForCommandInput(onCommandEntered: (String) -> Unit) {
    if (commandsInQueue.size > 1) {
      val nextCommand = commandsInQueue.remove()
      processHandler.notifyTextAvailable("$nextCommand\n", ProcessOutputType.STDOUT)
      onCommandEntered(nextCommand)
      return
    }
    this.onCommandEntered = onCommandEntered
    if (commandsInQueue.size == 1) {
      typedCommand = commandsInQueue.remove()
      cursorPosition = this.typedCommand.length
      textAfterCursor = ""
      processHandler.notifyTextAvailable(typedCommand, ProcessOutputType.STDOUT)
    } else {
      this.cleanCommand()
    }
    this.needToWaitForCommandInput = true
    if(initialized) {
      if (!expectParameters) {
        processHandler.notifyTextAvailable("> ", ProcessOutputType.STDOUT)
      } else {
        processHandler.notifyTextAvailable("", ProcessOutputType.STDOUT)
      }
    }
  }

  /**
   * Called when user finished typing the command and pressed Enter
   */
  fun stopWaitingCommand() {
    this.needToWaitForCommandInput = false
    this.cleanCommand()
  }

}