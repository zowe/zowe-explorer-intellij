/*
 * Copyright (c) 2024 IBA Group.
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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.explorer.actions.rexx

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.xml.util.XmlStringUtil
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.nullable
import org.zowe.explorer.utils.validateRexxArguments
import org.zowe.explorer.utils.validateTsoSessionSelection
import java.awt.Dimension
import javax.swing.*

/**
 * Execute REXX dialog represents selection for TSO session config and REXX arguments list (if any)
 */
class ExecuteRexxDialog(
  project: Project?,
  private val crudable: Crudable,
  private val connectionConfig: ConnectionConfig,
  override var state: ExecuteRexxDialogState
): StatefulDialog<ExecuteRexxDialogState>(project = project) {

  companion object {
    @JvmStatic
    fun create(project: Project?, crudable: Crudable, connectionConfig: ConnectionConfig, state: ExecuteRexxDialogState): ExecuteRexxDialog = ExecuteRexxDialog(project, crudable, connectionConfig, state)
  }

  private val suitableConnectionConfigUuids: MutableList<String> = crudable.getAll<ConnectionConfig>().toList().filter { it.url == connectionConfig.url }.map { it.uuid }.toMutableList()
  private val argumentsTooltipText = XmlStringUtil.wrapInHtml(
    "Please specify comma separated input arguments for the program you're going to run. Leave empty field if no arguments is supposed to be supplied." +
      "<br><br>For Example:" +
        "<br>&emsp; arg1,arg2,arg3  -  if there are 3 program arguments being supplied;" +
        "<br>&emsp; empty  -  if there are no input arguments."
  )
  private val tsoSessionComboBoxModel = CollectionComboBoxModel(crudable.getAll<TSOSessionConfig>().toList().filter { suitableConnectionConfigUuids.contains(it.connectionConfigUuid )})
  private lateinit var argumentsTextField: JBTextField
  private lateinit var centerPanel: DialogPanel

  override fun createCenterPanel(): JComponent {
    val sameWidthLabelsGroup = "EXECUTE_REXX_DIALOG_LABELS_WIDTH_GROUP"
    return panel {
      row {
        label("Specify TSO Session").widthGroup(sameWidthLabelsGroup)
        comboBox(
          model = tsoSessionComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.name }
        )
          .bindItem(
            {
              crudable.getAll<TSOSessionConfig>().findFirst().nullable
                .also { state.tsoSessionConfig = it }
            },
            { state.tsoSessionConfig = it }
          )
          .validationOnApply { validateTsoSessionSelection(it, crudable) }
          .align(AlignX.FILL)
          .applyToComponent {
            preferredSize = Dimension(200, preferredSize.height)
          }
      }.resizableRow()
      row {
        label("Arguments:").widthGroup(sameWidthLabelsGroup)
        textField()
          .columns(COLUMNS_MEDIUM)
          .also { argumentsTextField = it.component }
          .apply {
            validation(DialogValidation { validateRexxArguments(this.component) })
          }
          .onApply {
            state.pgmArgumentsList.addAll(argumentsTextField.text.split(","))
          }
        cell(JButton(AllIcons.General.ContextHelp)).also {
          it.component.preferredSize = Dimension(20, 20)
          it.component.minimumSize = Dimension(20, 20)
          it.component.maximumSize = Dimension(20, 20)
          it.component.isFocusPainted = false
          it.component.isBorderPainted = false
          it.component.isContentAreaFilled = false
          HelpTooltip().setDescription(argumentsTooltipText).installOn(it.component)
        }
      }.resizableRow()
    }.apply {
      minimumSize = Dimension(400, 50)
    }.also { centerPanel = it }
  }

  init {
    title = "Execute REXX Dialog"
    init()
  }
}

/**
 * Data class which represents state for the Execute REXX dialog
 */
data class ExecuteRexxDialogState(
  var tsoSessionConfig: TSOSessionConfig?,
  var pgmArgumentsList: MutableList<String> = mutableListOf()
)
