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

import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteUssAttributes
import javax.swing.JComponent
import com.intellij.ui.dsl.builder.*
import eu.ibagroup.formainframe.dataops.content.synchronizer.DEFAULT_BINARY_CHARSET
import eu.ibagroup.formainframe.utils.getSupportedEncodings
import eu.ibagroup.r2z.*
import java.nio.charset.Charset

/** Class for USS file properties dialog */
class UssFilePropertiesDialog(project: Project?, override var state: UssFileState) : DialogWrapper(project),
  StatefulComponent<UssFileState> {

  private val sameWidthGroup = "USS_FILE_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

  private lateinit var generalTab: DialogPanel

  private lateinit var permissionTab: DialogPanel

  private lateinit var comboBox: Cell<ComboBox<Charset>>

  var fileTypeName: String = "File"

  private val fileModeValues = listOf(
    FileModeValue.NONE,
    FileModeValue.EXECUTE,
    FileModeValue.WRITE,
    FileModeValue.WRITE_EXECUTE,
    FileModeValue.READ,
    FileModeValue.READ_EXECUTE,
    FileModeValue.READ_WRITE,
    FileModeValue.READ_WRITE_EXECUTE
  )

  init {

    if (state.ussAttributes.isDirectory)
      fileTypeName = "Directory"
    title = "$fileTypeName Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val tabbedPanel = JBTabbedPane()

    generalTab = panel {
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
      if (!state.ussAttributes.isDirectory && state.fileIsBeingEditingNow) {
        row {
          label("File encoding:").widthGroup(sameWidthGroup)
          comboBox = comboBox(getSupportedEncodings())
            .bindItem(state.ussAttributes::charset.toNullableProperty())
            .horizontalAlign(HorizontalAlign.FILL)
        }
        row {
          button("Reset Default Encoding", EmptyAction()) //TODO: EmptyAction()?
            .widthGroup(sameWidthGroup)
            .applyToComponent {
              addActionListener {
                state.ussAttributes.charset = DEFAULT_BINARY_CHARSET
                comboBox.component.item = DEFAULT_BINARY_CHARSET
              }
            }
        }
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

    permissionTab = panel {
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
        comboBox(fileModeValues)
          .bindItem(
            { state.ussAttributes.fileMode?.owner?.toFileModeValue() },
            { state.ussAttributes.fileMode?.owner = it?.mode ?: 0 }
          )
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Group permissions: ")
          .widthGroup(sameWidthGroup)
        comboBox(fileModeValues)
          .bindItem(
            { state.ussAttributes.fileMode?.group?.toFileModeValue() },
            { state.ussAttributes.fileMode?.group = it?.mode ?: 0 }
          )
          .horizontalAlign(HorizontalAlign.FILL)
      }
      row {
        label("Permissions for all users: ")
          .widthGroup(sameWidthGroup)
        comboBox(fileModeValues)
          .bindItem(
            { state.ussAttributes.fileMode?.all?.toFileModeValue() },
            { state.ussAttributes.fileMode?.all = it?.mode ?: 0 }
          )
          .horizontalAlign(HorizontalAlign.FILL)
      }
    }

    tabbedPanel.add("General", generalTab)
    tabbedPanel.add("Permissions", permissionTab)

    return tabbedPanel
  }

  override fun doOKAction() {
    generalTab.apply()
    permissionTab.apply()
    super.doOKAction()
  }

}

class UssFileState(var ussAttributes: RemoteUssAttributes, val fileIsBeingEditingNow: Boolean)

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