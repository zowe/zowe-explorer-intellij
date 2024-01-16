/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package workingset

import com.intellij.remoterobot.search.locators.byXpath

//common dialogs locators
val myDialogXpathLoc = byXpath("//div[@class='MyDialog']")
val removeButtonLoc = byXpath("//div[contains(@myvisibleactions, 'Down')]//div[@myaction.key='button.text.remove']")
val removeEMaskButtonLoc = byXpath("//div[contains(@myaction.key, 'dom.action.remove')]")
val linkLoc = byXpath("//div[@class='LinkLabel']")
val closeNotificationLoc = byXpath("//div[@tooltiptext.key='tooltip.close.notification']")

//allocate dialog locators
val datasetNameInputLoc = byXpath("//div[@class='JBTextField']")
val datasetOrgDropDownLoc = byXpath("//div[@class='ComboBox']")
val inputFieldLoc = byXpath("//div[@class='JBTextField']")
val dropdownsLoc = byXpath("//div[@class='ComboBox']")
val messageLoc = byXpath("//div[@class='HeavyWeightWindow']")
val helpLoc = byXpath("//div[@class='HeavyWeightWindow'][.//div[@class='Header']]")
val errorDetailHeaderLoc = byXpath("//div[@javaclass='javax.swing.JEditorPane']")
val errorDetailBodyLoc = byXpath("//div[contains(@visible_text, 'Code:')]")
val closeDialogLoc = byXpath("//div[@class='InplaceButton']")

// property tab
val dataTabLoc = byXpath("//div[@text='Data']")

val treesLoc = byXpath("//div[@class='DnDAwareTree']")
