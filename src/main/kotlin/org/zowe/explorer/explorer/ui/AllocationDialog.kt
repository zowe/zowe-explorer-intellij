/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.toNullableProperty
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.selectedValueMatches
import org.zowe.explorer.common.message
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.getUsername
import org.zowe.explorer.dataops.operations.DatasetAllocationParams
import org.zowe.explorer.explorer.config.Presets
import org.zowe.explorer.utils.*
import org.zowe.kotlinsdk.AllocationUnit
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.RecordFormat
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JTextField

class AllocationDialog(project: Project?, config: ConnectionConfig, override var state: DatasetAllocationParams) :
  StatefulDialog<DatasetAllocationParams>(project = project) {

  private lateinit var recordFormatBox: JComboBox<RecordFormat>
  private lateinit var spaceUnitBox: ComboBox<AllocationUnit>
  private lateinit var datasetOrganizationBox: ComboBox<DatasetOrganization>
  private lateinit var datasetNameField: JTextField
  private lateinit var memberNameField: JTextField
  private lateinit var primaryAllocationField: JTextField
  private lateinit var secondaryAllocationField: JTextField
  private lateinit var directoryBlocksField: JTextField
  private lateinit var recordLengthField: JTextField
  private lateinit var blockSizeField: JTextField
  private lateinit var averageBlockLengthField: JTextField
  private lateinit var advancedParametersField: JTextField
  private lateinit var presetsBox: JComboBox<Presets>
  private val HLQ = getUsername(config)

  private val mainPanel by lazy {
    val sameWidthLabelsGroup = "ALLOCATION_DIALOG_LABELS_WIDTH_GROUP"
    val sameWidthComboBoxGroup = "ALLOCATION_DIALOG_COMBO_BOX_WIDTH_GROUP"
    val nonNegativeIntRange = IntRange(0, Int.MAX_VALUE - 1)
    val positiveIntRange = IntRange(1, Int.MAX_VALUE - 1)

    panel {
      row {
        label("Choose preset: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(
          listOf(
            Presets.CUSTOM_DATASET,
            Presets.SEQUENTIAL_DATASET,
            Presets.PDS_DATASET,
            Presets.PDS_WITH_EMPTY_MEMBER,
            Presets.PDS_WITH_SAMPLE_JCL_MEMBER
          )
        )
          .bindItem(state::presets.toNullableProperty())
          .also { presetsBox = it.component }
          .widthGroup(sameWidthComboBoxGroup)
          .applyToComponent { whenItemSelected { doPresetAssignment(it) } }
      }
      row {
        label("Dataset name: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::datasetName)
          .also {
            datasetNameField = it.component
            datasetNameField.text.ifEmpty { datasetNameField.text = "${HLQ}.<CHANGEME>" }
          }
          .onApply { state.datasetName = state.datasetName.uppercase() }
          .horizontalAlign(HorizontalAlign.FILL)
          .focused()
      }
      row {
        label("Member name: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::memberName)
          .also {
            memberNameField = it.component
            memberNameField.text.ifEmpty { memberNameField.text = "SAMPLE" }
          }
          .onApply { state.memberName = state.memberName.uppercase() }
          .horizontalAlign(HorizontalAlign.FILL)
      }
        .visibleIf(presetsBox.selectedValueMatches { it == Presets.PDS_WITH_EMPTY_MEMBER || it == Presets.PDS_WITH_SAMPLE_JCL_MEMBER })
      row {
        label("Dataset organization: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(
          listOf(
            DatasetOrganization.PS,
            DatasetOrganization.PO,
            DatasetOrganization.POE
          ),
          SimpleListCellRenderer.create("") {
            when (it) {
              DatasetOrganization.PS -> "Sequential (PS)"
              DatasetOrganization.PO -> "Partitioned (PO)"
              DatasetOrganization.POE -> "Partitioned Extended (PO-E)"
              else -> ""
            }
          }
        )
          .bindItem(state.allocationParameters::datasetOrganization.toNullableProperty())
          .also { datasetOrganizationBox = it.component }
          .widthGroup(sameWidthComboBoxGroup)
      }
      row {
        label("Allocation unit: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(listOf(AllocationUnit.TRK, AllocationUnit.CYL))
          .bindItem(state.allocationParameters::allocationUnit.toNullableProperty())
          .also { spaceUnitBox = it.component }
          .widthGroup(sameWidthComboBoxGroup)
        contextHelp(
          description = message("allocation.dialog.unit.size.hint.description"),
          title = message("allocation.dialog.unit.size.hint.title")
        )
      }
      row {
        label("Primary allocation: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(positiveIntRange)
          .bindText(
            { state.allocationParameters.primaryAllocation.toString() },
            { state.allocationParameters.primaryAllocation = it.toIntOrNull() ?: 0 }
          )
          .also { primaryAllocationField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Secondary allocation: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(nonNegativeIntRange)
          .bindText(
            { state.allocationParameters.secondaryAllocation.toString() },
            { state.allocationParameters.secondaryAllocation = it.toIntOrNull() ?: 0 }
          )
          .also { secondaryAllocationField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Directory: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(positiveIntRange)
          .bindText(
            {
              if (state.allocationParameters.directoryBlocks != null) {
                state.allocationParameters.directoryBlocks.toString()
              } else {
                "0"
              }
            },
            { state.allocationParameters.directoryBlocks = it.toIntOrNull() ?: 0 }
          )
          .also { directoryBlocksField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
        .visibleIf(datasetOrganizationBox.selectedValueMatches { it != DatasetOrganization.PS })
      row {
        label("Record format: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(
          listOf(
            RecordFormat.F,
            RecordFormat.FB,
            RecordFormat.V,
            RecordFormat.VA,
            RecordFormat.VB,
            RecordFormat.U,
          )
        )
          .bindItem(state.allocationParameters::recordFormat.toNullableProperty())
          .also { recordFormatBox = it.component }
          .widthGroup(sameWidthComboBoxGroup)
      }
      row {
        label("Record Length: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(positiveIntRange)
          .bindText(
            { state.allocationParameters.recordLength?.toString() ?: "0" },
            { state.allocationParameters.recordLength = it.toIntOrNull() }
          )
          .also { recordLengthField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Block size: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(nonNegativeIntRange)
          .bindText(
            { state.allocationParameters.blockSize?.toString() ?: "0" },
            { state.allocationParameters.blockSize = it.toIntOrNull() }
          )
          .also { blockSizeField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Average Block Length: ")
          .widthGroup(sameWidthLabelsGroup)
        intTextField(nonNegativeIntRange)
          .bindText(
            { state.allocationParameters.averageBlockLength?.toString() ?: "0" },
            { state.allocationParameters.averageBlockLength = it.toIntOrNull() }
          )
          .also { averageBlockLengthField = it.component }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      collapsibleGroup("Advanced Parameters", false) {
        row {
          label("Volume: ")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(
              { state.allocationParameters.volumeSerial ?: "" },
              { state.allocationParameters.volumeSerial = it }
            )
            .also { advancedParametersField = it.component }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Device Type: ")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(
              { state.allocationParameters.deviceType ?: "" },
              { state.allocationParameters.deviceType = it }
            )
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Storage class: ")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(
              { state.allocationParameters.storageClass ?: "" },
              { state.allocationParameters.storageClass = it }
            )
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Management class: ")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(
              { state.allocationParameters.managementClass ?: "" },
              { state.allocationParameters.managementClass = it }
            )
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Data class: ")
            .widthGroup(sameWidthLabelsGroup)
          textField()
            .bindText(
              { state.allocationParameters.dataClass ?: "" },
              { state.allocationParameters.dataClass = it }
            )
            .horizontalAlign(HorizontalAlign.FILL)
        }
      }
    }
  }

  override fun createCenterPanel(): JComponent {
    return JBScrollPane(
      mainPanel,
      JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
      JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    ).apply {
      minimumSize = Dimension(450, 300)
      if (state.errorMessage != "") {
        setErrorText(state.errorMessage)
      }
      border = null
    }
  }

  /**
   * Function which sets the proper values for a chosen preset from UI
   * @param preset - a chosen preset from UI
   * @return Void
   */
  private fun doPresetAssignment(preset: Presets) {
    val dataContainer = Presets.initDataClass(preset)
    memberNameField.text.ifEmpty { memberNameField.text = "SAMPLE" }
    datasetOrganizationBox.selectedItem = dataContainer.datasetOrganization
    spaceUnitBox.selectedItem = dataContainer.spaceUnit
    primaryAllocationField.text = dataContainer.primaryAllocation.toString()
    secondaryAllocationField.text = dataContainer.secondaryAllocation.toString()
    directoryBlocksField.text = dataContainer.directoryBlocks.toString()
    recordFormatBox.selectedItem = dataContainer.recordFormat
    recordLengthField.text = dataContainer.recordLength.toString()
    blockSizeField.text = dataContainer.blockSize.toString()
    averageBlockLengthField.text = dataContainer.averageBlockLength.toString()
  }

  override fun doOKAction() {
    mainPanel.apply()
    super.doOKAction()
  }

  override fun doValidate(): ValidationInfo? {
    val defaultValidationInfos =
      mainPanel.validationsOnInput.mapNotNull { it.value.first().validate() }
        .filter { it.component?.isVisible == true }
        .plus(
          mainPanel.validationsOnApply.mapNotNull { it.value.first().validate() }
            .filter { it.component?.isVisible == true }
        )
    return validateDatasetNameOnInput(datasetNameField)
      ?: validateForBlank(memberNameField)
      ?: validateMemberName(memberNameField)
      ?: defaultValidationInfos.firstOrNull()
      ?: validateVolser(advancedParametersField)
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return mainPanel.preferredFocusedComponent ?: super.getPreferredFocusedComponent()
  }

  init {
    title = "Allocate Dataset"
    init()
  }
}
