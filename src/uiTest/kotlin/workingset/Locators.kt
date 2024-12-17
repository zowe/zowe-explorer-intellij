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
package workingset

import com.intellij.remoterobot.search.locators.byXpath

//common dialogs locators
val myDialogXpathLoc = byXpath("//div[@class='MyDialog']")
val dialogRootPaneLoc = byXpath("//div[@class='DialogRootPane']")
val removeButtonLoc = byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']")
val removeButtonSubLoc = byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'dom.action.remove')]")
val removeButtonLocAnother = byXpath("//div[@myaction.key='button.text.remove']")
val removeButtonFromConfigLoc = byXpath("//div[@accessiblename='Remove' and @class='ActionButton' and @myaction='Remove (Remove)']")
val linkLoc = byXpath("//div[@text='More']")
val closeNotificationLoc = byXpath("//div[@tooltiptext.key='tooltip.close.notification']")

val addJesWorkingSetDialogLoc = byXpath("//div[@accessiblename='Add JES Working Set' and @class='MyDialog']")
val editJesWorkingSetDialogLoc = byXpath("//div[@accessiblename='Edit JES Working Set' and @class='MyDialog']")
val editJobFilterDialogLoc = byXpath("//div[@accessiblename='Edit Jobs Filter' and @class='MyDialog']")

//notification
val notificationTitle = byXpath("//div[@javaclass='javax.swing.JLabel']")

//allocate dialog locators
val datasetNameInputLoc = byXpath("//div[@class='JBTextField']")
val datasetOrgDropDownLoc = byXpath("//div[@class='ComboBox']")
val inputFieldLoc = byXpath("//div[@class='JBTextField']")
val dropdownsLoc = byXpath("//div[@class='ComboBox']")
val passwordInputLoc = byXpath("//div[@class='JPasswordField']")
val messageLoc = byXpath("//div[@class='HeavyWeightWindow']")
val helpLoc = byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='Header']]")
val errorDetailHeaderLoc = byXpath("//div[@javaclass='javax.swing.JEditorPane']")
val errorDetailBodyLoc = byXpath("//div[contains(@visible_text, 'Code:')]")
val errorDetailBodyLocAlt = byXpath("//div[contains(@visible_text, 'null')]")
val closeDialogLoc = byXpath("//div[@class='InplaceButton']")
val errorTipsTextLoc = byXpath("//div[@class='Header']")
val errorConnectionNotification = byXpath("//div[@title='Error Creating Connection']")
val errorContainsWordYou = byXpath("//div[@class='Wrapper'][.//div[contains(@visible_text, 'you')]]")

// property tab
val dataTabLoc = byXpath("//div[@text='Data']")

val treesLoc = byXpath("//div[@class='DnDAwareTree']")

//action locators
val callSettingButtonLoc =  byXpath("//div[@class='ActionButton' and @myicon='settings.svg' and @myaction=' ()']")

//settings locator
val addWsLoc = byXpath("//div[@accessiblename='Add' and @class='ActionButton' and @myaction='Add (Add)']")
val editWsLoc = byXpath("//div[@accessiblename='Edit' and @class='ActionButton' and @myaction='Edit (Edit)']")
val addWorkingSetDialogLoc = byXpath("//div[@accessiblename='Add Working Set' and @class='MyDialog']")
val addConnectionDialogLoc = byXpath("Add Connection Dialog", "//div[@accessiblename='Add Connection' and @class='MyDialog']")
val editConnectionDialogLoc = byXpath("Edit Connection Dialog", "//div[@accessiblename='Add Connection' and @class='MyDialog']")
val addWorkingSetDialogLocAlt = byXpath("//div[@title='Add JES Working Set']")
val editWorkingSetDialogLocAlt = byXpath("//div[@title='Edit JES Working Set']")
val wsLineLoc = byXpath("//div[@class='DialogPanel']//div[@class='JPanel']")
val sslCheckBox = byXpath("//div[@accessiblename='Accept self-signed SSL certificates' " +
        "and @class='JBCheckBox' and @text='Accept self-signed SSL certificates']")

//sub dialogs
const val subButtonLocPattern = "//div[@class='CustomFrameDialogContent'][.//div[@class='ComboBox']]//div[@text.key='button.%s']"
val subOkButtonLoc = byXpath(subButtonLocPattern.format(OK_TEXT.lowercase()))
val subCancelButtonLoc = byXpath(subButtonLocPattern.format(CANCEL_TEXT.lowercase()))

//errors
val invalidPortInTreesLoc = byXpath("//div[@class='MyComponent'][.//div[@accessiblename='Invalid URL port: \"104431\"' and @class='JEditorPane']]")
val errorCloseIconLoc = byXpath("//div[@class='ActionButton' and @myicon= 'close.svg']")

//job notifications
val jobSubmittedIdLoc = byXpath("//div[@class='Tree']")
val jobConsoleHeaderLoc = byXpath("//div[@class='TabPanel'][.//div[@text='Jobs:']]//div[@class='ContentTabLabel']")


//editor locators
val editorLoc = byXpath("//div[contains(@accessiblename.key, 'editor.accessible.name')]")
const val actionLocPattern = "//div[@class='ActionButton' and @myaction='%s']"

//add job constants
val addJobFilterLoc = byXpath("//div[contains(@myvisibleactions, 'Down')]//div[contains(@myaction.key, 'button.add.a')]")
val jobFiltersLoc = byXpath("//div[@class='JBScrollPane'][.//div[@visible_text='Prefix || Owner || Job ID']]//div[@class='JBTextField']")
val filterRowLoc = byXpath("//div[@class='ValidatingTableView']")
val jobFilterTableHeaderLoc = byXpath("//div[@class='JBTableHeader']")


//Eexplorer

val actionButtonLoc = byXpath("//div[@class='ActionButton' and @myicon='add.svg' and @myaction=' ()']")
val explorerLoc = byXpath("//div[@class='InternalDecoratorImpl']")
val jesExplorerTabNameLoc = byXpath( "//div[@text='$JES_EXPLORER_W' and @class='ContentTabLabel']")

//jobs terminal

val jobConsolePurgeButton = byXpath("//div[@class='ActionButton' and @myaction='Purge Job ()']")


//add connection dialog
val okButtonAddConnection = byXpath("//div[@class='CustomFrameDialogContent'][.//div[@class='JBCheckBox']]//div[@text='OK']")
val cancelButtonAddConnection = byXpath("//div[@class='CustomFrameDialogContent'][.//div[@class='JBCheckBox']]//div[@text='Cancel']")
