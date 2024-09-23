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
 * Class representing the Spool File Properties Dialog.
 */
@FixtureName("Spool File Properties Dialog")
open class SpoolFilePropertiesDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Checks if the provided spool file properties and the properties in opened Spool File Properties Dialog are matching.
     */
    fun areSpoolFilePropertiesValid(
        jobId: String,
        jobName: String,
        correlator: String,
        className: String,
        id: String,
        ddName: String,
        stepName: String,
        processStep: String,
        recordFormat: String,
        byteContent: String,
        recordCount: String,
        recordUrl: String,
        recordLen: String,
        subsystem: String): Boolean {
        var result = true
        val jobGeneralParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(jobGeneralParams[0].text != jobId || jobGeneralParams[1].text != jobName ||
            jobGeneralParams[2].text != correlator || jobGeneralParams[3].text != className ||
            jobGeneralParams[4].text != id || jobGeneralParams[5].text != ddName ||
            jobGeneralParams[6].text != stepName || jobGeneralParams[7].text != processStep){
            result = false
        }

        find<ComponentFixture>(byXpath("//div[@text='Data']")).click()
        Thread.sleep(1000)
        val jobDataParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(jobDataParams[0].text != recordFormat || jobDataParams[1].text != byteContent ||
            jobDataParams[2].text != recordCount || jobDataParams[3].text != recordUrl ||
            jobDataParams[4].text != recordLen || jobDataParams[5].text != subsystem){
            result = false
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
        const val name = "Spool File Properties Dialog"

        /**
         * Returns the xPath of the Spool File Properties Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Spool File Properties' and @class='MyDialog']")
    }
}

/**
 * Finds the SpoolFilePropertiesDialog and modifies fixtureStack.
 */
fun ContainerFixture.spoolFilePropertiesDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: SpoolFilePropertiesDialog.() -> Unit = {}
) {
    find<SpoolFilePropertiesDialog>(SpoolFilePropertiesDialog.xPath(), timeout).apply {
        fixtureStack.add(SpoolFilePropertiesDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
