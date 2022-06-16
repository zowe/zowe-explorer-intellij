/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.config.connect.ui

import eu.ibagroup.formainframe.config.ConfigSandboxImpl
import eu.ibagroup.formainframe.config.UnitTestCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertNotEquals

/**
 * Testing a class, which only needs the existence of an Application to function properly
 */
class ConnectionsTableModelTest: UnitTestCase() {
    val sandbox = ConfigSandboxImpl()
    val conTab = ConnectionsTableModel(sandbox.crudable)
    val connectionDialogStateA = ConnectionDialogState(connectionName = "a", connectionUrl = "https://a.com",
        username = "a", password = "a")
    val connectionDialogStateB = ConnectionDialogState(connectionName = "b", connectionUrl = "https://b.com",
        username = "b", password = "b")

    /**
     * tests the fetch method of ConnectionTableModel
     */
    @Test
    fun fetch() {
        conTab.addRow(connectionDialogStateA)
        assertEquals(mutableListOf(connectionDialogStateA),conTab.fetch(sandbox.crudable))
    }

    /**
     * tests the onAdd method of ConnectionTableModel
     */
    @Test
    fun onAdd() {
        conTab.onAdd(sandbox.crudable, connectionDialogStateA)
        conTab.onAdd(sandbox.crudable, connectionDialogStateB)
        assertEquals(mutableListOf(connectionDialogStateA,connectionDialogStateB),conTab.fetch(sandbox.crudable))
    }

    /**
     * Tests what happens to the ConnectionTableModel if we try to add two connections of the same name.
     * Should return only one connection.
     */
    @Test
    fun onAddExistingName() {
        val connectionDialogState = ConnectionDialogState(connectionName = connectionDialogStateA.connectionName)
        conTab.onAdd(sandbox.crudable, connectionDialogStateA)
        conTab.onAdd(sandbox.crudable, connectionDialogState)
        assertEquals(mutableListOf(connectionDialogStateA),conTab.fetch(sandbox.crudable))
    }

    /**
     * Tests what happens to the ConnectionTableModel if we try to add two connections with the same url.
     */
    @Test
    fun onAddExistingUrl() {
        val connectionDialogState = ConnectionDialogState(connectionUrl = connectionDialogStateA.connectionUrl)
        conTab.onAdd(sandbox.crudable, connectionDialogStateA)
        conTab.onAdd(sandbox.crudable, connectionDialogState)
        assertEquals(mutableListOf(connectionDialogStateA,connectionDialogState),conTab.fetch(sandbox.crudable))
    }

    /**
     * Tests the onDelete method of ConnectionTableModel.
     */
    @Test
    fun onDelete() {
        conTab.onAdd(sandbox.crudable, connectionDialogStateA)
        conTab.onDelete(sandbox.crudable, connectionDialogStateA)
        assertEquals(mutableListOf<ConnectionDialogState>(),conTab.fetch(sandbox.crudable))
    }

    /**
     * Tests the set method of ConnectionTableModel.
     */
    @Test
    fun set() {
        conTab.addRow(ConnectionDialogState())
        conTab[0] = connectionDialogStateA
        assertEquals(connectionDialogStateA.connectionName,conTab[0].connectionName)
        assertEquals(connectionDialogStateA.connectionUrl,conTab[0].connectionUrl)
        assertEquals(connectionDialogStateA.username,conTab[0].username)
        assertEquals(connectionDialogStateA.password,conTab[0].password)
        assertNotEquals(connectionDialogStateA.connectionUuid,conTab[0].connectionUuid)
    }
}