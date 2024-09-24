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
package workingset.auxiliary.components.dialogs

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.fixtures.JTextFieldFixture
import com.intellij.remoterobot.search.locators.Locator
import workingset.Constants.remoteRobotUrl
import workingset.JOB_PROPERTIES_DIALOG
import workingset.OK_TEXT
import workingset.auxiliary.components.elements.ButtonElement
import workingset.datasetNameInputLoc

class JobPropertiesDialog(fixtureStack: MutableList<Locator>, remoteRobot: RemoteRobot) :AbstractDialog(fixtureStack, remoteRobot)  {

    override val dialogTitle: String = JOB_PROPERTIES_DIALOG
    var okButton = ButtonElement()

    init {
        okButton = ButtonElement(OK_TEXT, fixtureStack, remoteRobot)
    }

    constructor() : this(mutableListOf<Locator>(), RemoteRobot(remoteRobotUrl))

    /**
     * Checks if the provided job properties and the properties in opened Job Properties Dialog are matching.
     * On General tab
     */
    fun checkTabParam(expectedTabParams:List<String>, propertyMap: Map<String,String>): Boolean  = with(remoteRobot) {
        val jobGeneralParams = findAll<JTextFieldFixture>(datasetNameInputLoc)
        expectedTabParams.withIndex().forEach { (index, element) ->
            if(propertyMap.getValue(element) != jobGeneralParams[index].text) {
                return false
            }
        }
        return true
    }
}
