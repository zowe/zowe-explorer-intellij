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

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.toMutableProperty
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.zowe.explorer.v3.dialogs.LazyDialog
import org.zowe.explorer.v3.impl.teamconfig.profiles.ConfigProfile
import org.zowe.explorer.v3.impl.teamconfig.ConfigType
import org.zowe.explorer.v3.impl.teamconfig.profiles.ProfileField
import java.awt.Dimension
import java.awt.Toolkit
import java.io.File
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.reflect.KMutableProperty0

/**
 * Dialog for creating a new Zowe Team Config file (`zowe.config.json` or
 * `zowe.config.user.json`).
 *
 * Extends [LazyDialog] so the underlying [DialogWrapper] is instantiated only when
 * first displayed. The dialog presents a scrollable form that lets the user:
 * - choose the config [org.zowe.explorer.v3.impl.teamconfig.ConfigType] (local/global, team/user),
 * - fill in base profile fields (always present),
 * - optionally enable and configure z/OSMF, TSO/E, and SSH profiles,
 * - expand an "Additional Profiles" section to configure SYSVIEW, Endevor,
 *   JCLCheck, EBG, zFTP, CICS, Db2, and MQ profiles.
 *
 * On confirmation (doOKAction), the dialog:
 * 1. Applies all pending UI bindings to [state].
 * 2. Resolves the target file path via [resolveConfigFile].
 * 3. Serializes [state] to a pretty-printed JSON map via [buildConfigMap].
 * 4. Writes `zowe.config.json` (or the appropriate variant) and copies the
 *    bundled `zowe.schema.json` next to it.
 *
 * @param project the current IntelliJ [Project]; used to resolve the project root
 *   for local configs and as the parent for dialogs.
 */
class CreateTeamConfigDialog(private val project: Project?) : LazyDialog<CreateTeamConfigDialogState>(project) {
  override var state = CreateTeamConfigDialogState()

