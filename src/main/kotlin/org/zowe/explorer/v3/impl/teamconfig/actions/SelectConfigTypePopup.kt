/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButtonWithText
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.JBUI
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import java.util.Locale.getDefault
import javax.swing.JComponent

// TODO: doc
class SelectConfigTypePopup : ToggleAction(), CustomComponentAction, DumbAware {

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun isSelected(e: AnActionEvent): Boolean {
    return Toggleable.isSelected(e.presentation)
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    if (!state) return
    val component = e.inputEvent?.component as? JComponent ?: return
    val presentation = e.presentation
    val actionGroup = DefaultActionGroup().apply {
      ConfigType.entries.forEach { configType ->
        add(
          SelectConfigTypeAction(
            configType.displayName.replaceFirstChar {
              if (it.isLowerCase()) it.titlecase(getDefault())
              else it.toString()
            }
          )
        )
      }
    }
    val disposeCallback = { Toggleable.setSelected(presentation, false) }
    val popup = JBPopupFactory.getInstance()
      .createActionGroupPopup(
        null,
        actionGroup,
        e.dataContext,
        false,
        true,
        false,
        disposeCallback,
        30,
        null
      )
    popup.showUnderneathOf(component)
  }

  override fun update(e: AnActionEvent) {
    // TODO: implement basing on the actually selected config type
    e.presentation.putClientProperty(ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
    e.presentation.text = "Config: local (team)"
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
    return object : ActionButtonWithText(this, presentation, place, JBUI.size(0, 24)) {
      override fun shallPaintDownArrow() = true
    }
  }

}
