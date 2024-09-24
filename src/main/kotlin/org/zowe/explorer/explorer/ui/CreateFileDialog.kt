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
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import org.zowe.explorer.common.ui.StatefulDialog
import org.zowe.explorer.dataops.operations.UssAllocationParams
import org.zowe.explorer.utils.validateForBlank
import org.zowe.explorer.utils.validateUssFileName
import org.zowe.kotlinsdk.CreateUssFile
import org.zowe.kotlinsdk.FileMode
import org.zowe.kotlinsdk.FileModeValue
import org.zowe.kotlinsdk.FileType
import javax.swing.JComponent

val dummyState: CreateFileDialogState
  get() = CreateFileDialogState(
    parameters = CreateUssFile(FileType.FILE, FileMode(7, 7, 7))
  )

class CreateFileDialog(project: Project?, override var state: CreateFileDialogState = dummyState, filePath: String) :
  StatefulDialog<CreateFileDialogState>(project = project) {

  override fun createCenterPanel(): JComponent {

    val fileModeValues =
      listOf(
        FileModeValue.NONE,
        FileModeValue.READ,
        FileModeValue.WRITE,
        FileModeValue.READ_WRITE,
        FileModeValue.EXECUTE,
        FileModeValue.READ_EXECUTE,
        FileModeValue.WRITE_EXECUTE,
        FileModeValue.READ_WRITE_EXECUTE
      )

    fun Int.toFileModeValue(): FileModeValue {
      return when (this) {
        0 -> FileModeValue.NONE
        1 -> FileModeValue.EXECUTE
        2 -> FileModeValue.WRITE
        3 -> FileModeValue.WRITE_EXECUTE
        4 -> FileModeValue.READ
        5 -> FileModeValue.READ_EXECUTE
        6 -> FileModeValue.READ_WRITE
        7 -> FileModeValue.READ_WRITE_EXECUTE
        else -> FileModeValue.NONE
      }
    }

    val sameWidthLabelsGroup = "USS_CREATE_FILE_DIALOG_LABELS_WIDTH_GROUP"
    val sameWidthComboBoxGroup = "USS_CREATE_FILE_DIALOG_COMBO_BOX_WIDTH_GROUP"

    return panel {
      row {
        label("Name: ")
          .widthGroup(sameWidthLabelsGroup)
        textField()
          .bindText(state::fileName)
          .validationOnApply { validateForBlank(it) ?: validateUssFileName(it) }
          .align(AlignX.FILL)
          .focused()
      }
      row {
        label("Owner: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(fileModeValues)
          .bindItem(
            { state.parameters.mode.owner.toFileModeValue() },
            { state.parameters.mode.owner = it?.mode ?: 0 }
          )
          .widthGroup(sameWidthComboBoxGroup)
      }
      row {
        label("Group: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(fileModeValues)
          .bindItem(
            { state.parameters.mode.group.toFileModeValue() },
            { state.parameters.mode.group = it?.mode ?: 0 }
          )
          .widthGroup(sameWidthComboBoxGroup)
      }
      row {
        label("All: ")
          .widthGroup(sameWidthLabelsGroup)
        comboBox(fileModeValues)
          .bindItem(
            { state.parameters.mode.all.toFileModeValue() },
            { state.parameters.mode.all = it?.mode ?: 0 }
          )
          .widthGroup(sameWidthComboBoxGroup)
      }
    }
  }

  init {
    val type = if (state.parameters.type == FileType.DIR) "Directory" else "File"
    title = "Create $type under $filePath"
    init()
  }

}


val emptyFileState: CreateFileDialogState
  get() = CreateFileDialogState(
    parameters = CreateUssFile(
      type = FileType.FILE,
      mode = FileMode(6, 6, 6)
    )
  )

val emptyDirState: CreateFileDialogState
  get() = CreateFileDialogState(
    parameters = CreateUssFile(
      type = FileType.DIR,
      mode = FileMode(7, 7, 7)
    )
  )

data class CreateFileDialogState(
  val parameters: CreateUssFile,
  var fileName: String = "",
  var path: String = "",
)

fun CreateFileDialogState.toAllocationParams(): UssAllocationParams {
  return UssAllocationParams(parameters, fileName, path)
}
