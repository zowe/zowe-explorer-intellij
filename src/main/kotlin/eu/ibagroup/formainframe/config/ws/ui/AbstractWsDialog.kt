/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.ws.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.containers.isEmpty
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.common.ui.ValidatingTableView
import eu.ibagroup.formainframe.common.ui.tableWithToolbar
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.WorkingSetConfig
import eu.ibagroup.formainframe.utils.clone
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.validateForBlank
import eu.ibagroup.formainframe.utils.validateWorkingSetName
import java.awt.Dimension
import javax.swing.JComponent
import kotlin.streams.toList

/**
 * Abstract class for displaying configuration dialog of single Working Set.
 * @param WSConfig Implementation class of WorkingSetConfig
 * @see WorkingSetConfig
 * @param TableRow Class with data for each column of filters/masks table (for example mask, system type, ...)
 * @param WSDState Implementation of AbstractWsDialogState
 * @see AbstractWsDialogState
 * @param crudable Crudable instance to change data in after dialog applied.
 * @param wsdStateClass Instance of Class for WSDState
 * @param state Instance of WSDState
 * @param initialState Initial state of dialog. (used only working set name from initial state ???)
 * @author Valiantsin Krus
 * @author Viktar Mushtsin
 */
abstract class AbstractWsDialog<WSConfig : WorkingSetConfig, TableRow, WSDState : AbstractWsDialogState<WSConfig, TableRow>>(
  crudable: Crudable,
  wsdStateClass: Class<out WSDState>,
  override var state: WSDState,
  var initialState: WSDState = state.clone(wsdStateClass)
) : DialogWrapper(false), StatefulComponent<WSDState> {

  abstract val wsConfigClass: Class<out WSConfig>

  private val connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())

  /**
   * Name of masks table.
   */
  abstract val tableTitle: String

  /**
   * Represents table of files masks (for Files Working Sets) or job filters (for JES Working Sets)
   */
  abstract val masksTable: ValidatingTableView<TableRow>

  /**
   * Value of text label before Working Set Name input field.
   */
  abstract val wsNameLabel: String

  abstract fun emptyTableRow(): TableRow

  abstract fun validateOnApply(validationBuilder: ValidationInfoBuilder, component: JComponent): ValidationInfo?

  /**
   * Custom apply for specific WorkingSet implementation (e.g. Files Working Set, JES Working Set).
   * @param state State, modified from dialog, to apply.
   * @return applied state.
   */
  open fun onWSApplied(state: WSDState): WSDState = state

  private val panel by lazy {
    panel {
      row {
        label(wsNameLabel)
        textField()
          .bindText(state::workingSetName)
          .validationOnApply {
            validateForBlank(it) ?: validateWorkingSetName(
              it,
              initialState.workingSetName.ifBlank { null },
              crudable,
              wsConfigClass
            )
          }
      }
      row {
        label("Specify connection")
        comboBox(connectionComboBoxModel, SimpleListCellRenderer.create("") { it?.name })
          .bindItem(
            {
              return@bindItem crudable.getByUniqueKey<ConnectionConfig>(state.connectionUuid)
                ?: if (!crudable.getAll<ConnectionConfig>().isEmpty()) {
                  crudable.getAll<ConnectionConfig>().findAnyNullable()?.also {
                    state.connectionUuid = it.uuid
                  }
                } else {
                  null
                }
            },
            { config -> state.connectionUuid = config?.uuid ?: "" }
          )
          .validationOnApply {
            if (it.selectedItem == null) {
              ValidationInfo("You must provide a connection", it)
            } else {
              null
            }
          }
      }
      group(tableTitle, false) {
        row {
          tableWithToolbar(masksTable, addDefaultActions = true) {
            addNewItemProducer { emptyTableRow() }
          }
            .validationRequestor { }
            .validationOnApply { validateOnApply(this, it) }
            .onApply {
              state.maskRow = masksTable.items
              state = onWSApplied(state)
            }
        }
          .resizableRow()
      }
        .resizableRow()
    }
      .apply {
        minimumSize = Dimension(450, 500)
      }
  }

  override fun createCenterPanel(): JComponent {
    return panel
  }
}
