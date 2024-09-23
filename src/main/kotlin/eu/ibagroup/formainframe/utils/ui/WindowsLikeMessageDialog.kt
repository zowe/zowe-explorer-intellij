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

package eu.ibagroup.formainframe.utils.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.messages.MessageDialog
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.event.ActionEvent
import javax.swing.*

/**
 * Windows like message dialog class which does the same stuff as @see Messages.showDialog() but
 * uses Windows based representation of the dialog buttons
 */
class WindowsLikeMessageDialog(
  project: Project?,
  parentComponent: Component?,
  message: String,
  title: String,
  options: Array<String>,
  defaultOptionIndex: Int,
  focusedOptionIndex: Int,
  icon: Icon?,
  doNotAskOption: com.intellij.openapi.ui.DoNotAskOption?,
  canBeParent: Boolean,
  helpId: String?
) : MessageDialog(
  project,
  parentComponent,
  message,
  title,
  options,
  defaultOptionIndex,
  focusedOptionIndex,
  icon,
  doNotAskOption,
  canBeParent,
  helpId) {

  companion object {
    /**
     * Static function is used to show Windows based message dialog
     */
    fun showWindowsLikeMessageDialog(project: Project?,
                                     parentComponent: Component? = null,
                                     message: String,
                                     title: String,
                                     options: Array<String>,
                                     defaultOptionIndex: Int,
                                     focusedOptionIndex: Int,
                                     icon: Icon? = null,
                                     doNotAskOption: com.intellij.openapi.ui.DoNotAskOption? = null,
                                     canBeParent: Boolean = false,
                                     helpId: String? = null): Int {
      val windowsLikeMessageDialog = WindowsLikeMessageDialog(project, parentComponent, message, title, options, defaultOptionIndex, focusedOptionIndex, icon, doNotAskOption, canBeParent, helpId)
      windowsLikeMessageDialog.show()
      return windowsLikeMessageDialog.exitCode
    }
  }

  override fun createActions(): Array<Action> {
    return mutableListOf<Action>().toTypedArray()
  }

  override fun createLeftSideActions(): Array<Action> {
    val actions = mutableListOf<Action>()
    for (i in myOptions.indices) {
      val option = myOptions[i]
      val action = object: AbstractAction(UIUtil.replaceMnemonicAmpersand(option)) {
        override fun actionPerformed(e: ActionEvent?) {
          close(i, true)
        }
      }

      if (i == myDefaultOptionIndex) {
        action.putValue(DEFAULT_ACTION, true);
      }

      if (i == myFocusedOptionIndex) {
        action.putValue(FOCUSED_ACTION, true);
      }

      UIUtil.assignMnemonic(option, action);
      actions.add(action);
    }

    if (helpId != null) {
      actions.add(helpAction);
    }
    return actions.toTypedArray();
  }

  override fun createButtonsPanel(buttons: MutableList<out JButton>): JPanel {
    return createLayoutButtonsPanel(buttons)
  }

  /**
   * Function is used to create the bottom JComponent of the message dialog containing N buttons followed one by one
   * using vertical direction
   */
  private fun createLayoutButtonsPanel(buttons: MutableList<out JButton>): JPanel {
    val buttonsPanel: JPanel = NonOpaquePanel()
    val jPanels = mutableListOf<JPanel>()
    buttonsPanel.layout = BoxLayout(buttonsPanel, BoxLayout.Y_AXIS)

    for (i in buttons.indices) {
      val button: JButton = buttons[i]
      val jPanel: JPanel = NonOpaquePanel()
      jPanel.add(button)
      jPanels.add(jPanel)
    }

    jPanels.forEach { buttonsPanel.add(it) }
    return buttonsPanel
  }

}
