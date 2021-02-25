package eu.ibagroup.formainframe.explorer.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import eu.ibagroup.formainframe.common.ui.DialogMode
import eu.ibagroup.formainframe.common.ui.DialogState
import eu.ibagroup.formainframe.common.ui.StatefulComponent
import eu.ibagroup.formainframe.dataops.attributes.RemoteMemberAttributes
import javax.swing.JComponent

class MemberPropertiesDialog(var project: Project?, override var state: MemberState) : DialogWrapper(project),
  StatefulComponent<MemberState> {

  init {
    title = "Member Properties"
    init()
  }

  override fun createCenterPanel(): JComponent {
    val member = state.memberAttributes.memberInfo
    val tabbedPanel = JBTabbedPane()

    tabbedPanel.add("General", panel {
      row {
        label("Member name: ")
        JBTextField(member.name).apply { isEditable = false }()
      }
      row {
        label("Version.Modification: ")
        if (member.versionNumber != null && member.modificationLevel != null)
          JBTextField("${member.versionNumber}.${member.modificationLevel}").apply { isEditable = false }()
        else
          JBTextField("").apply { isEditable = false }()
      }
      row {
        label("Create Date: ")
        JBTextField(member.creationDate ?: "").apply { isEditable = false }()
      }
      row {
        label("Modification Date: ")
        JBTextField(member.modificationDate ?: "").apply { isEditable = false }()
      }
      row {
        label("Modification Time: ")
        JBTextField(member.lastChangeTime ?: "").apply { isEditable = false }()
      }
      row {
        label("Userid that Created/Modified: ")
        JBTextField(member.user ?: "").apply { isEditable = false }()
      }
    })

    tabbedPanel.add("Data", panel {
      row {
        label("Current number of records: ")
        JBTextField(member.currentNumberOfRecords?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Beginning number of records: ")
        JBTextField(member.beginningNumberOfRecords?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        label("Number of changed records: ")
        JBTextField(member.numberOfChangedRecords?.toString() ?: "").apply { isEditable = false }()
      }
      row {
        if ("Y".equals(member.sclm)) {
          label("Last update was made through SCLM")
        } else {
          label("Last update was made through ISPF")
        }
      }
    })

    tabbedPanel.add("Extended", panel {
      row {
        label("<html><b>Load Module Properties</b><br>(empty if member is not a load module)</html>")
      }
      row {
        label("Authorization code: ")
        JBTextField(member.authorizationCode ?: "").apply { isEditable = false }()
      }
      row {
        label("Current Member is alias of: ")
        JBTextField(member.aliasOf ?: "").apply { isEditable = false }()
      }
      row {
        label("Load module attributes: ")
        JBTextField(member.loadModuleAttributes ?: "").apply { isEditable = false }()
      }
      row {
        label("Member AMODE: ")
        JBTextField(member.amode ?: "").apply { isEditable = false }()
      }
      row {
        label("Member RMODE: ")
        JBTextField(member.rmode ?: "").apply { isEditable = false }()
      }
      row {
        label("Size: ")
        JBTextField(member.size ?: "").apply { isEditable = false }()
      }
      row {
        label("Member TTR: ")
        JBTextField(member.ttr ?: "").apply { isEditable = false }()
      }
      row {
        label("SSI information for a load module: ")
        JBTextField(member.ssi ?: "").apply { isEditable = false }()
      }
    })

    return tabbedPanel
  }
}


class MemberState(var memberAttributes: RemoteMemberAttributes, override var mode: DialogMode = DialogMode.READ) :
  DialogState