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

import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.utils.crudable.getAll
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.r2z.TsoCodePage
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField
import kotlin.streams.toList

/**
 * Class represents the TSO session creation dialog with its initial default values
 * @param project - a root project
 * @param state - a current dialog state
 */
class TSOSessionDialog(project: Project?, override var state: TSOSessionParams) :
  StatefulDialog<TSOSessionParams>(project = project) {

  private lateinit var logonProcField: JTextField
  private lateinit var charsetField: JTextField
  private lateinit var codepageField: JComboBox<TsoCodePage>
  private lateinit var rowsField: JTextField
  private lateinit var colsField: JTextField
  private lateinit var acctField: JTextField
  private lateinit var userGroupField: JTextField
  private lateinit var regionField: JTextField
  private lateinit var connectionBox: JComboBox<ConnectionConfig>

  private var connectionComboBoxModel = CollectionComboBoxModel(configCrudable.getAll<ConnectionConfig>().toList())
  private var codepageComboBoxModel = CollectionComboBoxModel(TsoCodePage.values().toList())

  /**
   * Represents an UI panel with values
   */
  private val mainPanel by lazy {
    val defaultWidthLabelsGroup = "DEFAULT_DIALOG_LABELS_WIDTH_GROUP"
    panel {
      row {
        label("Specify z/OSMF connection")
          .widthGroup(defaultWidthLabelsGroup)
        comboBox(
          model = connectionComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it?.name }
          ).bindItem(state::connectionConfig.toNullableProperty())
          .also {
            connectionBox = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
        }
      }
      row {
        label("Logon procedure")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::logonproc)
          .apply {
            focused()
          }.also {
            logonProcField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) }
      }
      row {
        label("Character set")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::charset)
          .also {
            charsetField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
      row {
        label("Codepage")
          .widthGroup(defaultWidthLabelsGroup)
        comboBox(
          model = codepageComboBoxModel,
          renderer = SimpleListCellRenderer.create("") { it?.codePage })
          .bindItem(state::codepage.toNullableProperty())
          .also {
            codepageField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }
      }
      row {
        label("Screen rows")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::rows)
          .also {
            rowsField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
      row {
        label("Screen columns")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::cols)
          .also {
            colsField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
      row {
        label("Account number")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::acct)
          .also {
            acctField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) }
      }
      row {
        label("User group")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::usergroup)
          .also {
            userGroupField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) }
      }
      row {
        label("Region size")
          .widthGroup(defaultWidthLabelsGroup)
        textField()
          .bindText(state::region)
          .also {
            regionField = it.component
            resizableRow()
            it.verticalAlign(VerticalAlign.FILL)
            it.horizontalAlign(HorizontalAlign.FILL)
          }.validationOnInput { validateForBlank(it) ?: validateForPositiveInteger(it) }
      }
      row {
        button("Reset Default Values", actionListener = { resetToDefault() })
      }
    }.apply {
      minimumSize = Dimension(450, 400)
    }
  }

  /**
   * Resets the current dialog state to default values
   */
  private fun resetToDefault() {
    val defaultParams = TSOSessionParams()
    logonProcField.text = defaultParams.logonproc
    charsetField.text = defaultParams.charset
    codepageField.selectedItem = defaultParams.codepage
    rowsField.text = defaultParams.rows
    colsField.text = defaultParams.cols
    acctField.text = defaultParams.acct
    userGroupField.text = defaultParams.usergroup
    regionField.text = defaultParams.region
  }

  /**
   * Superclass method to create the UI panel
   */
  override fun createCenterPanel(): JComponent {
    return mainPanel
  }

  /**
   * Listener for OK pressed button
   */
  override fun doOKAction() {
    super.doOKAction()
    mainPanel.apply()
  }

  /**
   * Init method. It's called first when an instance is going to be created
   */
  init {
    title = "Establish TSO Session"
    init()
  }
}

/**
 * Data class represents the initial state of the dialog. It sets the default parameters for the TSO session.
 */
data class TSOSessionParams(
  var connectionConfig : ConnectionConfig = configCrudable.getAll(ConnectionConfig::class.java).findFirst().get(),
  var logonproc : String = "DBSPROCC",
  var charset : String = "697",
  var codepage : TsoCodePage = TsoCodePage.IBM_1047,
  var rows : String = "24",
  var cols : String = "80",
  var acct : String = "ACCT#",
  var usergroup : String = "GROUP1",
  var region : String = "64000"
)