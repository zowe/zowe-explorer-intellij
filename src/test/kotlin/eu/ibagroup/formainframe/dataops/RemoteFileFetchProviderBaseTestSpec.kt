/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops

import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import eu.ibagroup.formainframe.config.connect.ConnectionConfig
import eu.ibagroup.formainframe.config.ws.DSMask
import eu.ibagroup.formainframe.dataops.fetch.DatasetFileFetchProvider
import eu.ibagroup.formainframe.testutils.WithApplicationShouldSpec
import eu.ibagroup.formainframe.testutils.testServiceImpl.TestDataOpsManagerImpl
import eu.ibagroup.formainframe.utils.service
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.platform.commons.util.ReflectionUtils
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.concurrent.locks.ReentrantLock

class RemoteFileFetchProviderBaseTestSpec: WithApplicationShouldSpec({

  afterSpec {
    clearAllMocks()
  }

  context("refresh cache test spec") {

    val dataOpsManagerService =
      ApplicationManager.getApplication().service<DataOpsManager>() as TestDataOpsManagerImpl
    val classUnderTest = DatasetFileFetchProvider(dataOpsManagerService)

    val queryMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
    val nodeMock = mockk<AbstractTreeNode<*>>()
    val queryOtherMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
    val nodeOtherMock = mockk<AbstractTreeNode<*>>()
    val lastRefreshDate = LocalDateTime.now()
    val lastRefreshDateOther = LocalDateTime.of(2023, 12, 30, 10, 0, 0)

    val refreshCacheStateField = ReflectionUtils
      .findFields(
        DatasetFileFetchProvider::class.java, { f -> f.name.equals("refreshCacheState") },
        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN
      )[0]
    refreshCacheStateField.isAccessible = true

    context("applyRefreshCacheDate") {
      should("should add new entry with given node and query and last refreshDate") {
        //given
        val actualRefreshCacheMap = mutableMapOf<Pair<AbstractTreeNode<*>, RemoteQuery<ConnectionConfig, DSMask, Unit>>, LocalDateTime>()
        val expectedRefreshCacheMap = mutableMapOf(Pair(Pair(nodeMock,queryMock), lastRefreshDate))
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        //when
        classUnderTest.applyRefreshCacheDate(queryMock, nodeMock, lastRefreshDate)

        //then
        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
        }
      }

      should("should not add new entry if entry already present") {
        //given
        val actualRefreshCacheMap = mutableMapOf(Pair(Pair(nodeMock,queryMock), lastRefreshDate))
        val expectedRefreshCacheMap = mutableMapOf(Pair(Pair(nodeMock,queryMock), lastRefreshDate))
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        //when
        classUnderTest.applyRefreshCacheDate(queryMock, nodeMock, lastRefreshDate)

        //then
        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
        }
      }
    }
    context("findCacheRefreshDateIfPresent") {
      should("should find the last refreshDate for the node for the given query") {
        //given
        val refreshCacheMapForTest = mutableMapOf(Pair(Pair(nodeMock,queryMock), lastRefreshDate), Pair(Pair(nodeOtherMock,queryOtherMock), lastRefreshDateOther))
        refreshCacheStateField.set(classUnderTest, refreshCacheMapForTest)

        //when
        val actualRefreshDate = classUnderTest.findCacheRefreshDateIfPresent(queryMock)

        //then
        assertSoftly {
          actualRefreshDate shouldBe lastRefreshDate
        }
      }
      should("should not find the last refreshDate and return null for the node for the given query") {
        //given
        val refreshCacheMapForTest = mutableMapOf(Pair(Pair(nodeMock,queryMock), lastRefreshDate), Pair(Pair(nodeOtherMock,queryOtherMock), lastRefreshDateOther))
        refreshCacheStateField.set(classUnderTest, refreshCacheMapForTest)
        val queryMockForTest = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()

        //when
        val actualRefreshDate = classUnderTest.findCacheRefreshDateIfPresent(queryMockForTest)

        //then
        assertSoftly {
          actualRefreshDate shouldBe null
        }
      }
    }
    unmockkAll()
  }
})