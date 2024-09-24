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
import java.time.Duration

/**
 * Class representing the Dataset Properties Dialog.
 */
@FixtureName("Dataset Properties Dialog")
open class DatasetPropertiesDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Checks if the provided dataset properties and the properties in opened Dataset Properties Dialog are matching.
     */
    fun areDatasetPropertiesValid(
        dsName: String,
        dsNameType: String,
        catalogName: String,
        volumeSerials: String,
        deviceType: String,
        organization: String,
        recordFormat: String,
        recordLength: String,
        blockSize: String,
        sizeInTracks: String,
        spaceUnits: String,
        usedTracks: String,
        usedExtents: String,
        createDate: String,
        modDate: String,
        expirationDate: String): Boolean {
        var result = true
        val dsGeneralParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(dsGeneralParams[0].text != dsName || dsGeneralParams[1].text != dsNameType ||
            dsGeneralParams[2].text != catalogName || dsGeneralParams[3].text != volumeSerials ||
            dsGeneralParams[4].text != deviceType){
            result = false
        }

        find<ComponentFixture>(byXpath("//div[@text='Data']")).click()
        Thread.sleep(1000)
        val dsDataParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(dsDataParams[0].text != organization || dsDataParams[1].text != recordFormat ||
            dsDataParams[2].text != recordLength || dsDataParams[3].text != blockSize ||
            dsDataParams[4].text != sizeInTracks || dsDataParams[5].text != spaceUnits){
            result = false
        }

        find<ComponentFixture>(byXpath("//div[@text='Extended']")).click()
        Thread.sleep(1000)
        val dsExtendedParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(dsExtendedParams[0].text != usedTracks || dsExtendedParams[1].text != usedExtents ||
            dsExtendedParams[2].text != createDate || dsExtendedParams[3].text != modDate ||
            dsExtendedParams[4].text != expirationDate){
            result = false
        }

        return result
    }

    /**
     * Checks if the dataset is migrated.
     */
    fun isDatasetMigrated(): Boolean {
        var result = false
        val dsDataParams = findAll<JLabelFixture>(byXpath("//div[@class='JBTextField']"))
        if(dsDataParams.last().hasText("Dataset has migrated.")){
            result = true
        }

        return result
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        clickButton("Cancel")
    }

    companion object {
        const val name = "Dataset Properties Dialog"

        /**
         * Returns the xPath of the Dataset Properties Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Dataset Properties' and @class='MyDialog']")
    }
}

/**
 * Finds the DatasetPropertiesDialog and modifies fixtureStack.
 */
fun ContainerFixture.datasetPropertiesDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: DatasetPropertiesDialog.() -> Unit = {}
) {
    find<DatasetPropertiesDialog>(DatasetPropertiesDialog.xPath(), timeout).apply {
        fixtureStack.add(DatasetPropertiesDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
