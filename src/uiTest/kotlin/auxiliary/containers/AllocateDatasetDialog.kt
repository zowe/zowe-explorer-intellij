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

package auxiliary.containers

import auxiliary.closable.ClosableCommonContainerFixture
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import workingset.*
import java.time.Duration

/**
 * Class representing the Allocate Dataset Dialog.
 */
@FixtureName("Allocate Dataset Dialog")
open class AllocateDatasetDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Fills in the parameters for allocating dataset.
     */
    fun allocateDataset(
        datasetName: String,
        datasetOrganization: String,
        allocationUnit: String,
        primaryAllocation: Int,
        secondaryAllocation: Int,
        directory: Int,
        recordFormat: String,
        recordLength: Int,
        blockSize: Int,
        averageBlockLength: Int = 0
    ) {
        findAll<JTextFieldFixture>(datasetNameInputLoc)[0].text = datasetName
        findAll<ComboBoxFixture>(datasetOrgDropDownLoc)[1].selectItem(datasetOrganization)

        val datasetTextParams = findAll<JTextFieldFixture>(inputFieldLoc)
        val datasetComboBoxParams = findAll<ComboBoxFixture>(dropdownsLoc)

        datasetComboBoxParams[2].selectItem(allocationUnit)
        datasetTextParams[1].text = primaryAllocation.toString()
        datasetTextParams[2].text = secondaryAllocation.toString()
        datasetComboBoxParams[3].selectItem(recordFormat)

        if (datasetOrganization == SEQUENTIAL_ORG_FULL) {
            datasetTextParams[3].text = recordLength.toString()
            datasetTextParams[4].text = blockSize.toString()
            datasetTextParams[5].text = averageBlockLength.toString()
        } else {
            datasetTextParams[3].text = directory.toString()
            datasetTextParams[4].text = recordLength.toString()
            datasetTextParams[5].text = blockSize.toString()
            datasetTextParams[6].text = averageBlockLength.toString()
        }
    }

    //TODO add allocate dataset with advanced parameters when switched tp mock

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Allocate Dataset Dialog"

        /**
         * Returns the xPath of the Add Working Set Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Allocate Dataset' and @class='MyDialog']")
    }
}

/**
 * Finds the AllocateDatasetDialog and modifies fixtureStack.
 */
fun ContainerFixture.allocateDatasetDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: AllocateDatasetDialog.() -> Unit = {}
) {
    find<AllocateDatasetDialog>(AllocateDatasetDialog.xPath(), timeout).apply {
        fixtureStack.add(AllocateDatasetDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
