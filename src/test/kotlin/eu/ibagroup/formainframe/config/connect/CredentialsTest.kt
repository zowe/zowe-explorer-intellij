package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Test class, which does not need any plugin component to unit test
 */
class CredentialsTest {
    val connectionDialogState = ConnectionDialogState(connectionName = "a", connectionUrl = "https://a.com",
        username = "a", password = "a")

    /**
     * Tests the hashCode method of Credentials.
     */
    @Test
    fun testHashCode() {
        val credentials = Credentials(connectionDialogState.connectionConfig.uuid, connectionDialogState.username,
            connectionDialogState.password)
        val credentials2 = Credentials(connectionDialogState.connectionConfig.uuid, connectionDialogState.username,
            connectionDialogState.password)
        assertNotEquals(credentials.hashCode(),credentials2.hashCode())
    }
}