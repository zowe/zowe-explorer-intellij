/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl.teamconfig.dialogs

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.CommonBundle
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.IntelliJSpacingConfiguration
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.dialogs.LazyDialog
import org.zowe.explorer.v3.dialogs.TreeTableRenderer
import org.zowe.explorer.v3.dialogs.TreeTableRenderer.RowModel
import org.zowe.explorer.v3.dialogs.TreeTableRenderer.RowValue
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

/**
 * Dialog for creating a new Zowe Team Config file (`zowe.config.json` or
 * `zowe.config.user.json`).
 *
 * On confirmation, delegates to [CreateTeamConfigHandler] to generate the config file.
 *
 * @param project the current IntelliJ [Project]; used to resolve the project root
 *   for local configs and as the parent for dialogs.
 */
class CreateTeamConfigDialog(private val project: Project?) : LazyDialog<CreateTeamConfigDialogState>(project) {
  override var state = CreateTeamConfigDialogState()
  private val handler = CreateTeamConfigHandler(project?.basePath)

  private val screen = Toolkit.getDefaultToolkit().screenSize
  private val minHeight = (screen.height * .4).toInt()
  private val minWidth = (screen.width * .35).toInt()

  private fun profileRowModel(name: String, type: String, properties: Map<String, Any>): RowModel {
    return RowModel(
      title = "Profile",
      summaryProvider = { values ->
        val main = "Profile: ${values["Name"]}, type: ${values["Type"]}"
        val props = values.filterKeys { it != "Name" && it != "Type" }
        if (props.isEmpty()) main
        else main + ", " + props.entries.joinToString(", ") { (k, v) -> "$k: $v" }
      },
      entries = listOf(
        "Name" to RowValue.Text(name),
        "Type" to RowValue.Text(type),
        "Properties" to if (properties.isNotEmpty()) {
          RowValue.Nested(
            RowModel(
              title = "Properties",
              summaryProvider = { values -> "Properties: ${values.entries.joinToString(", ") { (k, v) -> "$k: $v" }}" },
              entries = properties.map { (k, v) -> k to RowValue.Text(v.toString()) },
              defaultExpanded = true
            )
          )
        } else {
          RowValue.Text("{}")
        }
      )
    )
  }

  private fun buildConfigTypeHeader(): JPanel {
    val slash = if (SystemInfo.isWindows) "\\" else "/"
    val globalBase = if (SystemInfo.isWindows) "C:\\Users\\<user>\\.zowe" else "/home/<user>/.zowe"
    val configDescriptions = ConfigType.entries.associateWith { configType ->
      "${if (configType.isGlobal) globalBase else "~"}$slash${configType.fileName}"
    }
    val hintLabel = JLabel(configDescriptions[state.configType] ?: "").apply {
      foreground = UIUtil.getContextHelpForeground()
    }
    val combo = JComboBox(DefaultComboBoxModel(ConfigType.entries.toTypedArray())).apply {
      renderer = SimpleListCellRenderer.create("") { it?.displayName ?: "" }
      selectedItem = state.configType
      addActionListener {
        val selected = selectedItem as? ConfigType ?: ConfigType.LOCAL_TEAM
        state.configType = selected
        hintLabel.text = configDescriptions[selected] ?: ""
      }
    }

    val configTypeRow = JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(4), 0)).apply {
      border = JBUI.Borders.empty(0, -JBUI.scale(4), 0, 0)
      add(JLabel("Config type:"))
      add(combo)
      add(hintLabel)
    }

    return JPanel(BorderLayout(0, 0)).apply {
      add(configTypeRow, BorderLayout.NORTH)
      add(JLabel("Zowe Team Config structure to be created:").apply {
        border = JBUI.Borders.empty(4, 0, 8, 0)
      }, BorderLayout.SOUTH)
    }
  }

  override fun produceDialog(): DialogWrapper {
    return object : DialogWrapper(project) {
      init {
        title = "Create Zowe Team Config"
        init()
      }

      lateinit var contentPanel: DialogPanel
      lateinit var scrollPane: JBScrollPane

      override fun doOKAction() {
        try {
          contentPanel.apply()
          val configFile = this@CreateTeamConfigDialog.handler.resolveConfigFile(this@CreateTeamConfigDialog.state)
          if (configFile.exists()) {
            val result = Messages.showOkCancelDialog(
              project,
              "Zowe Team Config already exists at:\n${configFile.absolutePath}\n\nDo you want to replace it?",
              "File Already Exists",
              "Replace",
              CommonBundle.getCancelButtonText(),
              Messages.getWarningIcon()
            )
            if (result != Messages.OK) return
          }
          this@CreateTeamConfigDialog.handler.generate(this@CreateTeamConfigDialog.state)
          super.doOKAction()
        } catch (e: Exception) {
          Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Create Config")
        }
      }

      private fun resetToDefaults() {
        this@CreateTeamConfigDialog.state = CreateTeamConfigDialogState()
        contentPanel = buildContentPanel()
        scrollPane.setViewportView(contentPanel)
      }

      override fun createLeftSideActions(): Array<javax.swing.Action> {
        return arrayOf(object : javax.swing.AbstractAction("Reset to defaults") {
          override fun actionPerformed(e: java.awt.event.ActionEvent) {
            resetToDefaults()
          }
        })
      }

      private fun buildContentPanel(): DialogPanel {
        val renderer = TreeTableRenderer(
          tableScale = JBUI.scale(24),
          gapsScale = JBUI.scale(16),
          guideColor = EditorColorsManager.getInstance().globalScheme
            .getColor(EditorColors.INDENT_GUIDE_COLOR) ?: JBColor.GRAY,
          stripeColor = UIUtil.getDecoratedRowColor()
        )

        return panel {
          customizeSpacingConfiguration(object : IntelliJSpacingConfiguration() {
            override val verticalComponentGap = 0
          }) {
            with(renderer) {
              this@CreateTeamConfigDialog.handler.profileEntries().forEachIndexed { idx, (name, type, properties) ->
                renderRowModel(this@CreateTeamConfigDialog.profileRowModel(name, type, properties), isFirst = idx == 0)
              }
            }
          }
        }
      }

      override fun createCenterPanel(): JComponent {
        contentPanel = buildContentPanel()
        scrollPane = JBScrollPane(contentPanel).apply {
          horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
          border = null
          minimumSize = Dimension(minWidth, minHeight)
        }

        return JPanel(BorderLayout(0, 0)).apply {
          add(this@CreateTeamConfigDialog.buildConfigTypeHeader(), BorderLayout.NORTH)
          add(scrollPane, BorderLayout.CENTER)
        }
      }
    }
  }

}