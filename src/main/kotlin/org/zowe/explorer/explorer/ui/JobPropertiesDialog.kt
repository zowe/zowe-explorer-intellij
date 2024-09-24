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

package org.zowe.explorer.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.dataops.attributes.RemoteJobAttributes
import org.zowe.explorer.utils.getParamTextValueOrUnknown
import javax.swing.JComponent

/** Class to create dialog for job file properties*/
class JobPropertiesDialog(val project: Project?, override var state: JobState) : DialogWrapper(project),
  StatefulComponent<JobState> {
  init {
    title = "Job Properties"
    init()
  }
  companion object {

    // TODO: Remove when it becomes possible to mock class constructor with init section.
    /** wrapper for constructor. It is necessary only for test purposes for now. */
    @JvmStatic
    fun create(project: Project?, state: JobState): JobPropertiesDialog = JobPropertiesDialog(project, state)

  }

  /** Create job file properties dialog and fill text fields with received job file's state*/
  override fun createCenterPanel(): JComponent {
    val job = state.remoteJobAttributes.jobInfo
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "JOB_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        row {
          label("Job ID: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.jobId)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.jobName)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Subsystem: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.subSystem))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Owner: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.owner)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Status: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.status))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job type: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.type.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job class: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.jobClass))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Return code: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.returnedCode))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job correlator: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.jobCorrelator))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      })

    tabbedPanel.add(
      "Data",
      panel {
        row {
          label("Phase: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.phase.toString())
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Phase name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.phaseName)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("URL: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.url)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Files URL: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(job.filesUrl)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("System executor: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.execSystem))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Reason not running: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.reasonNotRunning))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Run info")
            .bold()
            .widthGroup(sameWidthGroup)
        }
        row {
          label("Submitted (input end time): ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.execSubmitted))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Job start time: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.execStarted))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Time ended: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(job.execEnded))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      })

    return tabbedPanel
  }
}

/** Class to represent job file state */
class JobState(val remoteJobAttributes: RemoteJobAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState
