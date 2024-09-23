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
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import eu.ibagroup.formainframe.utils.UNKNOWN_PARAM_VALUE
import eu.ibagroup.formainframe.utils.getParamTextValueOrUnknown
import javax.swing.JComponent

class MemberPropertiesDialog(var project: Project?, override var state: MemberState) : DialogWrapper(project),
  StatefulComponent<MemberState> {

  init {
    title = "Member Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val member = state.memberAttributes.info
    val tabbedPanel = JBTabbedPane()
    val sameWidthGroup = "MEMBER_PROPERTIES_DIALOG_LABELS_WIDTH_GROUP"

    tabbedPanel.add(
      "General",
      panel {
        row {
          label("Member name: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(member.name)
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Version.Modification: ")
            .widthGroup(sameWidthGroup)
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
          label("Create Date: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.creationDate))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Modification Date: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.modificationDate))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Modification Time: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.lastChangeTime))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Userid that Created/Modified: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.user))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      }
    )

    tabbedPanel.add(
      "Data",
      panel {
        row {
          label("Current number of records: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.currentNumberOfRecords))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Beginning number of records: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.beginningNumberOfRecords))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Number of changed records: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.numberOfChangedRecords))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          val updatePlace = if ("Y" == member.sclm) {
            "SCLM"
          } else {
            "ISPF"
          }
          label("Last update was made through $updatePlace")
            .widthGroup(sameWidthGroup)
        }
      }
    )

    tabbedPanel.add(
      "Extended",
      panel {
        row {
          label("<html><b>Load Module Properties</b><br>(empty if member is not a load module)</html>")
        }
        row {
          label("Authorization code: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.authorizationCode))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Current Member is alias of: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.aliasOf))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Load module attributes: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.loadModuleAttributes))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Member AMODE: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.amode))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Member RMODE: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.rmode))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Size: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.size))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("Member TTR: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.ttr))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
        row {
          label("SSI information for a load module: ")
            .widthGroup(sameWidthGroup)
          textField()
            .text(getParamTextValueOrUnknown(member.ssi))
            .applyToComponent { isEditable = false }
            .align(AlignX.FILL)
        }
      }
    )

    return tabbedPanel
  }
}


class MemberState(var memberAttributes: RemoteMemberAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState
