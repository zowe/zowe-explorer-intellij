/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.files.ds.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import org.zowe.explorer.utils.UNKNOWN_PARAM_VALUE
import org.zowe.explorer.utils.getParamTextValueOrUnknown
import org.zowe.explorer.v3.dialogs.LazyDialog
import org.zowe.kotlinsdk.providers.zowe.zosmf.datasets.definitions.ZosmfMemberItem
import javax.swing.JComponent

/**
 * Lazy dialog displaying z/OS data set member properties in read-only mode.
 * Shows a single flat panel for load modules, or a tabbed pane (General / Data) for plain members.
 * @param project the current project
 * @param memberItem the z/OSMF member item containing the member attributes
 */
class MemberPropertiesDialog(
  private val project: Project?,
  memberItem: ZosmfMemberItem
) : LazyDialog<ZosmfMemberItem>(project) {

  override var state = memberItem

  private fun isLoadModule(member: ZosmfMemberItem): Boolean {
    return listOf(
      member.authorizationCode,
      member.aliasOf,
      member.amode,
      member.loadModuleAttributes,
      member.rmode,
      member.size,
      member.ttr,
      member.ssi
    ).any { it != null }
  }

  override fun produceDialog(): DialogWrapper {
    return object : DialogWrapper(project) {
      init {
        title = "Member Properties"
        init()
      }

      override fun createCenterPanel(): JComponent {
        val member = this@MemberPropertiesDialog.state
        return if (isLoadModule(member)) buildLoadModulePanel(member) else buildPlainMemberTabs(member)
      }
    }
  }

  private fun buildLoadModulePanel(member: ZosmfMemberItem): JComponent {
    val sameWidthGroup = "MEMBER_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"
    return panel {
      memberNameRow(member, sameWidthGroup)
      memberTypeRow("Load Module", sameWidthGroup)
      loadModuleRows(member, sameWidthGroup)
    }
  }

  private fun buildPlainMemberTabs(member: ZosmfMemberItem): JComponent {
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "MEMBER_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        memberNameRow(member, sameWidthGroup)
        memberTypeRow("Plain Member", sameWidthGroup)
        generalRows(member, sameWidthGroup)
      }
    )

    tabbedPanel.add(
      "Data",
      panel {
        dataRows(member, sameWidthGroup)
      }
    )

    return tabbedPanel
  }

  private fun Panel.memberNameRow(member: ZosmfMemberItem, widthGroup: String) {
    row {
      label("Member name: ")
        .widthGroup(widthGroup)
      textField()
        .text(member.memberName)
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
  }

  private fun Panel.memberTypeRow(type: String, widthGroup: String) {
    row {
      label("Member type: ")
        .widthGroup(widthGroup)
      textField()
        .text(type)
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
  }

  private fun Panel.generalRows(member: ZosmfMemberItem, widthGroup: String) {
    row {
      label("Version.Modification: ")
        .widthGroup(widthGroup)
      textField()
        .text(
          if (member.versionNumber != null && member.modificationLevel != null) {
            "${member.versionNumber}.${member.modificationLevel}"
          } else {
            UNKNOWN_PARAM_VALUE
          }
        )
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Creation date: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.creationDate))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Modification date: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.modificationDate))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Modification time: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.lastChangeTime))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Userid that Created/Modified: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.user))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
  }

  private fun Panel.dataRows(member: ZosmfMemberItem, widthGroup: String) {
    row {
      label("Current number of records: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.currentNumberOfRecords))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Beginning number of records: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.beginningNumberOfRecords))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Number of changed records: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.numberOfChangedRecords))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      val updatePlace = when (member.modifiedIn) {
        ZosmfMemberItem.ModifiedIn.SCLM -> "SCLM"
        ZosmfMemberItem.ModifiedIn.ISPF -> "ISPF"
        else -> UNKNOWN_PARAM_VALUE
      }
      label("Last update was made through $updatePlace")
        .widthGroup(widthGroup)
    }
  }

  private fun Panel.loadModuleRows(member: ZosmfMemberItem, widthGroup: String) {
    row {
      label("Authorization code: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.authorizationCode))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Current Member is alias of: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.aliasOf))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Load module attributes: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.loadModuleAttributes))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Member AMODE: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.amode))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Member RMODE: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.rmode))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Size: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.size))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("Member TTR: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.ttr))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
    row {
      label("SSI information: ")
        .widthGroup(widthGroup)
      textField()
        .text(getParamTextValueOrUnknown(member.ssi))
        .applyToComponent { isEditable = false }
        .align(AlignX.FILL)
    }
  }
}
