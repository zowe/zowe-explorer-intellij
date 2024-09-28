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

package eu.ibagroup.formainframe.tso.config.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.formainframe.utils.crudable.Crudable
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.crudable.getByUniqueKey
import org.zowe.kotlinsdk.TsoCodePage
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField

/**
 * Class represents the TSO session creation dialog with its initial default values
 * @param project - a root project
 * @param state - a current dialog state
 */
class TSOSessionDialog(
  private val crudable: Crudable,
  override var state: TSOSessionDialogState,
  project: Project? = null,
) : StatefulDialog<TSOSessionDialogState>(project = project) {

  private val initialState = state.clone()

  private lateinit var sessionNameField: JTextField
  private lateinit var logonProcField: JTextField
  private lateinit var charsetField: JTextField
  private lateinit var codepageField: JComboBox<TsoCodePage>
  private lateinit var rowsField: JTextField
  private lateinit var colsField: JTextField
  private lateinit var acctField: JTextField
  private lateinit var userGroupField: JTextField
  private lateinit var regionField: JTextField
  private lateinit var timeoutField: JTextField
  private lateinit var maxAttemptsField: JTextField
  private lateinit var connectionBox: JComboBox<ConnectionConfig>

  private var connectionComboBoxModel = CollectionComboBoxModel(crudable.getAll<ConnectionConfig>().toList())
  private var codepageComboBoxModel = CollectionComboBoxModel(TsoCodePage.entries)

  /**
   * Represents an UI panel with values
   */
  private val mainPanel by lazy {
    val defaultWidthLabelsGroup = "DEFAULT_DIALOG_LABELS_WIDTH_GROUP"
    panel {
      row {
        label("Session name")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::name)
          .also { sessionNameField = it.component }
          .focused()
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply {
            validateForBlank(it) ?: validateTsoSessionName(
              it, initialState.name.ifEmpty { null }, crudable
            )
          }
      }
        .resizableRow()
      row {
        label("Specify z/OSMF connection")
          .widthGroup(defaultWidthLabelsGroup)
        comboBox(
          model = connectionComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.name }
        )
          .bindItem(
            {
              crudable.getByUniqueKey<ConnectionConfig>(state.connectionConfigUuid)
                ?: crudable.getAll<ConnectionConfig>().findFirst().nullable
                  ?.also { state.connectionConfigUuid = it.uuid }
            },
            { state.connectionConfigUuid = it?.uuid ?: "" }
          )
          .also { connectionBox = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateConnectionSelection(it) }
      }
        .resizableRow()
      row {
        label("Logon procedure")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::logonProcedure)
          .also { logonProcField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) }
      }
        .resizableRow()
      row {
        label("Character set")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::charset)
          .also { charsetField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
        .resizableRow()
      row {
        label("Codepage")
          .widthGroup(defaultWidthLabelsGroup)
        comboBox(
          model = codepageComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it.codePage })
          .bindItem(state::codepage.toNullableProperty())
          .also { codepageField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
      }
        .resizableRow()
      row {
        label("Screen rows")
          .widthGroup(defaultWidthLabelsGroup)
        intTextField()
          .bindIntText(state::rows)
          .also { rowsField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
        .resizableRow()
      row {
        label("Screen columns")
          .widthGroup(defaultWidthLabelsGroup)
        intTextField()
          .bindIntText(state::columns)
          .also { colsField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
        .resizableRow()
      row {
        label("Account number")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::accountNumber)
          .also { acctField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) }
      }
        .resizableRow()
      row {
        label("User group")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::userGroup)
          .also { userGroupField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) }
      }
        .resizableRow()
      row {
        label("Region size")
          .widthGroup(defaultWidthLabelsGroup)
        intTextField()
          .bindIntText(state::regionSize)
          .also { regionField = it.component }
          .align(AlignX.FILL.plus(AlignY.FILL))
          .validationOnApply { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
        .resizableRow()
      collapsibleGroup("Advanced Parameters", false) {
        row {
          label("Reconnect timeout (seconds)")
            .widthGroup(defaultWidthLabelsGroup)
          textField()
            .bindText(
              { state.timeout.toString() },
              { state.timeout = it.toLong() }
            )
            .also { timeoutField = it.component }
            .align(AlignX.FILL.plus(AlignY.FILL))
            .validationOnApply { validateForBlank(it) ?: validateForPositiveLong(it) }
        }
          .resizableRow()
        row {
          label("Reconnect max attempts")
            .widthGroup(defaultWidthLabelsGroup)
          intTextField()
            .bindIntText(state::maxAttempts)
            .also { maxAttemptsField = it.component }
            .align(AlignX.FILL.plus(AlignY.FILL))
            .validationOnApply { validateForBlank(it) ?: validateForPositiveInteger(it) }
        }
          .resizableRow()
      }
      row {
        button("Reset Default Values", actionListener = { resetToDefault() })
      }
    }
  }

  /**
   * Resets the current dialog state to default values
   */
  private fun resetToDefault() {
    val defaultState = TSOSessionDialogState()
    connectionComboBoxModel.selectedItem = crudable.getAll<ConnectionConfig>().findFirst().nullable
    logonProcField.text = defaultState.logonProcedure
    charsetField.text = defaultState.charset
    codepageField.selectedItem = defaultState.codepage
    rowsField.text = defaultState.rows.toString()
    colsField.text = defaultState.columns.toString()
    acctField.text = defaultState.accountNumber
    userGroupField.text = defaultState.userGroup
    regionField.text = defaultState.regionSize.toString()
    timeoutField.text = defaultState.timeout.toString()
    maxAttemptsField.text = defaultState.maxAttempts.toString()
  }

  /**
   * Superclass method to create the UI panel
   */
  override fun createCenterPanel(): JComponent {
    return JBScrollPane(
      mainPanel,
      JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {
      minimumSize = Dimension(450, 400)
      border = null
    }
  }

  /**
   * Listener for OK pressed button
   */
  override fun doOKAction() {
    mainPanel.apply()
    super.doOKAction()
  }

  /**
   * Overloaded method to validate components in the main panel
   */
  override fun doValidate(): ValidationInfo? {
    return mainPanel.validateAll().firstOrNull() ?: super.doValidate()
  }

  /**
   * Overloaded method to get focused component of the main panel
   */
  override fun getPreferredFocusedComponent(): JComponent? {
    return mainPanel.preferredFocusedComponent ?: super.getPreferredFocusedComponent()
  }

  /**
   * Init method. It's called first when an instance is going to be created
   */
  init {
    title = when (state.mode) {
      DialogMode.CREATE -> "Add TSO Session"
      else -> "Edit TSO Session"
    }
    mainPanel.registerValidators(myDisposable) { map ->
      isOKActionEnabled = map.isEmpty()
    }
    initialize { init() }
  }
}
