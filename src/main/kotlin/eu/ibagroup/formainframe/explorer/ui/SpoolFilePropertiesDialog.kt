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

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteSpoolFileAttributes
import eu.ibagroup.formainframe.utils.getParamTextValueOrUnknown
import javax.swing.JComponent

/** Class to create dialog for spool file properties*/
class SpoolFilePropertiesDialog(val project: Project?, override var state: SpoolFileState) : DialogWrapper(project),
  StatefulComponent<SpoolFileState> {
  init {
    title = "Spool File Properties"
    init()
  }

  companion object {

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** wrapper for constructor. It is necessary only for test purposes for now. */
    @JvmStatic
    fun create(project: Project?, state: SpoolFileState): SpoolFilePropertiesDialog =
        SpoolFilePropertiesDialog(project, state)

  }

  /** Create spool file properties dialog and fill text fields with received spool file's state*/
  override fun createCenterPanel(): JComponent {
    val spoolFile = state.remoteSpoolFileAttributes.info
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "SPOOL_FILE_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        row {
          label("Job ID: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.jobId)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.jobname)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job correlator: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(spoolFile.jobCorrelator))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Class: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.fileClass)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("ID: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.id.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("DD name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.ddName)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Step name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(spoolFile.stepName))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Process step: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(spoolFile.procStep))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      })
    tabbedPanel.add(
      "Data",
      panel {
        row {
          label("Record format: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.recfm)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Byte content: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.byteCount.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Record count: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.recordCount.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Record URL: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.recordsUrl)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Record length: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(spoolFile.recordLength.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Subsystem: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(spoolFile.subsystem))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      })
    return tabbedPanel
  }
}

/** Class to represent spool file state */
class SpoolFileState(
  val remoteSpoolFileAttributes: RemoteSpoolFileAttributes,
  override var mode: DialogMode = DialogMode.READ
) : DialogState
