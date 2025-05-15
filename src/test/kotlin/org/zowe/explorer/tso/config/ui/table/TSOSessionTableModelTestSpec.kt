/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 *
 * Contributors:
 *   Zowe Community
 *   Dzianis Lisiankou
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.tso.config.ui.table

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.zowe.explorer.testutils.MockkAwareShouldSpec
import org.zowe.explorer.tso.config.TSOSessionConfig
import org.zowe.explorer.tso.config.toDialogState
import org.zowe.explorer.tso.config.ui.TSOSessionDialogState
import org.zowe.explorer.utils.clone
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.crudable.MergedCollections
import java.util.*

class TSOSessionTableModelTestSpec : MockkAwareShouldSpec({
  context("tso/config/ui/table/TSOSessionTableModel") {
    context("all functions") {
      var didDeleteCorrectData = false
      var didAddCorrectData = false
      var didUpdateCorrectData = false

      val tsoSessionConfigAlreadyThere = TSOSessionConfig()
      tsoSessionConfigAlreadyThere.uuid = "test_already_added"
      val tsoSessionConfigToAdd = TSOSessionConfig()
      tsoSessionConfigToAdd.uuid = "test_to_add"
      val tsoSessionConfigToUpdate = TSOSessionConfig()
      tsoSessionConfigToUpdate.uuid = "test_to_update_old"
      tsoSessionConfigToUpdate.name = "old_name"
      val tsoSessionConfigWithChanges = tsoSessionConfigToUpdate.clone()
      tsoSessionConfigWithChanges.uuid = "test_to_update_new"
      tsoSessionConfigWithChanges.name = "new_name"
      val tsoSessionConfigToDelete = TSOSessionConfig()
      tsoSessionConfigToDelete.uuid = "test_to_delete"

      val crudableMock = mockk<Crudable> {
        every {
          getAll(any<Class<TSOSessionConfig>>())
        } answers {
          listOf(tsoSessionConfigAlreadyThere, tsoSessionConfigToUpdate, tsoSessionConfigToDelete).stream()
        }
        every {
          applyMergedCollections(TSOSessionConfig::class.java, any<MergedCollections<TSOSessionDialogState>>())
        } answers {
          callOriginal()
        }
        every { getByUniqueKey(any<Class<*>>(), any<Any>()) } returns Optional.ofNullable(null)
        every { find(any<Class<*>>(), any()) } answers { listOf<Any>().stream() }
        every {
          delete(TSOSessionConfig::class.java, any<TSOSessionConfig>())
        } answers {
          didDeleteCorrectData = secondArg<TSOSessionConfig>().uuid == tsoSessionConfigToDelete.uuid
          Optional.of(tsoSessionConfigToDelete)
        }
      }

      val tsoSessionTableModelMock = TSOSessionTableModel(crudableMock)

      beforeEach {
        didDeleteCorrectData = false
        didAddCorrectData = false
        didUpdateCorrectData = false

        every {
          crudableMock.delete(any<TSOSessionConfig>())
        } answers {
          didDeleteCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigToDelete.uuid
          Optional.of(tsoSessionConfigToDelete)
        }
      }

      should("apply merged collections to the CRUD-able") {
        val mergedCollections = MergedCollections(
          listOf(tsoSessionConfigToAdd.toDialogState()),
          listOf(tsoSessionConfigWithChanges.toDialogState()),
          listOf(tsoSessionConfigToDelete.toDialogState())
        )

        every {
          crudableMock.update(TSOSessionConfig::class.java, any<TSOSessionConfig>())
        } answers {
          didUpdateCorrectData = secondArg<TSOSessionConfig>().uuid == tsoSessionConfigWithChanges.uuid
          Optional.of(tsoSessionConfigWithChanges)
        }
        every {
          crudableMock.add(TSOSessionConfig::class.java, any<TSOSessionConfig>())
        } answers {
          didAddCorrectData = secondArg<TSOSessionConfig>().uuid == tsoSessionConfigToAdd.uuid
          Optional.of(tsoSessionConfigToAdd)
        }

        tsoSessionTableModelMock.onApplyingMergedCollection(crudableMock, mergedCollections)

        assertSoftly {
          didAddCorrectData shouldBe true
          didDeleteCorrectData shouldBe true
          didUpdateCorrectData shouldBe true
        }
      }
      should("delete a TSO session config from CRUD-able") {
        tsoSessionTableModelMock.onDelete(crudableMock, tsoSessionConfigToDelete.toDialogState())
        assertSoftly { didDeleteCorrectData shouldBe true }
      }
      should("update a TSO session config in CRUD-able") {
        every {
          crudableMock.update(any<TSOSessionConfig>())
        } answers {
          didUpdateCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigWithChanges.uuid
          Optional.of(tsoSessionConfigWithChanges)
        }

        val didUpdate = tsoSessionTableModelMock.onUpdate(crudableMock, tsoSessionConfigWithChanges.toDialogState())
        assertSoftly {
          didUpdate shouldBe true
          didUpdateCorrectData shouldBe true
        }
      }
      should("not update a TSO session config in CRUD-able when there are no changes") {
        every {
          crudableMock.update(any<TSOSessionConfig>())
        } answers {
          didUpdateCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigWithChanges.uuid
          Optional.ofNullable(null)
        }

        val didUpdate = tsoSessionTableModelMock.onUpdate(crudableMock, tsoSessionConfigToUpdate.toDialogState())
        assertSoftly {
          didUpdate shouldBe false
          didUpdateCorrectData shouldBe false
        }
      }
      should("not update a TSO session config in CRUD-able and return null instead of a TSO session config") {
        every { crudableMock.update(any<TSOSessionConfig>()) } returns null

        val didUpdate = tsoSessionTableModelMock.onUpdate(crudableMock, tsoSessionConfigToUpdate.toDialogState())
        assertSoftly {
          didUpdate shouldBe false
        }
      }
      should("add a new TSO session config in CRUD-able") {
        every {
          crudableMock.add(any<TSOSessionConfig>())
        } answers {
          didAddCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigToAdd.uuid
          Optional.of(tsoSessionConfigToAdd)
        }

        val didAdd = tsoSessionTableModelMock.onAdd(crudableMock, tsoSessionConfigToAdd.toDialogState())
        assertSoftly {
          didAdd shouldBe true
          didAddCorrectData shouldBe true
        }
      }
      should("not add a new TSO session config in CRUD-able when there is already the same config added") {
        every {
          crudableMock.add(any<TSOSessionConfig>())
        } answers {
          didAddCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigToAdd.uuid
          Optional.ofNullable(null)
        }

        val didAdd = tsoSessionTableModelMock.onAdd(crudableMock, tsoSessionConfigAlreadyThere.toDialogState())
        assertSoftly {
          didAdd shouldBe false
          didAddCorrectData shouldBe false
        }
      }
      should("not add a new TSO session config in CRUD-able and return null instead of a TSO session config") {
        every {
          crudableMock.add(any<TSOSessionConfig>())
        } answers {
          didAddCorrectData = firstArg<TSOSessionConfig>().uuid == tsoSessionConfigToAdd.uuid
          null
        }

        val didAdd = tsoSessionTableModelMock.onAdd(crudableMock, tsoSessionConfigAlreadyThere.toDialogState())
        assertSoftly {
          didAdd shouldBe false
        }
      }
      should("set row to table model") {
        val tsoSessionConfig = TSOSessionConfig()

        tsoSessionTableModelMock[0] = tsoSessionConfig.toDialogState()
      }
    }
  }
})
