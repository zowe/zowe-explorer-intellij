/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.actions.teamconfig

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
import java.awt.Dimension
import java.awt.Toolkit
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
 * - choose the config [ConfigType] (local/global, team/user),
 * - fill in base profile fields (always present),
 * - optionally enable and configure z/OSMF, TSO/E, and SSH profiles,
 * - expand an "Additional Profiles" section to configure SYSVIEW, Endevor,
 *   JCLCheck, EBG, zFTP, CICS, Db2, and MQ profiles.
 *
 * On confirmation ([doOKAction]), the dialog:
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

      private fun Panel.stringField(label: String, field: ProfileField<String?>) {
        row(label) {
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

      private fun Panel.intField(label: String, field: ProfileField<Int?>) {
        row(label) {
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

      private fun Panel.charArrayPasswordField(label: String, field: ProfileField<CharArray?>) {
        row(label) {
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

      private fun Panel.checkBoxField(label: String, field: ProfileField<Boolean?>, default: Boolean) {
        row(label) {
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
        stringField("Host:", state.baseProfile.host)
        intField("Port:", state.baseProfile.port)
        stringField("User:", state.baseProfile.user)
        charArrayPasswordField("Password:", state.baseProfile.password)
        checkBoxField("Reject unauthorized:", state.baseProfile.rejectUnauthorized, true)
        stringField("Token type:", state.baseProfile.tokenType)
        stringField("Token value:", state.baseProfile.tokenValue)
        stringField("Cert file:", state.baseProfile.certFile)
        stringField("Cert key file:", state.baseProfile.certKeyFile)
      }

      private fun Panel.zosmfProfileFields() {
        stringField("Host:", state.zosmfProfile.host)
        intField("Port:", state.zosmfProfile.port)
        stringField("User:", state.zosmfProfile.user)
        charArrayPasswordField("Password:", state.zosmfProfile.password)
        checkBoxField("Reject unauthorized:", state.zosmfProfile.rejectUnauthorized, true)
        stringField("Cert file:", state.zosmfProfile.certFile)
        stringField("Cert key file:", state.zosmfProfile.certKeyFile)
        stringField("Base path:", state.zosmfProfile.basePath)
        stringField("Protocol:", state.zosmfProfile.protocol)
        stringField("Encoding:", state.zosmfProfile.encoding)
        intField("Response timeout:", state.zosmfProfile.responseTimeout)
      }

      private fun Panel.tsoProfileFields() {
        stringField("Account:", state.tsoProfile.account)
        stringField("Character set:", state.tsoProfile.characterSet)
        stringField("Code page:", state.tsoProfile.codePage)
        intField("Columns:", state.tsoProfile.columns)
        stringField("Logon procedure:", state.tsoProfile.logonProcedure)
        intField("Region size:", state.tsoProfile.regionSize)
        intField("Rows:", state.tsoProfile.rows)
      }

      private fun Panel.sshProfileFields() {
        stringField("Host:", state.sshProfile.host)
        intField("Port:", state.sshProfile.port)
        stringField("User:", state.sshProfile.user)
        charArrayPasswordField("Password:", state.sshProfile.password)
        stringField("Private key:", state.sshProfile.privateKey)
        charArrayPasswordField("Key passphrase:", state.sshProfile.keyPassphrase)
        intField("Handshake timeout:", state.sshProfile.handshakeTimeout)
      }

      private fun Panel.sysviewProfileFields() {
        stringField("Host:", state.sysviewProfile.host)
        intField("Port:", state.sysviewProfile.port)
        stringField("User:", state.sysviewProfile.user)
        charArrayPasswordField("Password:", state.sysviewProfile.password)
        checkBoxField("Reject unauthorized:", state.sysviewProfile.rejectUnauthorized, false)
        stringField("SSID:", state.sysviewProfile.ssid)
        stringField("Base path:", state.sysviewProfile.basePath)
      }

      private fun Panel.sysviewFormatProfileFields() {
        checkBoxField("Overview:", state.sysviewFormatProfile.overview, false)
        checkBoxField("Info:", state.sysviewFormatProfile.info, false)
        checkBoxField("Pretty:", state.sysviewFormatProfile.pretty, false)
        checkBoxField("Blank if zero:", state.sysviewFormatProfile.blankIfZero, false)
        checkBoxField("Truncate:", state.sysviewFormatProfile.truncate, false)
      }

      private fun Panel.endevorProfileFields() {
        stringField("Host:", state.endevorProfile.host)
        intField("Port:", state.endevorProfile.port)
        stringField("User:", state.endevorProfile.user)
        charArrayPasswordField("Password:", state.endevorProfile.password)
        stringField("Protocol:", state.endevorProfile.protocol)
        stringField("Base path:", state.endevorProfile.basePath)
        checkBoxField("Reject unauthorized:", state.endevorProfile.rejectUnauthorized, false)
        stringField("Report dir:", state.endevorProfile.reportDir)
      }

      private fun Panel.endevorLocationProfileFields() {
        stringField("Instance:", state.endevorLocationProfile.instance)
        stringField("Environment:", state.endevorLocationProfile.environment)
        stringField("System:", state.endevorLocationProfile.system)
        stringField("Subsystem:", state.endevorLocationProfile.subsystem)
        stringField("Type:", state.endevorLocationProfile.type)
        stringField("Stage number:", state.endevorLocationProfile.stageNumber)
        stringField("Comment:", state.endevorLocationProfile.comment)
        stringField("CCID:", state.endevorLocationProfile.ccid)
        intField("Max RC:", state.endevorLocationProfile.maxrc)
        checkBoxField("Override signout:", state.endevorLocationProfile.overrideSignout, false)
        stringField("File extension:", state.endevorLocationProfile.fileExtension)
      }

      private fun Panel.jclCheckProfileFields() {
        stringField("Host:", state.jclCheckProfile.host)
        intField("Port:", state.jclCheckProfile.port)
        stringField("User:", state.jclCheckProfile.user)
        charArrayPasswordField("Password:", state.jclCheckProfile.password)
        stringField("Base path:", state.jclCheckProfile.basePath)
        checkBoxField("Reject unauthorized:", state.jclCheckProfile.rejectUnauthorized, true)
        stringField("Protocol:", state.jclCheckProfile.protocol)
        stringField("JCLCheck options:", state.jclCheckProfile.jclcheckOptions)
      }

      private fun Panel.ebgProfileFields() {
        stringField("Protocol:", state.ebgProfile.protocol)
        stringField("Host:", state.ebgProfile.host)
        intField("Port:", state.ebgProfile.port)
        stringField("User:", state.ebgProfile.user)
        charArrayPasswordField("Token:", state.ebgProfile.token)
        checkBoxField("Reject unauthorized:", state.ebgProfile.rejectUnauthorized, false)
      }

      private fun Panel.zftpProfileFields() {
        stringField("Host:", state.zftpProfile.host)
        intField("Port:", state.zftpProfile.port)
        stringField("User:", state.zftpProfile.user)
        charArrayPasswordField("Password:", state.zftpProfile.password)
        checkBoxField("Secure FTP:", state.zftpProfile.secureFtp, true)
        checkBoxField("Reject unauthorized:", state.zftpProfile.rejectUnauthorized, false)
        stringField("Server name:", state.zftpProfile.servername)
        intField("Connection timeout:", state.zftpProfile.connectionTimeout)
        stringField("Encoding:", state.zftpProfile.encoding)
      }

      private fun Panel.cicsProfileFields() {
        stringField("Host:", state.cicsProfile.host)
        intField("Port:", state.cicsProfile.port)
        stringField("User:", state.cicsProfile.user)
        charArrayPasswordField("Password:", state.cicsProfile.password)
        stringField("Region name:", state.cicsProfile.regionName)
        stringField("CICSPlex:", state.cicsProfile.cicsPlex)
        checkBoxField("Reject unauthorized:", state.cicsProfile.rejectUnauthorized, true)
        stringField("Protocol:", state.cicsProfile.protocol)
      }

      private fun Panel.db2ProfileFields() {
        stringField("Host:", state.db2Profile.host)
        intField("Port:", state.db2Profile.port)
        stringField("User:", state.db2Profile.user)
        charArrayPasswordField("Password:", state.db2Profile.password)
        stringField("Database:", state.db2Profile.database)
        stringField("SSL file:", state.db2Profile.sslFile)
      }

      private fun Panel.mqProfileFields() {
        stringField("Host:", state.mqProfile.host)
        intField("Port:", state.mqProfile.port)
        stringField("User:", state.mqProfile.user)
        charArrayPasswordField("Password:", state.mqProfile.password)
        checkBoxField("Reject unauthorized:", state.mqProfile.rejectUnauthorized, false)
        stringField("Protocol:", state.mqProfile.protocol)
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
            label("Base profile:")
          }
          collapsibleGroupWithSeparator(null) {
            baseProfileFields()
          }.also { it.expanded = true }

          lateinit var zosmfCheckBox: Cell<JCheckBox>
          row {
            zosmfCheckBox = descriptiveCheckBox("z/OSMF profile")
              .bindSelected(state.zosmfProfile::shouldCreate)
          }
          collapsibleGroupWithSeparator(zosmfCheckBox) {
            zosmfProfileFields()
          }

          lateinit var tsoCheckBox: Cell<JCheckBox>
          row {
            tsoCheckBox = descriptiveCheckBox("TSO/E profile")
              .bindSelected(state.tsoProfile::shouldCreate)
          }
          collapsibleGroupWithSeparator(tsoCheckBox) {
            tsoProfileFields()
          }

          lateinit var sshCheckBox: Cell<JCheckBox>
          row {
            sshCheckBox = descriptiveCheckBox("SSH profile")
              .bindSelected(state.sshProfile::shouldCreate)
          }
          collapsibleGroupWithSeparator(sshCheckBox) {
            sshProfileFields()
          }

          val additionalProfilesCollapsibleGroup = collapsibleGroup("Additional Profiles") {
            lateinit var sysviewCheckBox: Cell<JCheckBox>
            row {
              sysviewCheckBox = descriptiveCheckBox("SYSVIEW® profile")
                .bindSelected(state.sysviewProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(sysviewCheckBox) {
              sysviewProfileFields()
            }

            lateinit var sysviewFormatCheckBox: Cell<JCheckBox>
            row {
              sysviewFormatCheckBox = descriptiveCheckBox("SYSVIEW format profile")
                .bindSelected(state.sysviewFormatProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(sysviewFormatCheckBox) {
              sysviewFormatProfileFields()
            }

            lateinit var endevorCheckBox: Cell<JCheckBox>
            row {
              endevorCheckBox = descriptiveCheckBox("Endevor® profile")
                .bindSelected(state.endevorProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(endevorCheckBox) {
              endevorProfileFields()
            }

            lateinit var endevorLocationCheckBox: Cell<JCheckBox>
            row {
              endevorLocationCheckBox = descriptiveCheckBox("Endevor location profile")
                .bindSelected(state.endevorLocationProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(endevorLocationCheckBox) {
              endevorLocationProfileFields()
            }

            lateinit var jclCheckCheckBox: Cell<JCheckBox>
            row {
              jclCheckCheckBox = descriptiveCheckBox("JCLCheck™ profile")
                .bindSelected(state.jclCheckProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(jclCheckCheckBox) {
              jclCheckProfileFields()
            }

            lateinit var endevorBridgeCheckBox: Cell<JCheckBox>
            row {
              endevorBridgeCheckBox = descriptiveCheckBox("Endevor Bridge for Git profile")
                .bindSelected(state.ebgProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(endevorBridgeCheckBox) {
              ebgProfileFields()
            }

            lateinit var zftpCheckBox: Cell<JCheckBox>
            row {
              zftpCheckBox = descriptiveCheckBox("zFTP profile")
                .bindSelected(state.zftpProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(zftpCheckBox) {
              zftpProfileFields()
            }

            lateinit var ibmCicsCheckBox: Cell<JCheckBox>
            row {
              ibmCicsCheckBox = descriptiveCheckBox("IBM CICS profile")
                .bindSelected(state.cicsProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(ibmCicsCheckBox) {
              cicsProfileFields()
            }

            lateinit var ibmDb2CheckBox: Cell<JCheckBox>
            row {
              ibmDb2CheckBox = descriptiveCheckBox("IBM Db2 profile")
                .bindSelected(state.db2Profile::shouldCreate)
            }
            collapsibleGroupWithSeparator(ibmDb2CheckBox) {
              db2ProfileFields()
            }

            lateinit var ibmMqCheckBox: Cell<JCheckBox>
            row {
              ibmMqCheckBox = descriptiveCheckBox("IBM MQ profile")
                .bindSelected(state.mqProfile::shouldCreate)
            }
            collapsibleGroupWithSeparator(ibmMqCheckBox) {
              mqProfileFields()
            }
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

  private fun resolveConfigFile(): java.io.File {
    val configType = state.configType
    return if (!configType.isGlobal) {
      java.io.File(project?.basePath ?: ".", configType.fileName)
    } else {
      java.io.File(System.getProperty("user.home")).resolve(".zowe").resolve(configType.fileName)
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

    fun props(vararg pairs: Pair<String, Any?>): Map<String, Any> =
      linkedMapOf(*pairs.mapNotNull { (k, v) -> v?.let { k to it } }.toTypedArray())

    fun entry(type: String, properties: Map<String, Any>, secure: List<String>): Map<String, Any> =
      linkedMapOf("type" to type, "properties" to properties)
        .also { if (secure.isNotEmpty()) it["secure"] = secure }

    // Base — always included
    profiles["base"] = entry("base", props(
      "host" to state.baseProfile.host.value,
      "port" to state.baseProfile.port.value,
      "user" to state.baseProfile.user.value,
      "rejectUnauthorized" to state.baseProfile.rejectUnauthorized.value,
      "tokenType" to state.baseProfile.tokenType.value,
      "tokenValue" to state.baseProfile.tokenValue.value,
      "certFile" to state.baseProfile.certFile.value,
      "certKeyFile" to state.baseProfile.certKeyFile.value
    ), buildList {
      if (state.baseProfile.password.value?.isNotEmpty() == true) add("password")
    })
    defaults["base"] = "base"

    if (state.zosmfProfile.shouldCreate) {
      profiles["zosmf"] = entry("zosmf", props(
        "host" to state.zosmfProfile.host.value,
        "port" to state.zosmfProfile.port.value,
        "user" to state.zosmfProfile.user.value,
        "rejectUnauthorized" to state.zosmfProfile.rejectUnauthorized.value,
        "certFile" to state.zosmfProfile.certFile.value,
        "certKeyFile" to state.zosmfProfile.certKeyFile.value,
        "basePath" to state.zosmfProfile.basePath.value,
        "protocol" to state.zosmfProfile.protocol.value,
        "encoding" to state.zosmfProfile.encoding.value,
        "responseTimeout" to state.zosmfProfile.responseTimeout.value
      ), buildList {
        if (state.zosmfProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["zosmf"] = "zosmf"
    }

    if (state.tsoProfile.shouldCreate) {
      profiles["tso"] = entry("tso", props(
        "account" to state.tsoProfile.account.value,
        "characterSet" to state.tsoProfile.characterSet.value,
        "codePage" to state.tsoProfile.codePage.value,
        "columns" to state.tsoProfile.columns.value,
        "logonProcedure" to state.tsoProfile.logonProcedure.value,
        "regionSize" to state.tsoProfile.regionSize.value,
        "rows" to state.tsoProfile.rows.value
      ), emptyList())
      defaults["tso"] = "tso"
    }

    if (state.sshProfile.shouldCreate) {
      profiles["ssh"] = entry("ssh", props(
        "host" to state.sshProfile.host.value,
        "port" to state.sshProfile.port.value,
        "user" to state.sshProfile.user.value,
        "privateKey" to state.sshProfile.privateKey.value,
        "handshakeTimeout" to state.sshProfile.handshakeTimeout.value
      ), buildList {
        if (state.sshProfile.password.value?.isNotEmpty() == true) add("password")
        if (state.sshProfile.keyPassphrase.value?.isNotEmpty() == true) add("keyPassphrase")
      })
      defaults["ssh"] = "ssh"
    }

    if (state.sysviewProfile.shouldCreate) {
      profiles["sysview"] = entry("sysview", props(
        "host" to state.sysviewProfile.host.value,
        "port" to state.sysviewProfile.port.value,
        "user" to state.sysviewProfile.user.value,
        "rejectUnauthorized" to state.sysviewProfile.rejectUnauthorized.value,
        "ssid" to state.sysviewProfile.ssid.value,
        "basePath" to state.sysviewProfile.basePath.value
      ), buildList {
        if (state.sysviewProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["sysview"] = "sysview"
    }

    if (state.sysviewFormatProfile.shouldCreate) {
      profiles["sysview-format"] = entry("sysview-format", props(
        "contextFields" to state.sysviewFormatProfile.contextFields.value,
        "overview" to state.sysviewFormatProfile.overview.value,
        "info" to state.sysviewFormatProfile.info.value,
        "pretty" to state.sysviewFormatProfile.pretty.value,
        "blankIfZero" to state.sysviewFormatProfile.blankIfZero.value,
        "truncate" to state.sysviewFormatProfile.truncate.value
      ), emptyList())
      defaults["sysview-format"] = "sysview-format"
    }

    if (state.endevorProfile.shouldCreate) {
      profiles["endevor"] = entry("endevor", props(
        "host" to state.endevorProfile.host.value,
        "port" to state.endevorProfile.port.value,
        "user" to state.endevorProfile.user.value,
        "protocol" to state.endevorProfile.protocol.value,
        "basePath" to state.endevorProfile.basePath.value,
        "rejectUnauthorized" to state.endevorProfile.rejectUnauthorized.value,
        "reportDir" to state.endevorProfile.reportDir.value
      ), buildList {
        if (state.endevorProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["endevor"] = "endevor"
    }

    if (state.endevorLocationProfile.shouldCreate) {
      profiles["endevor-location"] = entry("endevor-location", props(
        "instance" to state.endevorLocationProfile.instance.value,
        "environment" to state.endevorLocationProfile.environment.value,
        "system" to state.endevorLocationProfile.system.value,
        "subsystem" to state.endevorLocationProfile.subsystem.value,
        "type" to state.endevorLocationProfile.type.value,
        "stageNumber" to state.endevorLocationProfile.stageNumber.value,
        "comment" to state.endevorLocationProfile.comment.value,
        "ccid" to state.endevorLocationProfile.ccid.value,
        "maxrc" to state.endevorLocationProfile.maxrc.value,
        "override-signout" to state.endevorLocationProfile.overrideSignout.value,
        "file-extension" to state.endevorLocationProfile.fileExtension.value
      ), emptyList())
      defaults["endevor-location"] = "endevor-location"
    }

    if (state.jclCheckProfile.shouldCreate) {
      profiles["jclcheck"] = entry("jclcheck", props(
        "host" to state.jclCheckProfile.host.value,
        "port" to state.jclCheckProfile.port.value,
        "user" to state.jclCheckProfile.user.value,
        "basePath" to state.jclCheckProfile.basePath.value,
        "rejectUnauthorized" to state.jclCheckProfile.rejectUnauthorized.value,
        "protocol" to state.jclCheckProfile.protocol.value,
        "jclcheckOptions" to state.jclCheckProfile.jclcheckOptions.value
      ), buildList {
        if (state.jclCheckProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["jclcheck"] = "jclcheck"
    }

    if (state.ebgProfile.shouldCreate) {
      profiles["ebg"] = entry("ebg", props(
        "protocol" to state.ebgProfile.protocol.value,
        "host" to state.ebgProfile.host.value,
        "port" to state.ebgProfile.port.value,
        "user" to state.ebgProfile.user.value,
        "rejectUnauthorized" to state.ebgProfile.rejectUnauthorized.value
      ), buildList {
        if (state.ebgProfile.token.value?.isNotEmpty() == true) add("token")
      })
      defaults["ebg"] = "ebg"
    }

    if (state.zftpProfile.shouldCreate) {
      profiles["zftp"] = entry("zftp", props(
        "host" to state.zftpProfile.host.value,
        "port" to state.zftpProfile.port.value,
        "user" to state.zftpProfile.user.value,
        "secureFtp" to state.zftpProfile.secureFtp.value,
        "rejectUnauthorized" to state.zftpProfile.rejectUnauthorized.value,
        "servername" to state.zftpProfile.servername.value,
        "connectionTimeout" to state.zftpProfile.connectionTimeout.value,
        "encoding" to state.zftpProfile.encoding.value
      ), buildList {
        if (state.zftpProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["zftp"] = "zftp"
    }

    if (state.cicsProfile.shouldCreate) {
      profiles["cics"] = entry("cics", props(
        "host" to state.cicsProfile.host.value,
        "port" to state.cicsProfile.port.value,
        "user" to state.cicsProfile.user.value,
        "regionName" to state.cicsProfile.regionName.value,
        "cicsPlex" to state.cicsProfile.cicsPlex.value,
        "rejectUnauthorized" to state.cicsProfile.rejectUnauthorized.value,
        "protocol" to state.cicsProfile.protocol.value
      ), buildList {
        if (state.cicsProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["cics"] = "cics"
    }

    if (state.db2Profile.shouldCreate) {
      profiles["db2"] = entry("db2", props(
        "host" to state.db2Profile.host.value,
        "port" to state.db2Profile.port.value,
        "user" to state.db2Profile.user.value,
        "database" to state.db2Profile.database.value,
        "sslFile" to state.db2Profile.sslFile.value
      ), buildList {
        if (state.db2Profile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["db2"] = "db2"
    }

    if (state.mqProfile.shouldCreate) {
      profiles["mq"] = entry("mq", props(
        "host" to state.mqProfile.host.value,
        "port" to state.mqProfile.port.value,
        "user" to state.mqProfile.user.value,
        "rejectUnauthorized" to state.mqProfile.rejectUnauthorized.value,
        "protocol" to state.mqProfile.protocol.value
      ), buildList {
        if (state.mqProfile.password.value?.isNotEmpty() == true) add("password")
      })
      defaults["mq"] = "mq"
    }

    return linkedMapOf(
      "\$schema" to "./zowe.schema.json",
      "profiles" to profiles,
      "defaults" to defaults,
      "autoStore" to true
    )
  }
}