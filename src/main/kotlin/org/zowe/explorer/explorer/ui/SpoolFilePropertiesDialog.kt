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
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import org.zowe.explorer.common.ui.DialogMode
import org.zowe.explorer.common.ui.DialogState
import org.zowe.explorer.common.ui.StatefulComponent
import org.zowe.explorer.dataops.attributes.RemoteSpoolFileAttributes
import javax.swing.JComponent

class SpoolFilePropertiesDialog(val project: Project?, override var state: SpoolFileState) : DialogWrapper(project), StatefulComponent<SpoolFileState> {
    init {
        title = "Spool File Properties"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val spoolFile = state.remoteSpoolFileAttributes.info
        val tabbedPanel = JBTabbedPane()

        tabbedPanel.add("General", panel {
            row {
                label("Job id: ")
                JBTextField(spoolFile.jobId).apply { isEditable = false }()
            }
            row {
                label("Job name: ")
                JBTextField(spoolFile.jobname).apply { isEditable = false }()
            }
            row {
                label("Job correlator: ")
                JBTextField(spoolFile.jobCorrelator ?: "").apply { isEditable = false }()
            }
            row {
                label("Class: ")
                JBTextField(spoolFile.fileClass).apply { isEditable = false }()
            }
            row {
                label("ID: ")
                JBTextField(spoolFile.id).apply { isEditable = false }()
            }
            row {
                label("DD name: ")
                JBTextField(spoolFile.ddName).apply { isEditable = false }()
            }
            row {
                label("Step name: ")
                JBTextField(spoolFile.stepName ?: "").apply { isEditable = false }()
            }
            row {
                label("Process step: ")
                JBTextField(spoolFile.procStep ?: "").apply { isEditable = false }()
            }
        })
        tabbedPanel.add("Data", panel {
            row {
                label("Record format: ")
                JBTextField(spoolFile.recfm).apply { isEditable = false }()
            }
            row {
                label("Byte count: ")
                JBTextField(spoolFile.byteCount).apply { isEditable = false }()
            }
            row {
                label("Record count: ")
                JBTextField(spoolFile.recordCount).apply { isEditable = false }()
            }
            row {
                label("Records url: ")
                JBTextField(spoolFile.recordsUrl).apply { isEditable = false }()
            }
            row {
                label("Record length: ")
                JBTextField(spoolFile.recordLength).apply { isEditable = false }()
            }
            row {
                label("Subsystem: ")
                JBTextField(spoolFile.subsystem ?: "").apply { isEditable = false }()
            }
        })
        return tabbedPanel
    }
}


class SpoolFileState(val remoteSpoolFileAttributes: RemoteSpoolFileAttributes, override var mode: DialogMode = DialogMode.READ) : DialogState
