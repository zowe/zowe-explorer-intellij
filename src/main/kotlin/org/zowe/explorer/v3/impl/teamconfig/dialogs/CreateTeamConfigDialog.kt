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
import org.zowe.explorer.v3.impl.teamconfig.ZoweConfigService
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
  override var state = CreateTeamConfigDialogState().apply {
    configType = ZoweConfigService.getService().getSelectedConfigType(project)
  }
  private val handler = CreateTeamConfigHandler(ZoweConfigService.getService(), project?.basePath)

  private val screen = Toolkit.getDefaultToolkit().screenSize
  private val minHeight = (screen.height * .4).toInt()
  private val minWidth = (screen.width * .35).toInt()

  private fun profileRowModel(entry: CreateTeamConfigHandler.ProfileEntry): RowModel {
    val entries = mutableListOf<Pair<String, RowValue>>(
      "Name" to RowValue.Text(entry.name),
      "Type" to RowValue.Text(entry.type)
    )
    if (entry.properties.isNotEmpty()) {
      entries += "Properties" to RowValue.Nested(
        RowModel(
          title = "Properties",
          summaryProvider = { values -> "Properties: ${values.entries.joinToString(", ") { (k, v) -> "$k: $v" }}" },
          entries = entry.properties.map { (k, v) -> k to RowValue.Text(v.toString()) },
          defaultExpanded = true
        )
      )
    }
    if (entry.children.isNotEmpty()) {
      entries += "Profiles" to RowValue.Nested(
        RowModel(
          title = "Profiles",
          summaryProvider = { _ -> "Profiles: ${entry.children.joinToString(", ") { it.name }}" },
          entries = entry.children.map { child -> child.name to RowValue.Nested(profileRowModel(child)) },
          defaultExpanded = true
        )
      )
    }
    return RowModel(
      title = "Profile",
      summaryProvider = { _ ->
        val main = "Profile: ${entry.name}, type: ${entry.type}"
        if (entry.children.isNotEmpty()) "$main, profiles: ${entry.children.joinToString(", ") { it.name }}"
        else main
      },
      entries = entries
    )
  }

  private fun buildConfigTypeHeader(): JPanel {
    val configService = ZoweConfigService.getService()
    val hintLabel = JLabel(configService.configPathDescription(state.configType)).apply {
      foreground = UIUtil.getContextHelpForeground()
    }
    val availableTypes = ConfigType.availableEntries(project != null)
    val combo = JComboBox(DefaultComboBoxModel(availableTypes.toTypedArray())).apply {
      renderer = SimpleListCellRenderer.create("") { it?.displayName ?: "" }
      selectedItem = state.configType
      addActionListener {
        val selected = selectedItem as? ConfigType ?: ConfigType.LOCAL_TEAM
        state.configType = selected
        hintLabel.text = configService.configPathDescription(selected)
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
              this@CreateTeamConfigDialog.handler.profileEntries().forEachIndexed { idx, entry ->
                renderRowModel(this@CreateTeamConfigDialog.profileRowModel(entry), isFirst = idx == 0)
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