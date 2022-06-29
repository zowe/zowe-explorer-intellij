package auxiliary.components

import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.ComponentFixture
import com.intellij.remoterobot.fixtures.ContainerFixture
import com.intellij.remoterobot.fixtures.FixtureName
import com.intellij.remoterobot.search.locators.Locator
import java.time.Duration

/**
 * Function, which looks for the StripeButton.
 */
fun ContainerFixture.stripeButton(locator: Locator): StripeButtonFixture {
    return find(locator, Duration.ofSeconds(60))
}

/**
 * This class represents the StripeButton.
 */
@FixtureName("StripeButton")
open class StripeButtonFixture(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ComponentFixture(remoteRobot, remoteComponent) {}