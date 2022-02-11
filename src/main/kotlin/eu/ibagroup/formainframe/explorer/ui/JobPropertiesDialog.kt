/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteJobAttributes
import javax.swing.JComponent

class JobPropertiesDialog(val project: Project?, override var state: JobState) : DialogWrapper(project), StatefulComponent<JobState> {
    init {
        title = "Job Properties"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val job = state.remoteJobAttributes.jobInfo
        val tabbedPanel = JBTabbedPane()

        tabbedPanel.add("General", panel {
            row {
                label("Job id: ")
                JBTextField(job.jobId).apply { isEditable = false }()
            }
            row {
                label("Job name: ")
                JBTextField(job.jobName).apply { isEditable = false }()
            }
            row {
                label("Subsystem: ")
                JBTextField(job.subSystem ?: "").apply { isEditable = false }()
            }
            row {
                label("Owner: ")
                JBTextField(job.owner).apply { isEditable = false }()
            }
            row {
                label("Status: ")
                JBTextField(job.status?.toString() ?: "").apply { isEditable = false }()
            }
            row {
                label("Job Type: ")
                JBTextField(job.type.toString()).apply { isEditable = false }()
            }
            row {
                label("Job Class: ")
                JBTextField(job.jobClass ?: "").apply { isEditable = false }()
            }
            row {
                label("Return Code: ")
                JBTextField(job.returnedCode ?: "").apply { isEditable = false }()
            }
            row {
                label("Job correlator: ")
                JBTextField(job.jobCorrelator ?: "").apply { isEditable = false }()
            }
        })

        tabbedPanel.add("Data", panel {
            row {
                label("Phase: ")
                JBTextField(job.phase).apply { isEditable = false }()
            }
            row {
                label("Phase name: ")
                JBTextField(job.phaseName).apply { isEditable = false }()
            }
            row {
                label("Url: ")
                JBTextField(job.url).apply { isEditable = false }()
            }
            row {
                label("Files url: ")
                JBTextField(job.filesUrl).apply { isEditable = false }()
            }
            row {
                label("System executor: ")
                JBTextField(job.execSystem ?: "").apply { isEditable = false }()
            }
            row {
                label("Reason not running: ")
                JBTextField(job.reasonNotRunning ?: "").apply { isEditable = false }()
            }
            row {
                label("<html><b>Run info</b></html>")
            }
            row {
                label("Submitted (input end time): ")
                JBTextField(job.execSubmitted ?: "").apply { isEditable = false }()
            }
            row {
                label("Job start time: ")
                JBTextField(job.execStarted ?: "").apply { isEditable = false }()
            }
            row {
                label("Time ended: ")
                JBTextField(job.execEnded ?: "").apply { isEditable = false }()
            }
        })

        return tabbedPanel
    }
}


class JobState(val remoteJobAttributes: RemoteJobAttributes, override var mode: DialogMode = DialogMode.READ) : DialogState