  override fun produceDialog(): DialogWrapper {
    val screen = Toolkit.getDefaultToolkit().screenSize
    val minHeight = (screen.height * .4).toInt()
    val minWidth = (screen.width * .35).toInt()

    return object : DialogWrapper(project) {
      init {
        title = "Create Zowe Team Config"
        init()
      }

      lateinit var contentPanel: DialogPanel

      private fun Row.descriptiveCheckBox(text: String): Cell<JBCheckBox> {
        val checkBox = this.checkBox(text)
        checkBox.component.addActionListener {
          checkBox.component.text = if (checkBox.component.isSelected) "${text}:" else text
        }
        return checkBox
      }

      private fun Cell<JBCheckBox>.bindSelected(prop: KMutableProperty0<Boolean>): Cell<JBCheckBox> {
        val currVal = prop.toMutableProperty()
        val text = component.text
        component.text = if (currVal.get()) "${text}:" else text
        return bindSelected(currVal)
      }

      private fun Row.rightGap(gapSize: Int = 20): Cell<JPanel> {
        return cell(JPanel().apply {
          isOpaque = false
          preferredSize = Dimension(JBUI.scale(gapSize), 0)
        })
      }

      private fun Panel.collapsibleGroupWithSeparator(
        checkBox: Cell<JCheckBox>?,
        collapsibleGroupContent: Panel.() -> Unit
      ): CollapsibleRow {
        val collapsibleGroup = this.collapsibleGroup("") {
          collapsibleGroupContent()
        }
        collapsibleGroup
          .topGap(TopGap.NONE)
          .bottomGap(BottomGap.NONE)
          .visibleIf(checkBox?.selected ?: ComponentPredicate.fromValue(true))
        val separator = this.separator()
        separator.visible(false)
        separator.topGap(TopGap.SMALL)
        collapsibleGroup.addExpandedListener { expanded ->
          separator.visible(expanded && checkBox?.component?.isSelected ?: true)
        }
        checkBox?.component?.addActionListener {
          collapsibleGroup.expanded = checkBox.component.isSelected
          separator.visible(collapsibleGroup.expanded && checkBox.component.isSelected)
        }
        collapsibleGroup.expanded = checkBox?.component?.isSelected ?: false
        return collapsibleGroup
      }

      private fun Panel.profileSection(profile: ConfigProfile, fields: Panel.() -> Unit) {
        lateinit var checkBox: Cell<JCheckBox>
        row {
          checkBox = descriptiveCheckBox(profile.profileName)
            .bindSelected(profile::shouldCreate)
        }
        collapsibleGroupWithSeparator(checkBox) {
          fields()
        }
      }

      private fun Panel.stringField(field: ProfileField<String?>) {
        row("${field.name}:") {
          textField()
            .bindText(
              getter = { field.value ?: "" },
              setter = { field.value = it.ifBlank { null } }
            )
            .align(AlignX.FILL)
            .resizableColumn()
            .applyToComponent { toolTipText = field.description }
          rightGap()
        }
      }

      private fun Panel.intField(field: ProfileField<Int?>) {
        row("${field.name}:") {
          textField()
            .bindText(
              getter = { field.value?.toString() ?: "" },
              setter = { field.value = it.toIntOrNull() }
            )
            .align(AlignX.FILL)
            .resizableColumn()
            .applyToComponent { toolTipText = field.description }
          rightGap()
        }
      }

      private fun Panel.charArrayPasswordField(field: ProfileField<CharArray?>) {
        row("${field.name}:") {
          passwordField()
            .applyToComponent {
              field.value?.let { text = String(it) }
              toolTipText = field.description
              document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) = sync()
                override fun removeUpdate(e: DocumentEvent) = sync()
                override fun changedUpdate(e: DocumentEvent) = sync()
                private fun sync() {
                  val chars = password
                  field.value = if (chars.isEmpty()) null else chars
                }
              })
            }
            .align(AlignX.FILL)
            .resizableColumn()
          rightGap()
        }
      }

      private fun Panel.checkBoxField(field: ProfileField<Boolean?>, default: Boolean) {
        row("${field.name}:") {
          checkBox("")
            .bindSelected(
              getter = { field.value ?: default },
              setter = {
                field.value = it
              }
            )
            .applyToComponent { toolTipText = field.description }
        }
      }

      private fun Panel.baseProfileFields() {
        stringField(state.baseProfile.host)
        intField(state.baseProfile.port)
        stringField(state.baseProfile.user)
        charArrayPasswordField(state.baseProfile.password)
        checkBoxField(state.baseProfile.rejectUnauthorized, true)
        stringField(state.baseProfile.tokenType)
        stringField(state.baseProfile.tokenValue)
        stringField(state.baseProfile.certFile)
        stringField(state.baseProfile.certKeyFile)
      }

      private fun Panel.zosmfProfileFields() {
        stringField(state.zosmfProfile.host)
        intField(state.zosmfProfile.port)
        stringField(state.zosmfProfile.user)
        charArrayPasswordField(state.zosmfProfile.password)
        checkBoxField(state.zosmfProfile.rejectUnauthorized, true)
        stringField(state.zosmfProfile.certFile)
        stringField(state.zosmfProfile.certKeyFile)
        stringField(state.zosmfProfile.basePath)
        stringField(state.zosmfProfile.protocol)
        stringField(state.zosmfProfile.encoding)
        intField(state.zosmfProfile.responseTimeout)
      }

      private fun Panel.tsoProfileFields() {
        stringField(state.tsoProfile.account)
        stringField(state.tsoProfile.characterSet)
        stringField(state.tsoProfile.codePage)
        intField(state.tsoProfile.columns)
        stringField(state.tsoProfile.logonProcedure)
        intField(state.tsoProfile.regionSize)
        intField(state.tsoProfile.rows)
      }

      private fun Panel.sshProfileFields() {
        stringField(state.sshProfile.host)
        intField(state.sshProfile.port)
        stringField(state.sshProfile.user)
        charArrayPasswordField(state.sshProfile.password)
        stringField(state.sshProfile.privateKey)
        charArrayPasswordField(state.sshProfile.keyPassphrase)
        intField(state.sshProfile.handshakeTimeout)
      }

      private fun Panel.sysviewProfileFields() {
        stringField(state.sysviewProfile.host)
        intField(state.sysviewProfile.port)
        stringField(state.sysviewProfile.user)
        charArrayPasswordField(state.sysviewProfile.password)
        checkBoxField(state.sysviewProfile.rejectUnauthorized, false)
        stringField(state.sysviewProfile.ssid)
        stringField(state.sysviewProfile.basePath)
      }

      private fun Panel.sysviewFormatProfileFields() {
        checkBoxField(state.sysviewFormatProfile.overview, false)
        checkBoxField(state.sysviewFormatProfile.info, false)
        checkBoxField(state.sysviewFormatProfile.pretty, false)
        checkBoxField(state.sysviewFormatProfile.blankIfZero, false)
        checkBoxField(state.sysviewFormatProfile.truncate, false)
      }

      private fun Panel.endevorProfileFields() {
        stringField(state.endevorProfile.host)
        intField(state.endevorProfile.port)
        stringField(state.endevorProfile.user)
        charArrayPasswordField(state.endevorProfile.password)
        stringField(state.endevorProfile.protocol)
        stringField(state.endevorProfile.basePath)
        checkBoxField(state.endevorProfile.rejectUnauthorized, false)
        stringField(state.endevorProfile.reportDir)
      }

      private fun Panel.endevorLocationProfileFields() {
        stringField(state.endevorLocationProfile.instance)
        stringField(state.endevorLocationProfile.environment)
        stringField(state.endevorLocationProfile.system)
        stringField(state.endevorLocationProfile.subsystem)
        stringField(state.endevorLocationProfile.type)
        stringField(state.endevorLocationProfile.stageNumber)
        stringField(state.endevorLocationProfile.comment)
        stringField(state.endevorLocationProfile.ccid)
        intField(state.endevorLocationProfile.maxrc)
        checkBoxField(state.endevorLocationProfile.overrideSignout, false)
        stringField(state.endevorLocationProfile.fileExtension)
      }

      private fun Panel.jclCheckProfileFields() {
        stringField(state.jclCheckProfile.host)
        intField(state.jclCheckProfile.port)
        stringField(state.jclCheckProfile.user)
        charArrayPasswordField(state.jclCheckProfile.password)
        stringField(state.jclCheckProfile.basePath)
        checkBoxField(state.jclCheckProfile.rejectUnauthorized, true)
        stringField(state.jclCheckProfile.protocol)
        stringField(state.jclCheckProfile.jclcheckOptions)
      }

      private fun Panel.ebgProfileFields() {
        stringField(state.ebgProfile.protocol)
        stringField(state.ebgProfile.host)
        intField(state.ebgProfile.port)
        stringField(state.ebgProfile.user)
        charArrayPasswordField(state.ebgProfile.token)
        checkBoxField(state.ebgProfile.rejectUnauthorized, false)
      }

      private fun Panel.zftpProfileFields() {
        stringField(state.zftpProfile.host)
        intField(state.zftpProfile.port)
        stringField(state.zftpProfile.user)
        charArrayPasswordField(state.zftpProfile.password)
        checkBoxField(state.zftpProfile.secureFtp, true)
        checkBoxField(state.zftpProfile.rejectUnauthorized, false)
        stringField(state.zftpProfile.servername)
        intField(state.zftpProfile.connectionTimeout)
        stringField(state.zftpProfile.encoding)
      }

      private fun Panel.cicsProfileFields() {
        stringField(state.cicsProfile.host)
        intField(state.cicsProfile.port)
        stringField(state.cicsProfile.user)
        charArrayPasswordField(state.cicsProfile.password)
        stringField(state.cicsProfile.regionName)
        stringField(state.cicsProfile.cicsPlex)
        checkBoxField(state.cicsProfile.rejectUnauthorized, true)
        stringField(state.cicsProfile.protocol)
      }

      private fun Panel.db2ProfileFields() {
        stringField(state.db2Profile.host)
        intField(state.db2Profile.port)
        stringField(state.db2Profile.user)
        charArrayPasswordField(state.db2Profile.password)
        stringField(state.db2Profile.database)
        stringField(state.db2Profile.sslFile)
      }

      private fun Panel.mqProfileFields() {
        stringField(state.mqProfile.host)
        intField(state.mqProfile.port)
        stringField(state.mqProfile.user)
        charArrayPasswordField(state.mqProfile.password)
        checkBoxField(state.mqProfile.rejectUnauthorized, false)
        stringField(state.mqProfile.protocol)
      }

      override fun doOKAction() {
        try {
          contentPanel.apply()
          this@CreateTeamConfigDialog.generateConfigFile()
          super.doOKAction()
        } catch (e: Exception) {
          Messages.showErrorDialog(project, e.message ?: "Unknown error", "Failed to Create Config")
        }
      }

      override fun createCenterPanel(): JComponent {
        contentPanel = panel {
          row {
            label("Config type:")
            val slash = if (SystemInfo.isWindows) "\\" else "/"
            val globalBase = if (SystemInfo.isWindows) "C:\\Users\\<user>\\.zowe" else "/home/<user>/.zowe"
            val configDescriptions = ConfigType.entries.associateWith { configType ->
              "${if (configType.isGlobal) globalBase else "~"}$slash${configType.fileName}"
            }
            val combo = comboBox(ConfigType.entries.toList())
              .applyToComponent {
                renderer = SimpleListCellRenderer.create("") { it?.displayName ?: "" }
                selectedItem = state.configType
              }
            val hint = label(configDescriptions[state.configType] ?: "")
            hint.applyToComponent { foreground = UIUtil.getContextHelpForeground() }
            combo.component.addActionListener {
              val selected = combo.component.selectedItem as? ConfigType ?: ConfigType.LOCAL_TEAM
              state.configType = selected
              hint.component.text = configDescriptions[selected] ?: ""
            }
          }
          row {
            label("${state.baseProfile.profileName}:")
          }
          collapsibleGroupWithSeparator(null) {
            baseProfileFields()
          }.also { it.expanded = true }

          profileSection(state.zosmfProfile) { zosmfProfileFields() }
          profileSection(state.tsoProfile) { tsoProfileFields() }
          profileSection(state.sshProfile) { sshProfileFields() }

          val additionalProfilesCollapsibleGroup = collapsibleGroup("Additional Profiles") {
            profileSection(state.sysviewProfile) { sysviewProfileFields() }
            profileSection(state.sysviewFormatProfile) { sysviewFormatProfileFields() }
            profileSection(state.endevorProfile) { endevorProfileFields() }
            profileSection(state.endevorLocationProfile) { endevorLocationProfileFields() }
            profileSection(state.jclCheckProfile) { jclCheckProfileFields() }
            profileSection(state.ebgProfile) { ebgProfileFields() }
            profileSection(state.zftpProfile) { zftpProfileFields() }
            profileSection(state.cicsProfile) { cicsProfileFields() }
            profileSection(state.db2Profile) { db2ProfileFields() }
            profileSection(state.mqProfile) { mqProfileFields() }
          }
          additionalProfilesCollapsibleGroup.topGap(TopGap.NONE)
        }

        return object : JBScrollPane(contentPanel) {}
          .apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            border = null
            minimumSize = Dimension(minWidth, minHeight)
          }
      }
    }
  }

  private fun resolveConfigFile(): File {
    val configType = state.configType
    return if (!configType.isGlobal) {
      File(project?.basePath ?: ".", configType.fileName)
    } else {
      File(System.getProperty("user.home")).resolve(".zowe").resolve(configType.fileName)
    }
  }

  private fun generateConfigFile() {
    val file = resolveConfigFile()
    file.parentFile?.mkdirs()
    val gson = GsonBuilder().setPrettyPrinting().create()
    file.writeText(gson.toJson(buildConfigMap()))

    val schemaSource = javaClass.getResourceAsStream("/files/zowe.schema.json")
    schemaSource?.use { input ->
      file.resolveSibling("zowe.schema.json").outputStream().use { output ->
        input.copyTo(output)
      }
    }
  }

  private fun buildConfigMap(): Map<String, Any> {
    val profiles = linkedMapOf<String, Any>()
    val defaults = linkedMapOf<String, String>()

    listOf(
      state.baseProfile,
      state.zosmfProfile,
      state.tsoProfile,
      state.sshProfile,
      state.sysviewProfile,
      state.sysviewFormatProfile,
      state.endevorProfile,
      state.endevorLocationProfile,
      state.jclCheckProfile,
      state.ebgProfile,
      state.zftpProfile,
      state.cicsProfile,
      state.db2Profile,
      state.mqProfile
    )
      .filter { it.shouldCreate }
      .forEach { profile ->
        profiles[profile.profileType] = profile.jsonFriendly()
        defaults[profile.profileType] = profile.profileType
      }

    return linkedMapOf(
      "\$schema" to "./zowe.schema.json",
      "profiles" to profiles,
      "defaults" to defaults,
      "autoStore" to true
    )
  }
}