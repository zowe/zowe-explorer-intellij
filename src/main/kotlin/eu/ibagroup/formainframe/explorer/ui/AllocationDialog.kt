package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.selectedValueMatches
import eu.ibagroup.formainframe.common.ui.StatefulDialog
import eu.ibagroup.formainframe.dataops.operations.DatasetAllocationParams
import eu.ibagroup.formainframe.utils.*
import eu.ibagroup.r2z.AllocationUnit
import eu.ibagroup.r2z.DatasetOrganization
import eu.ibagroup.r2z.RecordFormat
import java.awt.Dimension
import javax.swing.*

class AllocationDialog(project: Project?, override var state: DatasetAllocationParams) :
  StatefulDialog<DatasetAllocationParams>(project = project) {

  private lateinit var recordFormatBox: JComboBox<RecordFormat>
  private lateinit var spaceUnitBox: ComboBox<AllocationUnit?>
  private lateinit var datasetOrganizationBox: JComboBox<DatasetOrganization>
  private lateinit var datasetNameField: JTextField
  private lateinit var primaryAllocationField: JTextField
  private lateinit var secondaryAllocationField: JTextField
  private lateinit var directoryBlocksField: JTextField
  private lateinit var recordLengthField: JTextField
  private lateinit var blockSizeField: JTextField
  private lateinit var averageBlockLengthField: JTextField
  private lateinit var advancedParametersField: JTextField

  private val mainPanel by lazy {
    panel {
      row {
        label("Dataset name")
        textField(state::datasetName)
          .apply {
          focused()
        }.also {
          datasetNameField = it.component
        }
      }
      row {
        label("Dataset organization")
        comboBox(
          model = CollectionComboBoxModel(
            listOf(
              DatasetOrganization.PS,
              DatasetOrganization.PO,
              DatasetOrganization.POE
            )
          ),
          prop = state.allocationParameters::datasetOrganization
        ).also {
          datasetOrganizationBox = it.component
        }
      }
      row {
        label("Allocation unit")
        comboBox(
          model = CollectionComboBoxModel(listOf(AllocationUnit.TRK, AllocationUnit.BLK, AllocationUnit.CYL)),
          modelBinding = PropertyBinding(
            get = { state.allocationParameters.allocationUnit },
            set = { state.allocationParameters.allocationUnit = it }
          )
        ).also { spaceUnitBox = it.component }
      }
      row {
        label("Primary allocation")
        textField(PropertyBinding(
          get = { state.allocationParameters.primaryAllocation.toString() },
          set = { state.allocationParameters.primaryAllocation = it.toIntOrNull() ?: 0 }
        )).also { primaryAllocationField = it.component }
      }
      row {
        label("Secondary allocation")
        textField(PropertyBinding(
          get = { state.allocationParameters.secondaryAllocation.toString() },
          set = { state.allocationParameters.secondaryAllocation = it.toIntOrNull() ?: 0 }
        )).also {
          secondaryAllocationField = it.component
        }
      }
      row {
        label("Directory")
        textField(PropertyBinding(
          get = { state.allocationParameters.directoryBlocks.toString() ?: "0" },
          set = { state.allocationParameters.directoryBlocks = it.toIntOrNull() ?: 0 }
        )).enableIf(datasetOrganizationBox.selectedValueMatches { it == DatasetOrganization.PO })
          .also {
            directoryBlocksField = it.component
          }
      }
      row {
        label("Record format")
        comboBox(
          model = CollectionComboBoxModel(
            listOf(
              RecordFormat.F,
              RecordFormat.FB,
              RecordFormat.V,
              RecordFormat.VA,
              RecordFormat.VB,
            )
          ),
          prop = state.allocationParameters::recordFormat
        ).also {
          recordFormatBox = it.component
        }
      }
      row {
        label("Record Length")
        textField(PropertyBinding(
          get = { state.allocationParameters.recordLength?.toString() ?: "0" },
          set = { state.allocationParameters.recordLength = it.toIntOrNull() }
        )).also {
          recordLengthField = it.component
        }
      }
      row {
        label("Block size")
        textField(PropertyBinding(
          get = { state.allocationParameters.blockSize?.toString() ?: "0" },
          set = { state.allocationParameters.blockSize = it.toIntOrNull() }
        )).also {
          blockSizeField = it.component
        }
      }
      row {
        label("Average Block Length")
        textField(PropertyBinding(
          get = { state.allocationParameters.averageBlockLength?.toString() ?: "0" },
          set = { state.allocationParameters.averageBlockLength = it.toIntOrNull() }
        )).enableIf(spaceUnitBox.selectedValueMatches { it == AllocationUnit.BLK })
          .also {
            averageBlockLengthField = it.component
          }

      }
      hideableRow("Advanced Parameters") {
        row {
          label("Volume")
          textField(PropertyBinding(
            get = { state.allocationParameters.volumeSerial ?: "" },
            set = { state.allocationParameters.volumeSerial = it }
          )).also {
            advancedParametersField = it.component
          }
        }
        row {
          label("Device Type")
          textField(PropertyBinding(
            get = { state.allocationParameters.deviceType ?: "" },
            set = { state.allocationParameters.deviceType = it }
          ))
        }
        row {
          label("Storage class")
          textField(PropertyBinding(
            get = { state.allocationParameters.storageClass ?: "" },
            set = { state.allocationParameters.storageClass = it }
          ))
        }
        row {
          label("Management class")
          textField(PropertyBinding(
            get = { state.allocationParameters.managementClass ?: "" },
            set = { state.allocationParameters.managementClass = it }
          ))
        }
        row {
          label("Data class")
          textField(PropertyBinding(
            get = { state.allocationParameters.dataClass ?: "" },
            set = { state.allocationParameters.dataClass = it }
          ))
        }
      }
    }
  }

  override fun createCenterPanel(): JComponent {
    return JBScrollPane(mainPanel).apply {
      horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
      verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
      minimumSize = Dimension(450, 300)
      if (state.errorMessage != "") {
        setErrorText(state.errorMessage)
      }
    }
  }

  override fun doOKAction() {
    super.doOKAction()
    mainPanel.apply()
  }

  override fun doValidate(): ValidationInfo? {
    super.doValidate()
    return validateDataset(
      datasetNameField,
      datasetOrganizationBox.selectedItem as DatasetOrganization,
      primaryAllocationField,
      secondaryAllocationField,
      directoryBlocksField,
      recordLengthField,
      blockSizeField,
      averageBlockLengthField,
      advancedParametersField
    )
  }

  init {
    title = "Allocate Dataset"
    init()
  }
}



