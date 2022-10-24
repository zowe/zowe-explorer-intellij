/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package auxiliary.closable

import auxiliary.containers.*
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.search.locators.Locator
import java.time.Duration

/**
 * The fixture that needs to be closed in the tear-down method.
 */
data class ClosableFixtureItem(
    var name: String,
    /**
     * List of all the xPaths of all its closable predecessors.
     */
    var fixtureStack: MutableList<Locator>
)

/**
 * Class which collects the fixtures that needed to be clsoed in the tear-down method and closes them.
 */
class ClosableFixtureCollector {
    /**
     * List of the fixtures that needed to be closed in the tear-down method.
     */
    var items = mutableListOf<ClosableFixtureItem>()

    /**
     * Adds closable fixture to the list.
     */
    fun add(xPath: Locator, stack: List<Locator>) {
        items.add(ClosableFixtureItem(xPath.byDescription, (stack + xPath).toMutableList()))
    }

    /**
     * Finds the closable fixture by its Locator.
     */
    fun findClosable(remoteRobot: RemoteRobot, locator: Locator) = with(remoteRobot) {
        when (locator.byDescription) {
            SettingsDialog.name -> find<SettingsDialog>(locator, Duration.ofSeconds(60))
            AddConnectionDialog.name -> find<AddConnectionDialog>(locator, Duration.ofSeconds(60))
            EditConnectionDialog.name -> find<EditConnectionDialog>(locator, Duration.ofSeconds(60))
            ErrorCreatingConnectionDialog.name -> find<ErrorCreatingConnectionDialog>(
                locator,
                Duration.ofSeconds(60)
            )

            AddWorkingSetDialog.name -> find<AddWorkingSetDialog>(locator, Duration.ofSeconds(60))
            EditWorkingSetDialog.name -> find<EditWorkingSetDialog>(locator, Duration.ofSeconds(60))
            CreateMaskDialog.name -> find<CreateMaskDialog>(locator, Duration.ofSeconds(60))
            AllocateDatasetDialog.name -> find<AllocateDatasetDialog>(locator, Duration.ofSeconds(60))
            AddJesWorkingSetDialog.name -> find<AddJesWorkingSetDialog>(locator, Duration.ofSeconds(60))
            EditJesWorkingSetDialog.name -> find<EditJesWorkingSetDialog>(locator, Duration.ofSeconds(60))
            IdeFrameImpl.xPath("untitled").byDescription -> find<IdeFrameImpl>(
                locator,
                Duration.ofSeconds(60)
            )

            else -> throw IllegalAccessException("There is no corresponding class to ${locator.byDescription}")
        }
    }

    /**
     * Closes a single member of the items list.
     *
     * Is a recursive function. Should be called with i = 0.
     */
    fun closeItem(i: Int, item: ClosableFixtureItem, remoteRobot: RemoteRobot) {
        findClosable(remoteRobot, item.fixtureStack[i]).apply {
            if (i == item.fixtureStack.size - 1) {
                close()
                items.remove(item)
            } else {
                closeItem(i + 1, item, remoteRobot)
            }
        }
    }

    /**
     * Closes all closable fixtures, which we want to close.
     */
    fun closeWantedClosables(wantToClose: List<String>, remoteRobot: RemoteRobot) {
        for (item in items.reversed()) {
            if (item.name in wantToClose) {
                closeItem(0, item, remoteRobot)
            }
        }
    }

    /**
     * Closes a single closable fixture by name if it exists.
     */
    fun closeOnceIfExists(name: String) {
        for (item in items) {
            if (item.name == name) {
                items.remove(item)
                return
            }
        }
    }

}