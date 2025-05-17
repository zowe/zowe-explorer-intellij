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
 *   Uladzislau Kalesnikau
 *   Dzianis Lisiankou
 */

package org.zowe.explorer.dataops

import com.intellij.openapi.components.ComponentManager
import com.intellij.util.messages.Topic
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.fetch.DatasetFileFetchProvider
import org.zowe.explorer.dataops.fetch.FileCacheListener
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.utils.sendTopic
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.junit.platform.commons.util.ReflectionUtils
import org.zowe.explorer.testutils.AppInitShouldSpec
import java.time.LocalDateTime
import kotlin.reflect.KFunction

class RemoteFileFetchProviderBaseTestSpec : AppInitShouldSpec("dataops/RemoteFileFetchProviderBase", {
  context("refresh cache test spec") {
    val dataOpsManagerService = DataOpsManager.getService()
    every { dataOpsManagerService.componentManager } returns mockk()
    val classUnderTest = DatasetFileFetchProvider(dataOpsManagerService)

    val queryMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
    val queryOtherMock = mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>()
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
        val actualRefreshCacheMap =
          mutableMapOf<RemoteQuery<ConnectionConfig, DSMask, Unit>, LocalDateTime>()
        val expectedRefreshCacheMap = mutableMapOf(Pair(queryMock, lastRefreshDate))
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        //when
        classUnderTest.applyRefreshCacheDate(queryMock, lastRefreshDate)

        //then
        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
        }
      }

      should("should not add new entry if entry already present") {
        //given
        val actualRefreshCacheMap = mutableMapOf(Pair(queryMock, lastRefreshDate))
        val expectedRefreshCacheMap = mutableMapOf(Pair(queryMock, lastRefreshDate))
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        //when
        classUnderTest.applyRefreshCacheDate(queryMock, lastRefreshDate)

        //then
        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
        }
      }
    }
    context("findCacheRefreshDateIfPresent") {
      should("should find the last refreshDate for the node for the given query") {
        //given
        val refreshCacheMapForTest = mutableMapOf(
          Pair(queryMock, lastRefreshDate),
          Pair(queryOtherMock, lastRefreshDateOther)
        )
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
        val refreshCacheMapForTest = mutableMapOf(
          Pair(queryMock, lastRefreshDate),
          Pair(queryOtherMock, lastRefreshDateOther)
        )
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
    context("clean cache") {
      should("clean cache state and refresh cache of the fetch provider and no topic sent") {
        val actualRefreshCacheMap = mutableMapOf(Pair(queryMock, lastRefreshDate))
        val expectedRefreshCacheMap = mutableMapOf<RemoteQuery<ConnectionConfig, DSMask, Unit>, LocalDateTime>()
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        classUnderTest.cleanCache(queryMock, false)

        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
        }
      }
      should("clean cache state and refresh cache of the fetch provider and send topic") {
        var topicSent = false
        val actualRefreshCacheMap = mutableMapOf(Pair(queryMock, lastRefreshDate))
        val expectedRefreshCacheMap = mutableMapOf<RemoteQuery<ConnectionConfig, DSMask, Unit>, LocalDateTime>()
        refreshCacheStateField.set(classUnderTest, actualRefreshCacheMap)

        val mockSendTopic : (Topic<FileCacheListener>, ComponentManager) -> FileCacheListener = ::sendTopic
        mockkStatic(mockSendTopic as KFunction<*>)
        every {
          sendTopic(FileFetchProvider.CACHE_CHANGES, any<ComponentManager>())
        } answers {
          val fileCacheListenerMock = mockk<FileCacheListener>()
          every {
            fileCacheListenerMock.onCacheCleaned(queryMock)
          } answers {
            topicSent = true
          }
          fileCacheListenerMock
        }

        classUnderTest.cleanCache(queryMock, true)

        assertSoftly {
          actualRefreshCacheMap shouldContainExactly expectedRefreshCacheMap
          topicSent shouldBe true
        }
      }
    }
  }
})
