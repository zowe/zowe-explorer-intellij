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

package org.zowe.explorer.explorer

import com.intellij.openapi.Disposable
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.ConfigStateV2
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.makeCrudableWithoutListeners
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.UssPath
import org.zowe.explorer.utils.gson
import org.zowe.explorer.utils.optional
import org.zowe.explorer.utils.toMutableList
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import java.util.*
import java.util.stream.Stream

class FilesWorkingSetImplTestSpec : AppInitShouldSpec("explorer/FilesWorkingSetImpl", {
  context("all functions") {
    val uuid1 = "uuid1"
    val mockedFilesWSConfig = mockk<FilesWorkingSetConfig> {
      every { uuid } returns uuid1
      every { name } returns "filesWSuuid1"
      every { connectionConfigUuid } returns "connUuid"
      every { dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
    }
    val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })

    mockkObject(gson)

    val clonedConfig = FilesWorkingSetConfig(
      uuid1,
      "filesWSuuid1",
      "connUuid",
      mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
      mutableListOf()
    )

    every { gson.toJson(any<FilesWorkingSetConfig>()) } returns "mocked_config_to_copy"
    every { gson.fromJson(any<String>(), FilesWorkingSetConfig::class.java) } returns clonedConfig

    val configService = ConfigService.getService()
    val configServiceCrudableMock = mockk<Crudable>()
    every { configService.crudable } returns configServiceCrudableMock

    beforeEach {
      clonedConfig.ussPaths = spyk(mutableListOf())

      every { configServiceCrudableMock.update(any()) } returns null
    }

    context("addUssPath") {
      every {
        mockedCrud.getAll(FilesWorkingSetConfig::class.java)
      } answers {
        Stream.of(
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf(UssPath("/u/test1"))
          ),
          FilesWorkingSetConfig(
            uuid1,
            "filesWSuuid1",
            "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf()
          )
        )
      }

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual1: Optional<FilesWorkingSetConfig>? = null

      beforeEach {
        clonedConfig.ussPaths = spyk(mutableListOf())

        every { mockedFilesWSConfig.ussPaths } returns mutableListOf()

        every {
          configServiceCrudableMock.update(any<FilesWorkingSetConfig>())
        } answers {
          actual1 =
            FilesWorkingSetConfig(
              uuid1,
              "filesWSuuid1",
              "connUuid",
              mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
              mutableListOf(UssPath("/u/test1"))
            ).optional
          actual1
        }
      }

      // addUssPath when clone and collection.add succeeds
      should("add USS path to a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every {
          clonedConfig.ussPaths.add(any<UssPath>())
        } answers {
          true
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))

        val expected = expectedValues[0].optional

        assertSoftly { actual1 shouldBe expected }
      }

      // addUssPath when clone succeeds but collection.add is not
      should("add USS path to a config if collection.add is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every {
          clonedConfig.ussPaths.add(any<UssPath>())
        } answers {
          false
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))
        val actual2 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly { actual2 shouldBe expected }
      }

      // addUssPath with null config
      should("not add USS path to a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.addUssPath(UssPath("/u/test2"))
        val actual3 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly { actual3 shouldBe expected }
      }
    }

    context("removeUssPath") {
      every {
        mockedCrud.getAll(FilesWorkingSetConfig::class.java)
      } answers {
        Stream.of(
          FilesWorkingSetConfig(
            uuid1, "filesWSuuid1", "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf()
          ),
          FilesWorkingSetConfig(
            uuid1, "filesWSuuid1", "connUuid",
            mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
            mutableListOf(UssPath("/u/uss_path_to_remove"))
          )
        )
      }

      fun getMockedFilesWorkingSetConfigNotNull(): FilesWorkingSetConfig {
        return mockedFilesWSConfig
      }

      fun getMockedFilesWorkingSetConfigNull(): FilesWorkingSetConfig? {
        return null
      }

      val mockedFileExplorer =
        mockk<AbstractExplorerBase<ConnectionConfig, FilesWorkingSetImpl, FilesWorkingSetConfig>>()
      val mockedDisposable = mockk<Disposable>()
      val expectedValues = mockedCrud.getAll(FilesWorkingSetConfig::class.java).toMutableList()

      var actual4: Optional<FilesWorkingSetConfig>? = null

      beforeEach {
        clonedConfig.ussPaths = spyk(mutableListOf(UssPath("/u/uss_path_to_remove")))

        every { mockedFilesWSConfig.ussPaths } returns mutableListOf(UssPath("/u/uss_path_to_remove"))

        every {
          configServiceCrudableMock.update(any<FilesWorkingSetConfig>())
        } answers {
          actual4 =
            FilesWorkingSetConfig(
              uuid1,
              "filesWSuuid1",
              "connUuid",
              mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
              mutableListOf()
            ).optional
          actual4
        }
      }

      // removeUssPath when clone and collection.remove succeeds
      should("remove USS path from a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every {
          clonedConfig.ussPaths.remove(any<UssPath>())
        } answers {
          true
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))

        val expected = expectedValues[0].optional

        assertSoftly { actual4 shouldBe expected }
      }

      // removeUssPath when clone succeeds but collection.remove is not
      should("remove USS path from a config if collection.remove is not succeeded") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )
        every {
          clonedConfig.ussPaths.remove(any<UssPath>())
        } answers {
          false
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual5 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly { actual5 shouldBe expected }
      }

      // removeUssPath with null config
      should("not remove USS path from a config as working set config is null") {
        val filesWorkingSetImpl2 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNull() },
            mockedDisposable
          )
        )

        filesWorkingSetImpl2.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual6 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly { actual6 shouldBe expected }
      }
    }
  }
})
