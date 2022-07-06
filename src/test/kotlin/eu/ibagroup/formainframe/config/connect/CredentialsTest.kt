/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect

import eu.ibagroup.formainframe.config.connect.ui.ConnectionDialogState
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Test class, which does not need any plugin component to unit test
 */
class CredentialsTest {
  val connectionDialogState = ConnectionDialogState(
    connectionName = "a", connectionUrl = "https://a.com",
    username = "a", password = "a"
  )

  /**
   * Tests the hashCode method of Credentials.
   */
  @Test
  fun testHashCode() {
    val credentials = Credentials(
      connectionDialogState.connectionConfig.uuid, connectionDialogState.username,
      connectionDialogState.password
    )
    val credentials2 = Credentials(
      connectionDialogState.connectionConfig.uuid, connectionDialogState.username,
      connectionDialogState.password
    )
    assertNotEquals(credentials.hashCode(), credentials2.hashCode())
  }
}