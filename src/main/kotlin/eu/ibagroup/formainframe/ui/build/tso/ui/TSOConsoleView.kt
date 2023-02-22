/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.ui.build.tso.ui

//import com.intellij.ui.layout.cellPanel
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ExecutionConsole
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBPanel
import com.intellij.ui.dsl.builder.panel
import eu.ibagroup.formainframe.dataops.operations.MessageData
import eu.ibagroup.formainframe.dataops.operations.MessageType
import eu.ibagroup.formainframe.ui.build.TerminalCommandReceiver
import eu.ibagroup.formainframe.ui.build.tso.SESSION_COMMAND_ENTERED
import eu.ibagroup.formainframe.ui.build.tso.config.TSOConfigWrapper
import eu.ibagroup.formainframe.ui.build.tso.utils.InputRecognizer
import eu.ibagroup.formainframe.utils.sendTopic
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JComponent

/**
 * Base class which represents TSO console view tool window. Basic TSO console view UI defined here
 * @param project - represents a root project
 * @param tsoSession - wrapper instance of TSO session response
 */
class TSOConsoleView(
  private val project: Project,
  private var tsoSession: TSOConfigWrapper
) : ExecutionConsole, JBPanel<TSOConsoleView>() {

  private lateinit var tsoMessageType: JComboBox<MessageType>
  private lateinit var tsoDataType: JComboBox<MessageData>
  private val tsoWidthGroup: String = "TSO_WIDTH_GROUP"

  private val tsoMessageTypes: List<MessageType> =
    listOf(MessageType.TSO_MESSAGE, MessageType.TSO_PROMPT, MessageType.TSO_RESPONSE)
  private val tsoDataTypes: List<MessageData> =
    listOf(MessageData.DATA_DATA, MessageData.DATA_HIDDEN, MessageData.DATA_ACTION)

  private var tsoMessageTypeComboBoxModel = CollectionComboBoxModel(tsoMessageTypes)
  private var tsoDataTypeComboBoxModel = CollectionComboBoxModel(tsoDataTypes)
  private var inputRecognizer: InputRecognizer

  private val consoleView: TerminalExecutionConsole = TerminalExecutionConsole(project, null)
  private val terminalCommandReceiver: TerminalCommandReceiver = TerminalCommandReceiver(consoleView)
  private val processHandler: ProcessHandler = terminalCommandReceiver.processHandler

  /**
   * UI panel which contains 2 combo boxes of TSO message type and message data type
   */
  private val tsoPanel by lazy {
    panel {
//      cellPanel()
      row {
        label("TSO message type").widthGroup(tsoWidthGroup)
        comboBox(
          model = tsoMessageTypeComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.type }
        ).also {
          tsoMessageType = it.component
        }
      }
      row {
        label("TSO data type").widthGroup(tsoWidthGroup)
        comboBox(
          model = tsoDataTypeComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.data }
        ).also {
          tsoDataType = it.component
        }
      }
    }
  }

  /**
   * Initialization method when class is called first. Initialize some callbacks and UI place
   */
  init {
    inputRecognizer = InputRecognizer(project, tsoSession)
    terminalCommandReceiver.inputRecognizer = inputRecognizer

    terminalCommandReceiver.waitForCommandInput { enteredCommand ->
      // TODO: eliminate this (consider "log")
      println("ENTERED COMMAND: $enteredCommand")
      sendTopic(SESSION_COMMAND_ENTERED).processCommand(
        project,
        this,
        tsoSession,
        enteredCommand.trim(),
        tsoMessageType.selectedItem as MessageType,
        tsoDataType.selectedItem as MessageData,
        processHandler
      )
      terminalCommandReceiver.waitForCommandInput()
    }

    terminalCommandReceiver.initialized = true

    Disposer.register(this, consoleView)
    layout = BorderLayout()
    add(this.component, BorderLayout.WEST)
    add(this.consoleView.component, BorderLayout.CENTER)
    this.preferredFocusableComponent.requestFocusInWindow()
  }

  /**
   * Getter for TSO session wrapper class for each TSO session created
   */
  fun getTsoSession(): TSOConfigWrapper {
    return tsoSession
  }

  /**
   * Setter for TSO session wrapper class for each TSO session.
   * Also it sets input recognizer for every TSO session initialized
   */
  fun setTsoSession(session: TSOConfigWrapper) {
    tsoSession = session
    updateSessionForInputRecognizer(session)
    terminalCommandReceiver.inputRecognizer = inputRecognizer
  }

  /**
   * Function to update the session for input recognizer when the session was broken
   * @param session - new session after reconnect
   */
  private fun updateSessionForInputRecognizer(session: TSOConfigWrapper) {
    inputRecognizer = InputRecognizer(project, session)
  }

  /**
   * Getter for process handler object instance
   */
  fun getProcessHandler(): ProcessHandler {
    return processHandler
  }

  /**
   * Disposer method
   */
  override fun dispose() {
    Disposer.dispose(this)
  }

  /**
   * Getter for UI control panel
   */
  override fun getComponent(): JComponent {
    return tsoPanel
  }

  /**
   * Getter for preferred focusable UI component
   */
  override fun getPreferredFocusableComponent(): JComponent {
    return consoleView.component
  }

}