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
import org.zowe.explorer.dataops.attributes.RemoteUssAttributes
import org.zowe.kotlinsdk.FileModeValue
import javax.swing.JComponent

/** Class for USS file properties dialog */
class UssFilePropertiesDialog(project: Project?, override var state: UssFileState) : DialogWrapper(project),
  StatefulComponent<UssFileState> {

  var fileTypeName: String = "File"

  init {

    if (state.ussAttributes.isDirectory)
      fileTypeName = "Directory"
    title = "$fileTypeName Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "USS_FILE_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        row {
          label("$fileTypeName name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.name)
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Location: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.parentDirPath)
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Path: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.path)
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("$fileTypeName size: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text("${state.ussAttributes.length} bytes")
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Last modified: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.modificationTime ?: "")
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        if (state.ussAttributes.isSymlink) {
          row {
            label("Symlink to: ")
              .widthGroup(sameWidthGroup)
            textField()
              .text(state.ussAttributes.symlinkTarget ?: "")
              .applyToComponent { isEditable = false }
              .horizontalAlign(HorizontalAlign.FILL)
          }
        }
      }
    )

    tabbedPanel.add(
      "Permissions",
      panel {
        row {
          label("Owner: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.owner ?: "")
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Group: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.groupId ?: "")
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("The numeric group ID (GID): ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.gid?.toString() ?: "")
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Owner permissions: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.fileMode?.owner?.toFileModeValue().toString())
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Group permissions: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.fileMode?.group?.toFileModeValue().toString())
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          label("Permissions for all users: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(state.ussAttributes.fileMode?.all?.toFileModeValue().toString())
            .applyToComponent { isEditable = false }
            .horizontalAlign(HorizontalAlign.FILL)
        }
      }
    )
    return tabbedPanel
  }


}


class UssFileState(var ussAttributes: RemoteUssAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState

//class MemberState(var member: Member, override var mode: DialogMode = DialogMode.CREATE) : PropertiesState(mode)

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
