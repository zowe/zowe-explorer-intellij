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

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.utils.UNKNOWN_PARAM_VALUE
import org.zowe.explorer.utils.getParamTextValueOrUnknown
import org.zowe.kotlinsdk.DatasetOrganization
import org.zowe.kotlinsdk.HasMigrated
import javax.swing.JComponent

class DatasetPropertiesDialog(val project: Project?, override var state: DatasetState) : DialogWrapper(project),
  StatefulComponent<DatasetState> {
  init {
    title = "Dataset Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val dataset = state.datasetAttributes.datasetInfo
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "DATASET_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        row {
          label("Dataset name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(dataset.name)
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Dataset name type: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.dsnameType))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Catalog name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.catalogName))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Volume serials: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.volumeSerials))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Device type: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.deviceType))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        if (dataset.migrated?.equals(HasMigrated.YES) == true) {
          row {
            label("Dataset has migrated.")
          }
        }
      }
    )


    tabbedPanel.add(
      "Data",
      panel {
        row {
          label("Organization: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(
              when (dataset.datasetOrganization) {
                DatasetOrganization.PS -> "Sequential (PS)"
                DatasetOrganization.PO -> "Partitioned (PO)"
                DatasetOrganization.POE -> "Partitioned Extended (PO-E)"
                else -> UNKNOWN_PARAM_VALUE
              }
            )
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Record format: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.recordFormat))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Record length: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.recordLength))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Block size: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.blockSize))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Size in tracks: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.sizeInTracks))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Space units: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(dataset.spaceUnits))
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        if ("YES" == dataset.spaceOverflowIndicator) {
          row {
            label("<html><font color=\"red\">Space overflow!</font></html>")
          }
        }
      }
    )



    tabbedPanel.add("Extended", panel {
      row {
        label("Current Utilization")
          .bold()
      }
      row {
        label("Used tracks (blocks): ")
          .widthGroup(sameWidthGroup)
        textField()
          .text(getParamTextValueOrUnknown(dataset.usedTracksOrBlocks))
          .applyToComponent { isEditable = false }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Used extents: ")
          .widthGroup(sameWidthGroup)
        textField()
          .text(getParamTextValueOrUnknown(dataset.extendsUsed))
          .applyToComponent { isEditable = false }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Dates")
          .bold()
      }
      row {
        label("Creation date: ")
          .widthGroup(sameWidthGroup)
        textField()
          .text(getParamTextValueOrUnknown(dataset.creationDate))
          .applyToComponent { isEditable = false }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Referenced date: ")
          .widthGroup(sameWidthGroup)
        textField()
          .text(getParamTextValueOrUnknown(dataset.lastReferenceDate))
          .applyToComponent { isEditable = false }
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Expiration date: ")
          .widthGroup(sameWidthGroup)
        textField()
          .text(getParamTextValueOrUnknown(dataset.expirationDate))
          .applyToComponent { isEditable = false }
          .horizontalAlign(HorizontalAlign.FILL)
      }
    })

    return tabbedPanel
  }

}


class DatasetState(val datasetAttributes: RemoteDatasetAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState
