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
 * Class representing the Job Properties Dialog.
 */
@FixtureName("Job Properties Dialog")
open class JobPropertiesDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {

    /**
     * Checks if the provided job properties and the properties in opened Job Properties Dialog are matching.
     */
    fun areJobPropertiesValid(
        jobId: String,
        jobName: String,
        subsystem: String,
        owner: String,
        status: String,
        jobType: String,
        jobClass: String,
        returnCode: String,
        correlator: String,
        phase: String,
        phaseName: String,
        url: String,
        filesUrl: String,
        executor: String,
        reasonNotRunning: String,
        submitted: String,
        startTime: String,
        endTime: String): Boolean {
        var result = true
        val jobGeneralParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(jobGeneralParams[0].text != jobId || jobGeneralParams[1].text != jobName ||
            jobGeneralParams[2].text != subsystem || jobGeneralParams[3].text != owner ||
            jobGeneralParams[4].text != status || jobGeneralParams[5].text != jobType ||
            jobGeneralParams[6].text != jobClass || jobGeneralParams[7].text != returnCode ||
            jobGeneralParams[8].text != correlator){
            result = false
        }

        find<ComponentFixture>(byXpath("//div[@text='Data']")).click()
        Thread.sleep(1000)
        val jobDataParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))
        if(jobDataParams[0].text != phase || jobDataParams[1].text != phaseName ||
            jobDataParams[2].text != url || jobDataParams[3].text != filesUrl ||
            jobDataParams[4].text != executor || jobDataParams[5].text != reasonNotRunning ||
            jobDataParams[6].text != submitted || jobDataParams[7].text != startTime ||
            jobDataParams[8].text != endTime){
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
        const val name = "Job Properties Dialog"

        /**
         * Returns the xPath of the Job Properties Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath(name, "//div[@accessiblename='Job Properties' and @class='MyDialog']")
    }
}

/**
 * Finds the JobPropertiesDialog and modifies fixtureStack.
 */
fun ContainerFixture.jobPropertiesDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: JobPropertiesDialog.() -> Unit = {}
) {
    find<JobPropertiesDialog>(JobPropertiesDialog.xPath(), timeout).apply {
        fixtureStack.add(JobPropertiesDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}
