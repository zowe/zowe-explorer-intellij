package workingset

import com.intellij.remoterobot.search.locators.byXpath

//common dialogs locators
val myDialogXpathLoc = byXpath("//div[@class='MyDialog']")


//allocate dialog locators
val datasetNameInputLoc = byXpath("//div[@class='JBTextField']")
val datasetOrgDropDownLoc = byXpath("//div[@class='ComboBox']")
val inputFieldLoc = byXpath("//div[@class='JBTextField']")
val dropdownsLoc = byXpath("//div[@class='ComboBox']")