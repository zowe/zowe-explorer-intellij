/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.config.ws.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.containers.isEmpty
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.common.ui.ValidatingTableView
import org.zowe.explorer.common.ui.toolbarTable
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.WorkingSetConfig
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.getAll
import org.zowe.explorer.utils.crudable.getByUniqueKey
import org.zowe.explorer.utils.findAnyNullable
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateWorkingSetName
import java.awt.Dimension
import javax.swing.JComponent
import kotlin.streams.toList

abstract class AbstractWsDialog<WSConfig: WorkingSetConfig, TableRow, WSDState : AbstractWsDialogState<WSConfig, TableRow>>(
  crudable: Crudable,
  wsdStateClass: Class<out WSDState>,
  override var state: WSDState,
  var initialState: WSDState = state.clone(wsdStateClass)
) : DialogWrapper(false), StatefulComponent<WSDState> {
  abstract val wsConfigClass: Class<out WSConfig>

  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  abstract val tableTitle: String

  abstract val masksTable: ValidatingTableView<TableRow>

  abstract val wsNameLabel: String

  abstract fun emptyTableRow(): TableRow

  abstract fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo?

  open fun onWSApplyed(state: WSDState): WSDState = state

  private val panel by lazy {
    panel {
      row {
        label(wsNameLabel)
        textField(getter = { state.workingSetName }, setter = { state.workingSetName = it })
          .withValidationOnInput {
            validateWorkingSetName(
              it,
              initialState.workingSetName.ifBlank { null },
              crudable,
              wsConfigClass
            )
          }
          .withValidationOnApply {
            validateForBlank(it)
          }
      }
      row {
        label("Specify connection")
        comboBox(
          model = connectionComboBoxModel,
          modelBinding = PropertyBinding(
            get = {
              return@PropertyBinding crudable.getByUniqueKey<ConnectionConfig>(state.connectionUuid)
                ?: if (!crudable.getAll<ConnectionConfig>().isEmpty()) {
                  crudable.getAll<ConnectionConfig>().findAnyNullable()?.also {
                    state.connectionUuid = it.uuid
                  }
                } else {
                  null
                }
            },
            set = { config -> state.connectionUuid = config?.uuid ?: "" }
          ),
          renderer = SimpleListCellRenderer.create("") { it?.name }
        ).withValidationOnApply {
          if (it.selectedItem == null) {
            ValidationInfo("You must provide a connection", it)
          } else {
            null
          }
        }

      }
      row {
        toolbarTable(tableTitle, masksTable, addDefaultActions = true) {
          addNewItemProducer { emptyTableRow() }
        }.withValidationOnApply {
          validateOnApply(this, it)
        }.onApply {
          state.maskRow = masksTable.items
          state = onWSApplyed(state)
        }
      }
    }.apply {
      minimumSize = Dimension(450, 500)
    }
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }
}
