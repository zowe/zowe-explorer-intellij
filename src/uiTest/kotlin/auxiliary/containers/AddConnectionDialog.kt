package auxiliary.containers

import auxiliary.ClosableCommonContainerFixture
import auxiliary.clickButton
import com.intellij.remoterobot.RemoteRobot
import com.intellij.remoterobot.data.RemoteComponent
import com.intellij.remoterobot.fixtures.*
import com.intellij.remoterobot.search.locators.Locator
import com.intellij.remoterobot.search.locators.byXpath
import java.time.Duration

/**
 * Finds the AddConnectionDialog and modifies fixtureStack.
 */
fun ContainerFixture.addConnectionDialog(
    fixtureStack: MutableList<Locator>,
    timeout: Duration = Duration.ofSeconds(60),
    function: AddConnectionDialog.() -> Unit = {}) {
    find<AddConnectionDialog>(AddConnectionDialog.xPath(), timeout).apply {
        fixtureStack.add(AddConnectionDialog.xPath())
        function()
        fixtureStack.removeLast()
    }
}

/**
 * Class representing the Add Connection Dialog.
 */
@FixtureName("Add Connection Dialog")
open class AddConnectionDialog(
    remoteRobot: RemoteRobot,
    remoteComponent: RemoteComponent
) : ClosableCommonContainerFixture(remoteRobot, remoteComponent) {
    val connectionTextParams = findAll<JTextFieldFixture>(byXpath("//div[@class='JBTextField']"))

    /**
     * Fills in the required information for adding a new connection.
     */
    fun addConnection(connectionName: String, connectionUrl: String, username: String, password: String, ssl: Boolean) {
        connectionTextParams[0].text = connectionName
        connectionTextParams[1].text = connectionUrl
        connectionTextParams[2].text = username
        textField(byXpath("//div[@class='JPasswordField']")).text = password
        if (ssl) {
            checkBox(byXpath("//div[@accessiblename='Accept self-signed SSL certificates' " +
                    "and @class='JBCheckBox' and @text='Accept self-signed SSL certificates']"))
                .select()
        }
    }

    /**
     * Clicks on the Cancel button.
     */
    fun cancel() {
        clickButton("Cancel")
    }

    /**
     * Clicks on the Ok button.
     */
    fun ok() {
        clickButton("OK")
    }

    /**
     * The close function, which is used to close the dialog in the tear down method.
     */
    override fun close() {
        cancel()
    }
    companion object {
        const val name = "Add Connection Dialog"

        /**
         * Returns the xPath of the Add Connection Dialog.
         */
        @JvmStatic
        fun xPath() = byXpath( name,"//div[@accessiblename='Add Connection' and @class='MyDialog']")
    }
}