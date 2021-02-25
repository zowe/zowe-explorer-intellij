package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteDatasetAttributes
import eu.ibagroup.r2z.HasMigrated
import javax.swing.JComponent

class DatasetPropertiesDialog(val project: Project?, override var state: DatasetState) : DialogWrapper(project), StatefulComponent<DatasetState> {
  init {
    title = "Dataset Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val dataset = state.datasetAttributes.datasetInfo
    val tabbedPanel = JBTabbedPane()

    tabbedPanel.add("General", panel {
      row {
        label("Dataset name: ")
        JBTextField(dataset.name).apply { isEditable = false }()
      }
      row {
        label("Dataset name type: ")
        JBTextField(dataset.dsnameType?.toString() ?: "").apply { isEditable = false }()

      }
      row {
        label("Catalog name: ")
        JBTextField(dataset.catalogName ?: "").apply { isEditable = false }()

      }
      row {
        label("Volume serials: ")
        JBTextField(dataset.volumeSerials ?: "").apply { isEditable = false }()
      }
      row {
        label("Device type: ")
        JBTextField(dataset.deviceType ?: "").apply { isEditable = false }()
      }
      if (dataset.migrated?.equals(HasMigrated.YES) == true) {
        row {
          label("Dataset has migrated.")
        }
      }
    })


    tabbedPanel.add("Data", panel {
      row {
        label("Organization: ")
        JBTextField(dataset.datasetOrganization?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Record format: ")
        JBTextField(dataset.recordFormat?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Record length: ")
        JBTextField(dataset.recordLength?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Block size: ")
        JBTextField(dataset.blockSize?.toString() ?: "").apply { isEditable = false }()

      }
      row {
        label("Size in tracks: ")
        JBTextField(dataset.sizeInTracks?.toString() ?: "").apply { isEditable = false }()

      }
      row {
        label("Space units: ")
        JBTextField(dataset.spaceUnits?.toString() ?: "").apply { isEditable = false }()
      }
      if ("YES".equals(dataset.spaceOverflowIndicator)) {
        row {
          label("<html><font color=\"red\">Space overflow!</font></html>")
        }
      }
    })



    tabbedPanel.add("Extended", panel {
      row {
        label("<html><b>Current Utilization</b></html>")
      }
      row {
        label("Used tracks (blocks): ")
        JBTextField(dataset.usedTracksOrBlocks?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Used extents: ")
        JBTextField(dataset.extendsUsed?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("<html><b>Dates</b></html>")
      }
      row {
        label("Creation date: ")
        JBTextField(dataset.creationDate ?: "").apply { isEditable = false }()
      }
      row {
        label("Referenced date: ")
        JBTextField(dataset.lastReferenceDate ?: "").apply { isEditable = false }()
      }
      row {
        label("Expiration date: ")
        JBTextField(dataset.expirationDate ?: "").apply { isEditable = false }()
      }
    })

    return tabbedPanel
  }

}


class DatasetState(val datasetAttributes: RemoteDatasetAttributes, override var mode: DialogMode = DialogMode.READ) : DialogState