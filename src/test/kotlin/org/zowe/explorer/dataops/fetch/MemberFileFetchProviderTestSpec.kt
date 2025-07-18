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
 *   Uladzislau Kalesnikau
 */

package org.zowe.explorer.dataops.fetch

import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.junit.jupiter.api.assertThrows
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.attributes.AttributesListener
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributes
import org.zowe.explorer.dataops.attributes.RemoteDatasetAttributesService
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributes
import org.zowe.explorer.dataops.attributes.RemoteMemberAttributesService
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.services.ErrorSeparatorService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.getPrivateFieldValue
import org.zowe.explorer.testutils.setPrivateFieldValue
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.runInEdtAndWait
import org.zowe.explorer.utils.sendTopic
import org.zowe.explorer.vfs.MFVirtualFile
import org.zowe.kotlinsdk.DataAPI
import org.zowe.kotlinsdk.MembersList
import org.zowe.kotlinsdk.XIBMAttr
import retrofit2.Response
import java.io.StringReader
import java.util.Properties
import kotlin.Int
import kotlin.reflect.KFunction

class MemberFileFetchProviderTestSpec : AppInitShouldSpec("dataops/fetch/MemberFileFetchProvider", {
  context("all functions") {
    var clearAttributesCallCount = 0
    var cacheUpdatedEventTriggerCount = 0

    val mockedDatasetAttributesService = mockk<RemoteDatasetAttributesService>()
    val mockedMemberAttributesService = mockk<RemoteMemberAttributesService>()
    every {
      mockedMemberAttributesService.clearAttributes(any<MFVirtualFile>())
    } answers {
      clearAttributesCallCount++
    }

    val mockedResponse = mockk<Response<MembersList>>()
    val mockedApi = mockk<DataAPI>()
    every {
      mockedApi.listDatasetMembers(
        authorizationToken = any<String>(),
        datasetName = any<String>(),
        xIBMAttr = any<XIBMAttr>(),
        xIBMMaxItems = any<Int>(),
        start = any<String>(),
        pattern = any<String>()
      )
    } returns mockk {
      every { execute() } returns mockedResponse
    }

    val zosmfApi = ZosmfApi.getService()
    every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns mockedApi

    val dataOpsManager = DataOpsManager.getService()
    every {
      dataOpsManager
        .getAttributesService(RemoteDatasetAttributes::class.java, MFVirtualFile::class.java)
    } returns mockedDatasetAttributesService
    every {
      dataOpsManager
        .getAttributesService(RemoteMemberAttributes::class.java, MFVirtualFile::class.java)
    } returns mockedMemberAttributesService
    every { dataOpsManager.componentManager } returns mockk()

    val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
    mockkStatic(sendTopicRef as KFunction<*>)

    val configService = ConfigService.getService()

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val errorSeparatorService = ErrorSeparatorService.getService()
    every {
      errorSeparatorService.separateErrorMessage(any())
    } answers {
      val properties = Properties()
      properties["error.description"] = "test"
      properties
    }

    val memberFileFetchProvider = MemberFileFetchProvider(dataOpsManager)

    beforeEach {
      clearAttributesCallCount = 0
      cacheUpdatedEventTriggerCount = 0

      every {
        mockedMemberAttributesService.getOrCreateVirtualFile(any())
      } answers {
        val attrs = firstArg<RemoteMemberAttributes>()
        mockk { every { path } returns attrs.name }
      }
      every {
        mockedDatasetAttributesService.getAttributes(any<MFVirtualFile>())
      } returns mockk { every { name } returns "TEST" }

      every { mockedResponse.isSuccessful } returns true
      every { mockedResponse.body() } returns mockk<MembersList> {
        every { items } returns mutableListOf(
          mockk { every { name } returns "MEM1" },
          mockk { every { name } returns "MEM2" },
          mockk { every { name } returns "MEM3" }
        )
        every { totalRows } returns 3
      }

      every { configService.batchSize } returns 3

      setPrivateFieldValue(
        memberFileFetchProvider,
        "cache",
        mutableMapOf<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>(),
        RemoteFileFetchProviderBase::class.java
      )
    }

    context("fetchBatch") {
      should("fetch full list of members in a dataset") {
        val queryMock = mockk<UnitRemoteQueryImpl<ConnectionConfig, LibraryQuery>> {
          every { request } returns mockk {
            every { library } returns mockk()
          }
          every { connectionConfig } returns mockk {
            every { uuid } returns "test"
          }
        }

        val response = memberFileFetchProvider
          .fetchBatch(queryMock, mockk(), null, null)
        val result = memberFileFetchProvider.convertResponseToBody(response.body()).items

        assertSoftly { result?.size shouldBe 3 }
      }

      should("fetch a batch of members in a dataset") {
        val queryMock = mockk<BatchedRemoteQuery<LibraryQuery>> {
          every { request } returns mockk {
            every { library } returns mockk()
          }
          every { connectionConfig } returns mockk {
            every { uuid } returns "test"
          }
        }

        val response = memberFileFetchProvider
          .fetchBatch(queryMock, mockk(), "TEST", null)
        val result = memberFileFetchProvider.convertResponseToBody(response.body()).items

        assertSoftly { result?.size shouldBe 3 }
      }

      should("not fetch a list of members cause there is no appropriate dataset attributes") {
        val queryMock = mockk<BatchedRemoteQuery<LibraryQuery>> {
          every { request } returns mockk {
            every { library } returns mockk()
          }
          every { connectionConfig } returns mockk {
            every { uuid } returns "test"
          }
        }

        every { mockedDatasetAttributesService.getAttributes(any<MFVirtualFile>()) } returns null

        assertThrows<IllegalArgumentException> {
          memberFileFetchProvider
            .fetchBatch(queryMock, mockk(), "TEST", null)
        }
      }
    }

    context("reload") {
      var isFetchFailureTriggered = false
      var isFetchCancelledTriggered = false
      var newStart: String? = null
      var newAlreadyFetched = 0
      var newFetchNeeded: Boolean? = null
      var newTotalRows: Int? = null

      val queryMock = mockk<BatchedRemoteQuery<LibraryQuery>>(relaxUnitFun = true) {
        every { connectionConfig } returns mockk { every { uuid } returns "test" }
        every { start } returns null
        every {
          start = any<String>()
        } answers {
          newStart = firstArg<String>()
        }
        every { pattern } returns null
        every { request } returns mockk {
          every { library } returns mockk {
            every { isReadable } returns true
          }
        }
        every { fetchNeeded } returns true
        every {
          fetchNeeded = any()
        } answers {
          newFetchNeeded = firstArg<Boolean>()
        }
        every {
          alreadyFetched
        } answers {
          newAlreadyFetched
        }
        every {
          alreadyFetched = any()
        } answers {
          newAlreadyFetched = firstArg<Int>()
        }
        every {
          totalRows
        } answers {
          newTotalRows
        }
        every {
          totalRows = any()
        } answers {
          newTotalRows = firstArg<Int>()
        }
      }

      val fileCacheListenerMock = mockk<FileCacheListener>()
      every {
        sendTopic(FileFetchProvider.CACHE_CHANGES, any<ComponentManager>())
      } returns fileCacheListenerMock
      every {
        fileCacheListenerMock.onCacheUpdated(any<BatchedRemoteQuery<LibraryQuery>>(), any<List<MFVirtualFile>>())
      } answers {
        cacheUpdatedEventTriggerCount++
      }
      every {
        fileCacheListenerMock.onFetchCancelled(any<BatchedRemoteQuery<LibraryQuery>>())
      } answers {
        isFetchCancelledTriggered = true
      }

      beforeEach {
        isFetchFailureTriggered = false
        isFetchCancelledTriggered = false
        newStart = null
        newAlreadyFetched = 0
        newFetchNeeded = null
        newTotalRows = null

        setPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          mutableMapOf<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>(),
          RemoteFileFetchProviderBase::class.java
        )

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<LibraryQuery>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
        }
      }

      should("fetch a fresh batch of dataset members") {
        every { configService.batchSize } returns 1

        every { mockedResponse.body() } returns mockk<MembersList> {
          every { items } returns mutableListOf(
            mockk { every { name } returns "MEM1" },
          )
          every { totalRows } returns 3
        }

        runInEdtAndWait {
          memberFileFetchProvider.reload(queryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 1
          newCache[queryMock]?.size shouldBe 1
          newStart shouldBe "MEM1"
          newAlreadyFetched shouldBe 1
          newFetchNeeded shouldBe true
          cacheUpdatedEventTriggerCount shouldBe 1
          isFetchFailureTriggered shouldBe false
          isFetchCancelledTriggered shouldBe false
          clearAttributesCallCount shouldBe 0
        }
      }

      should("fetch a full list of dataset members again") {
        every { configService.batchSize } returns 3

        val vFileAlreadyInCache1 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "TEST1"
        }
        val vFileAlreadyInCache2 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns false
          every { path } returns "TEST1"
        }
        val vFileAlreadyInCache3 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "TEST"
        }
        setPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          mutableMapOf(queryMock to listOf(vFileAlreadyInCache1, vFileAlreadyInCache2, vFileAlreadyInCache3)),
          RemoteFileFetchProviderBase::class.java
        )

        runInEdtAndWait {
          memberFileFetchProvider.reload(queryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 1
          newCache[queryMock]?.size shouldBe 3
          newStart shouldBe "MEM3"
          newAlreadyFetched shouldBe 3
          newFetchNeeded shouldBe false
          cacheUpdatedEventTriggerCount shouldBe 1
          isFetchFailureTriggered shouldBe false
          isFetchCancelledTriggered shouldBe false
          clearAttributesCallCount shouldBe 2
        }
      }

      should("process a failure response during members fetch") {
        var didProduceCorrectException = false

        every { mockedResponse.code() } returns 500
        every { mockedResponse.errorBody() } returns mockk {
          every { charStream() } returns StringReader("{\"details\": [\"Test details error\"]}")
        }
        every { mockedResponse.isSuccessful } returns false
        every { mockedResponse.message() } returns "Test exception"

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<LibraryQuery>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
          val exception = secondArg<Throwable>().castOrNull<CallException>()
          if (exception != null) {
            didProduceCorrectException = exception.headMessage.contains("Cannot retrieve")
              && exception.message?.contains("Test details error") ?: false
          }
        }

        runInEdtAndWait {
          memberFileFetchProvider.reload(queryMock, mockk(relaxUnitFun = true))
        }

        assertSoftly {
          cacheUpdatedEventTriggerCount shouldBe 0
          isFetchFailureTriggered shouldBe true
          isFetchCancelledTriggered shouldBe false
          didProduceCorrectException shouldBe true
        }
      }

      should("process an unauthorized failure response during members fetch") {
        var didProduceCorrectException = false

        every { mockedResponse.code() } returns 500
        every { mockedResponse.errorBody() } returns mockk {
          every { charStream() } returns StringReader("{\"details\": [\"Test details error\"]}")
        }
        every { mockedResponse.isSuccessful } returns false
        every { mockedResponse.message() } returns "Unauthorized"

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<LibraryQuery>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
          val exception = secondArg<Throwable>().castOrNull<CallException>()
          if (exception != null) {
            didProduceCorrectException = exception.headMessage.contains("Credentials are not valid")
              && exception.message?.contains("Test details error") ?: false
          }
        }

        runInEdtAndWait {
          memberFileFetchProvider.reload(queryMock, mockk(relaxUnitFun = true))
        }

        assertSoftly {
          cacheUpdatedEventTriggerCount shouldBe 0
          isFetchFailureTriggered shouldBe true
          isFetchCancelledTriggered shouldBe false
          didProduceCorrectException shouldBe true
        }
      }

      should("fetch a new batch of dataset members and refresh the same dataset members list in a different connection") {
        every { configService.batchSize } returns 2

        every { mockedResponse.body() } returns mockk<MembersList> {
          every { items } returns mutableListOf(
            mockk { every { name } returns "MEM1" },
            mockk { every { name } returns "MEM2" },
            mockk { every { name } returns "MEM3" },
          )
          every { totalRows } returns 3
        }

        val batchedQueryMock = spyk(
          BatchedRemoteQuery<LibraryQuery>(
            mockk { every { library } returns mockk { every { isReadable } returns true } },
            mockk { every { uuid } returns "test" },
            totalRows = 3,
            alreadyFetched = 1,
            start = "MEM1"
          )
        )
        val anotherQuery = spyk(
          BatchedRemoteQuery<LibraryQuery>(
            mockk { every { library } returns mockk { every { isReadable } returns true } },
            mockk { every { uuid } returns "test" },
          )
        )
        val vFileAlreadyInCache1 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM1"
        }
        val vFileAlreadyInCache2 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM2"
        }
        val vFileAlreadyInCache2New = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM2"
        }
        val vFileAlreadyInCache3 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM3"
        }

        every {
          mockedMemberAttributesService.getOrCreateVirtualFile(any())
        } answers {
          val attrs = firstArg<RemoteMemberAttributes>()
          when (attrs.name) {
            "MEM2" -> vFileAlreadyInCache2New
            "MEM3" -> vFileAlreadyInCache3
            else -> mockk { every { path } returns attrs.name }
          }
        }

        setPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          mutableMapOf(
            batchedQueryMock to listOf(vFileAlreadyInCache1),
            anotherQuery to listOf(vFileAlreadyInCache1, vFileAlreadyInCache2, vFileAlreadyInCache3)
          ),
          RemoteFileFetchProviderBase::class.java
        )

        runInEdtAndWait {
          memberFileFetchProvider.reload(batchedQueryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 2
          newCache[batchedQueryMock]?.size shouldBe 2
          newCache[batchedQueryMock]?.get(0) == vFileAlreadyInCache2New
          newCache[batchedQueryMock]?.get(1) == vFileAlreadyInCache3
          newCache[anotherQuery]?.size shouldBe 3
          newCache[anotherQuery]?.get(0) == vFileAlreadyInCache1
          newCache[anotherQuery]?.get(1) == vFileAlreadyInCache2New
          newCache[anotherQuery]?.get(2) == vFileAlreadyInCache3
          batchedQueryMock.alreadyFetched shouldBe 3
          batchedQueryMock.fetchNeeded shouldBe false
          batchedQueryMock.start shouldBe "MEM3"
          cacheUpdatedEventTriggerCount shouldBe 2
          isFetchFailureTriggered shouldBe false
          clearAttributesCallCount shouldBe 1
        }
      }
    }

    context("loadMore") {
      var isFetchFailureTriggered = false

      lateinit var queryMock: BatchedRemoteQuery<LibraryQuery>

      val fileCacheListenerMock = mockk<FileCacheListener>()
      every {
        sendTopic(FileFetchProvider.CACHE_CHANGES, any<ComponentManager>())
      } returns fileCacheListenerMock
      every {
        fileCacheListenerMock.onCacheUpdated(any<BatchedRemoteQuery<LibraryQuery>>(), any<List<MFVirtualFile>>())
      } answers {
        cacheUpdatedEventTriggerCount++
      }
      every {
        fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<LibraryQuery>>(), any())
      } answers {
        isFetchFailureTriggered = true
      }

      beforeEach {
        isFetchFailureTriggered = false

        queryMock = spyk(
        BatchedRemoteQuery(
          mockk { every { library } returns mockk { every { isReadable } returns true } },
          mockk { every { uuid } returns "test" },
          )
        )
      }

      should("fetch the next batch of dataset members and refresh the same dataset members list in a different connection") {
        queryMock.totalRows = 3
        queryMock.alreadyFetched = 1
        queryMock.start = "MEM1"

        every { configService.batchSize } returns 2

        every { mockedResponse.body() } returns mockk<MembersList> {
          every { items } returns mutableListOf(
            mockk { every { name } returns "MEM1" },
            mockk { every { name } returns "MEM2" },
            mockk { every { name } returns "MEM3" },
          )
          every { totalRows } returns 3
        }

        val anotherQuery = spyk(
          BatchedRemoteQuery<LibraryQuery>(
            mockk { every { library } returns mockk { every { isReadable } returns true } },
            mockk { every { uuid } returns "test" },
          )
        )
        val vFileAlreadyInCache1 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM1"
        }
        val vFileAlreadyInCache2 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM2"
        }
        val vFileAlreadyInCache2New = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM2"
        }
        val vFileAlreadyInCache3 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM3"
        }

        every {
          mockedMemberAttributesService.getOrCreateVirtualFile(any())
        } answers {
          val attrs = firstArg<RemoteMemberAttributes>()
          when (attrs.name) {
            "MEM2" -> vFileAlreadyInCache2New
            "MEM3" -> vFileAlreadyInCache3
            else -> mockk { every { path } returns attrs.name }
          }
        }

        setPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          mutableMapOf(
            queryMock to listOf(vFileAlreadyInCache1),
            anotherQuery to listOf(vFileAlreadyInCache1, vFileAlreadyInCache2, vFileAlreadyInCache3)
          ),
          RemoteFileFetchProviderBase::class.java
        )

        runInEdtAndWait {
          memberFileFetchProvider.loadMore(queryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 2
          newCache[queryMock]?.size shouldBe 3
          newCache[queryMock]?.get(0) == vFileAlreadyInCache1
          newCache[queryMock]?.get(1) == vFileAlreadyInCache2
          newCache[queryMock]?.get(2) == vFileAlreadyInCache3
          newCache[anotherQuery]?.size shouldBe 3
          newCache[anotherQuery]?.get(0) == vFileAlreadyInCache1
          newCache[anotherQuery]?.get(1) == vFileAlreadyInCache2New
          newCache[anotherQuery]?.get(2) == vFileAlreadyInCache3
          queryMock.alreadyFetched shouldBe 3
          queryMock.fetchNeeded shouldBe false
          queryMock.start shouldBe "MEM3"
          cacheUpdatedEventTriggerCount shouldBe 2
          isFetchFailureTriggered shouldBe false
          clearAttributesCallCount shouldBe 0
        }
      }

      should("fetch the next batch of dataset members, but zero new members retrieved") {
        queryMock.totalRows = 3
        queryMock.alreadyFetched = 1
        queryMock.start = "MEM1"

        every { configService.batchSize } returns 2

        every { mockedResponse.body() } returns mockk<MembersList> {
          every { items } returns mutableListOf()
          every { totalRows } returns 3
        }

        val anotherQuery = spyk(
          BatchedRemoteQuery<LibraryQuery>(
            mockk { every { library } returns mockk { every { isReadable } returns true } },
            mockk { every { uuid } returns "test" },
          )
        )
        val vFileAlreadyInCache1 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM1"
        }
        val vFileAlreadyInCache2 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM2"
        }
        val vFileAlreadyInCache3 = mockk<MFVirtualFile>(relaxUnitFun = true) {
          every { isValid } returns true
          every { path } returns "MEM3"
        }

        setPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          mutableMapOf(
            queryMock to listOf(vFileAlreadyInCache1),
            anotherQuery to listOf(vFileAlreadyInCache1, vFileAlreadyInCache2, vFileAlreadyInCache3)
          ),
          RemoteFileFetchProviderBase::class.java
        )

        runInEdtAndWait {
          memberFileFetchProvider.loadMore(queryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 2
          newCache[queryMock]?.size shouldBe 1
          newCache[queryMock]?.get(0) == vFileAlreadyInCache1
          newCache[anotherQuery]?.size shouldBe 3
          newCache[anotherQuery]?.get(0) == vFileAlreadyInCache1
          newCache[anotherQuery]?.get(1) == vFileAlreadyInCache2
          newCache[anotherQuery]?.get(2) == vFileAlreadyInCache3
          queryMock.alreadyFetched shouldBe 1
          queryMock.fetchNeeded shouldBe true
          queryMock.start shouldBe "MEM1"
          cacheUpdatedEventTriggerCount shouldBe 1
          isFetchFailureTriggered shouldBe false
          clearAttributesCallCount shouldBe 0
        }
      }

      should("not fetch the next batch of dataset members due to a server error") {
        var didProduceCorrectException = false

        every { mockedResponse.code() } returns 500
        every { mockedResponse.errorBody() } returns mockk {
          every { charStream() } returns StringReader("{\"details\": [\"Test details error\"]}")
        }
        every { mockedResponse.isSuccessful } returns false
        every { mockedResponse.message() } returns "Test exception"

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<LibraryQuery>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
          val exception = secondArg<Throwable>().castOrNull<CallException>()
          if (exception != null) {
            didProduceCorrectException = exception.headMessage.contains("Cannot retrieve")
              && exception.message?.contains("Test details error") ?: false
          }
        }

        runInEdtAndWait {
          memberFileFetchProvider.loadMore(queryMock, mockk(relaxUnitFun = true))
        }

        assertSoftly {
          cacheUpdatedEventTriggerCount shouldBe 0
          isFetchFailureTriggered shouldBe true
          didProduceCorrectException shouldBe true
        }
      }

      should("fetch the next batch of dataset members and initialize a cache that is empty for some reason") {
        queryMock.totalRows = 3
        queryMock.alreadyFetched = 1
        queryMock.start = "MEM1"

        every { configService.batchSize } returns 2

        every { mockedResponse.body() } returns mockk<MembersList> {
          every { items } returns mutableListOf(
            mockk { every { name } returns "MEM1" },
            mockk { every { name } returns "MEM2" },
            mockk { every { name } returns "MEM3" },
          )
          every { totalRows } returns 3
        }

        runInEdtAndWait {
          memberFileFetchProvider.loadMore(queryMock, mockk(relaxUnitFun = true))
        }

        val newCache = getPrivateFieldValue(
          memberFileFetchProvider,
          "cache",
          RemoteFileFetchProviderBase::class.java
        ) as Map<BatchedRemoteQuery<LibraryQuery>, List<MFVirtualFile>>

        assertSoftly {
          newCache.size shouldBe 1
          newCache[queryMock]?.size shouldBe 2
          queryMock.alreadyFetched shouldBe 3
          queryMock.fetchNeeded shouldBe false
          queryMock.start shouldBe "MEM3"
          cacheUpdatedEventTriggerCount shouldBe 1
          isFetchFailureTriggered shouldBe false
          clearAttributesCallCount shouldBe 0
        }
      }
    }
  }
})
