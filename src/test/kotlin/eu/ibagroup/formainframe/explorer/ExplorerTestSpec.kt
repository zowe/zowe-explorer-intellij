/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.explorer

import com.intellij.openapi.Disposable
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.config.ConfigService
import eu.ibagroup.formainframe.config.ConfigStateV2
import eu.ibagroup.formainframe.config.configCrudable
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.makeCrudableWithoutListeners
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.config.ws.FilesWorkingSetConfig
import eu.ibagroup.formainframe.config.ws.UssPath
import eu.ibagroup.formainframe.utils.*
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.util.*
import java.util.stream.Stream

class ExplorerTestSpec : ShouldSpec({
  beforeSpec {
    // FIXTURE SETUP TO HAVE ACCESS TO APPLICATION INSTANCE
    val factory = IdeaTestFixtureFactory.getFixtureFactory()
    val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
    val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "for-mainframe")
    val fixture = fixtureBuilder.fixture
    val myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
      fixture,
      LightTempDirTestFixtureImpl(true)
    )
    myFixture.setUp()
  }
  afterSpec {
    clearAllMocks()
  }
  context("explorer module: FilesWorkingSetImpl") {

    context("addUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf()

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1, "filesWSuuid1", "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf()
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
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
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
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

      // addUssPath when clone and collection.add succeeds
      should("add USS path to a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual1 shouldBe expected
        }
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
        every { clonedConfig.ussPaths.add(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.addUssPath(UssPath("/u/test1"))
        val actual2 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual2 shouldBe expected
        }
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

        assertSoftly {
          actual3 shouldBe expected
        }
      }
    }

    context("removeUssPath") {
      val mockedCrud = spyk(makeCrudableWithoutListeners(false) { ConfigStateV2() })
      val uuid1 = "uuid1"
      mockkObject(ConfigService)
      every { ConfigService.instance.crudable } returns mockk()
      every { configCrudable } returns mockk()
      mockkObject(gson)

      val mockedFilesWSConfig = mockk<FilesWorkingSetConfig>()
      every { mockedFilesWSConfig.uuid } returns uuid1
      every { mockedFilesWSConfig.name } returns "filesWSuuid1"
      every { mockedFilesWSConfig.connectionConfigUuid } returns "connUuid"
      every { mockedFilesWSConfig.dsMasks } returns mutableListOf(DSMask("ZOSMFAD.*", mutableListOf()))
      every { mockedFilesWSConfig.ussPaths } returns mutableListOf(UssPath("/u/uss_path_to_remove"))

      every { gson.toJson(any() as FilesWorkingSetConfig) } returns "mocked_config_to_copy"

      val clonedConfig = FilesWorkingSetConfig(
        uuid1,
        "filesWSuuid1",
        "connUuid",
        mutableListOf(DSMask("ZOSMFAD.*", mutableListOf())),
        mutableListOf(UssPath("/u/uss_path_to_remove"))
      )
      every { gson.fromJson(any() as String, FilesWorkingSetConfig::class.java) } returns clonedConfig
      mockkObject(clonedConfig)

      every { mockedCrud.getAll(FilesWorkingSetConfig::class.java) } returns Stream.of(
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
      every { configCrudable.update(any() as FilesWorkingSetConfig) } answers {
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

      // removeUssPath when clone and collection.remove succeeds
      should("remove USS path from a config") {
        val filesWorkingSetImpl1 = spyk(
          FilesWorkingSetImpl(
            uuid1,
            mockedFileExplorer, { getMockedFilesWorkingSetConfigNotNull() },
            mockedDisposable
          )
        )

        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          true
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))

        val expected = expectedValues[0].optional

        assertSoftly {
          actual4 shouldBe expected
        }
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
        every { clonedConfig.ussPaths.remove(any() as UssPath) } answers {
          false
        }

        filesWorkingSetImpl1.removeUssPath(UssPath("/u/uss_path_to_remove"))
        val actual5 = clonedConfig.optional
        val expected = expectedValues[1].optional

        assertSoftly {
          actual5 shouldBe expected
        }
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

        assertSoftly {
          actual6 shouldBe expected
        }
      }
    }
  }

  context("explorer module: ui/ExplorerPasteProvider") {
    // performPaste
    should("perform paste without conflicts") {}
    should("perform paste accepting conflicts") {}
    should("perform paste declining conflicts") {}
  }
  context("explorer module: actions/RenameAction") {
    // actionPerformed
    should("perform rename on dataset") {}
    should("perform rename on dataset member") {}
    should("perform rename on USS file") {}
    should("perform rename on USS directory") {}
  }
  context("explorer module: actions/PurgeJobAction") {
    // actionPerformed
    should("perform purge on job successfully") {}
    should("perform purge on job with error") {}
  }
  context("explorer module: actions/GetJobPropertiesAction") {
    // actionPerformed
    should("get job properties") {}
    should("get spool file properties") {}
  }
  context("explorer module: actions/GetFilePropertiesAction") {
    // actionPerformed
    should("get dataset properties") {}
    should("get dataset member properties") {}
    should("get USS file properties") {}
  }
  context("explorer module: actions/ForceRenameAction") {
    // actionPerformed
    should("perform force rename on USS file") {}
    should("not rename dataset") {}
  }
  context("explorer module: actions/ChangeContentModeAction") {
    // isSelected
    should("check if the 'Use binary mode' selected for a file") {}
    // setSelected
    should("select 'Use binary mode' for a file") {}
    should("unselect 'Use binary mode' for a file") {}
  }
  context("explorer module: actions/AllocateDatasetAction") {
    // actionPerformed
    should("perform dataset allocate with default parameters") {}
  }
  context("explorer module: actions/AddMemberAction") {
    // actionPerformed
    should("perform dataset member allocate with default parameters") {}
  }
})
