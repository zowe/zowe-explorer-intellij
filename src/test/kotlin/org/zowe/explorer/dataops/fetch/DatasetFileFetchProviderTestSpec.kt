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
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.zowe.explorer.api.ZosmfApi
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.ws.DSMask
import org.zowe.explorer.dataops.attributes.*
import org.zowe.explorer.vfs.MFVirtualFile
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.dataops.BatchedRemoteQuery
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.RemoteQuery
import org.zowe.explorer.dataops.UnitRemoteQueryImpl
import org.zowe.explorer.dataops.exceptions.CallException
import org.zowe.explorer.dataops.services.ErrorSeparatorService
import org.zowe.explorer.telemetry.NotificationCompatibleException
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.testutils.setPrivateFieldValue
import org.zowe.explorer.utils.castOrNull
import org.zowe.explorer.utils.sendTopic
import org.zowe.kotlinsdk.*
import retrofit2.Response
import java.io.StringReader
import java.security.cert.CertificateException
import java.util.Properties
import kotlin.reflect.KFunction

class DatasetFileFetchProviderTestSpec : AppInitShouldSpec("dataops/fetch/DatasetFileFetchProvider", {
  context("all functions") {
    var didShowArciveWarningNotification = false
    var didShowOtherWarningNotification = false

    val credentialService = CredentialService.getService()
    every { credentialService.getUsernameByKey(any<String>()) } returns "test"
    every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()

    val mockedConnectionConfig = mockk<ConnectionConfig> {
      every { uuid } returns "test_uuid"
      every { url } returns "test_url"
    }
    val mockedQuery = mockk<UnitRemoteQueryImpl<ConnectionConfig, DSMask>> {
      every { request } returns mockk {
        every { mask } returns "TESTMASK"
        every { volser } returns "TESTVOL"
      }
      every { connectionConfig } returns mockedConnectionConfig
    }
    val dataSetsList = mockk<DataSetsList> {
      every { items } returns mutableListOf(
        mockk<Dataset> {
          every { name } returns "DATASET1"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PS
        },
        mockk<Dataset> {
          every { name } returns "DATASET2"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PS
        },
        mockk<Dataset> {
          every { name } returns "DATASET3"
          every { migrated } returns HasMigrated.NO
          every { datasetOrganization } returns DatasetOrganization.PO
        }
      )
      every { totalRows } returns 3
    }
    val mockedResponse = mockk<Response<DataSetsList>> {
      every { body() } returns dataSetsList
      every { isSuccessful } returns true
    }

    val mockedApi = mockk<DataAPI>()

    val zosmfApi = ZosmfApi.getService()

    val mockedAttributesService = mockk<RemoteDatasetAttributesService>()
    val dataOpsManagerService = DataOpsManager.getService()
    every {
      dataOpsManagerService.getAttributesService(RemoteDatasetAttributes::class.java, MFVirtualFile::class.java)
    } returns mockedAttributesService
    every {
      dataOpsManagerService.componentManager
    } returns mockk()

    val sendTopicRef: (Topic<AttributesListener>, Project) -> AttributesListener = ::sendTopic
    mockkStatic(sendTopicRef as KFunction<*>)

    val datasetFileFetchProviderForTest =
      spyk(DatasetFileFetchProvider(dataOpsManagerService), recordPrivateCalls = true)

    val notificationsService = NotificationsService.getService()
    every {
      notificationsService.notifyWarning(any<Project>(), any<String>(), any<String>(), any<String>())
    } answers {
      if ((args[3] as String).contains("Plug-in is capable of processing datasets on this volume")) {
        didShowArciveWarningNotification = true
      } else {
        didShowOtherWarningNotification = true
      }
    }

    val errorSeparatorService = ErrorSeparatorService.getService()
    every {
      errorSeparatorService.separateErrorMessage(any())
    } answers {
      val properties = Properties()
      properties["error.description"] = "test"
      properties
    }

    beforeEach {
      didShowArciveWarningNotification = false
      didShowOtherWarningNotification = false

      every {
        mockedApi.listDataSets(
          authorizationToken = any<String>(),
          dsLevel = any<String>(),
          volser = any<String>(),
          xIBMAttr = any<XIBMAttr>(),
          xIBMMaxItems = any<Int>(),
          start = any<String>()
        )
      } returns mockk {
        every { execute() } returns mockedResponse
      }

      every { zosmfApi.getApi(DataAPI::class.java, any<ConnectionConfig>()) } returns mockedApi

      every { mockedAttributesService.getAttributes(any<MFVirtualFile>()) } returns mockk()
      every { mockedAttributesService.clearAttributes(any<MFVirtualFile>()) } returns Unit
      every {
        mockedAttributesService
          .updateAttributes(any<MFVirtualFile>(), any<RemoteDatasetAttributes.() -> Unit>())
      } returns Unit
      every { mockedAttributesService.getOrCreateVirtualFile(any()) } returns mockk()
    }

    context("fetchResponse") {
      val progressMockk = mockk<ProgressIndicator> {
        every { fraction = any<Double>() } just Runs
      }

      should("fetch dataset attributes") {
        val fetchResponseMethodRef = DatasetFileFetchProvider::class.java
          .getDeclaredMethod("fetchResponse", RemoteQuery::class.java, ProgressIndicator::class.java)
        fetchResponseMethodRef.isAccessible = true

        val datasetAttributes = fetchResponseMethodRef
          .invoke(datasetFileFetchProviderForTest, mockedQuery, progressMockk) as Collection<*>

        assertSoftly { datasetAttributes shouldHaveSize dataSetsList.items.size }
      }
    }

    context("fetchBatch") {
      val progressIndicatorMock = mockk<ProgressIndicator>()

      should("return datasets for the provided request") {
        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)
        val result = datasetFileFetchProviderForTest.convertResponseToBody(response.body()).items

        assertSoftly { result?.size shouldBe 3 }
      }

      should("return the basic info for migrated datasets for the provided request when some dataset is on the ARCIVE volume") {
        var requestNumber = 0

        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          requestNumber++
          when (requestNumber) {
            1 -> mockk {
              every { execute() } returns mockk {
                every { isSuccessful } returns false
                every { code() } returns 500
                every { errorBody() } returns mockk {
                  every { contentType() } returns null
                  every { string() } returns """{
                    "category": 2,
                    "details": [
                        "DMS2987 DATA SET CATALOGED TO CA DISK PSEUDO-VOLUME ARCIVE                                                              ",
                        "DMS2987 CA DISK HAS ARCHIVED TEST.ARCHIVED                                                                              ",
                        "   ",
                        "DMS2971 DO YOU WANT TO RESTORE THE DATA SET? (Y/N)"
                    ],
                    "message": "ServletDispatcher failed - received TSO Prompt when expecting TsoServletResponse",
                    "rc": 4,
                    "reason": 3
                  }""".trimIndent()
                }
              }
            }
            2 -> mockk {
              every { execute() } returns mockk<Response<DataSetsList>> {
                every { body() } returns mockk<DataSetsList> {
                  every { items } returns mutableListOf(
                    mockk<Dataset> {
                      every { name } returns "DATASET1"
                      every { migrated } returns HasMigrated.YES
                      every { datasetOrganization } returns DatasetOrganization.PS
                    },
                    mockk<Dataset> {
                      every { name } returns "DATASET2"
                      every { migrated } returns HasMigrated.NO
                      every { datasetOrganization } returns DatasetOrganization.PS
                    },
                    mockk<Dataset> {
                      every { name } returns "DATASET3"
                      every { migrated } returns HasMigrated.NO
                      every { datasetOrganization } returns DatasetOrganization.PO
                    }
                  )
                  every { totalRows } returns 3
                }
                every { isSuccessful } returns true
              }
            }
            else -> fail("Unexpected list datasets request")
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)
        val result = datasetFileFetchProviderForTest.convertResponseToBody(response.body()).items

        assertSoftly {
          response.isSuccessful shouldBe true
          result?.size shouldBe 3
          didShowArciveWarningNotification shouldBe true
          didShowOtherWarningNotification shouldBe false
        }
      }

      should("return the basic info for migrated datasets for the provided request when some dataset is on another migration volume") {
        var requestNumber = 0

        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          requestNumber++
          when (requestNumber) {
            1 -> mockk {
              every { execute() } returns mockk {
                every { isSuccessful } returns false
                every { code() } returns 500
                every { errorBody() } returns mockk {
                  every { contentType() } returns null
                  every { string() } returns """{
                    "category": 2,
                    "details": [
                        "DMS2987 DATA SET CATALOGED TO CA DISK PSEUDO-VOLUME SMMIG                                                               ",
                        "DMS2987 CA DISK HAS ARCHIVED TEST.ARCHIVED                                                                              ",
                        "   ",
                        "DMS2971 DO YOU WANT TO RESTORE THE DATA SET? (Y/N)"
                    ],
                    "message": "ServletDispatcher failed - received TSO Prompt when expecting TsoServletResponse",
                    "rc": 4,
                    "reason": 3
                  }""".trimIndent()
                }
              }
            }
            2 -> mockk {
              every { execute() } returns mockk<Response<DataSetsList>> {
                every { body() } returns mockk<DataSetsList> {
                  every { items } returns mutableListOf(
                    mockk<Dataset> {
                      every { name } returns "DATASET1"
                      every { migrated } returns HasMigrated.YES
                      every { datasetOrganization } returns DatasetOrganization.PS
                    },
                    mockk<Dataset> {
                      every { name } returns "DATASET2"
                      every { migrated } returns HasMigrated.NO
                      every { datasetOrganization } returns DatasetOrganization.PS
                    },
                    mockk<Dataset> {
                      every { name } returns "DATASET3"
                      every { migrated } returns HasMigrated.NO
                      every { datasetOrganization } returns DatasetOrganization.PO
                    }
                  )
                  every { totalRows } returns 3
                }
                every { isSuccessful } returns true
              }
            }
            else -> fail("Unexpected list datasets request")
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)
        val result = datasetFileFetchProviderForTest.convertResponseToBody(response.body()).items

        assertSoftly {
          response.isSuccessful shouldBe true
          result?.size shouldBe 3
          didShowArciveWarningNotification shouldBe false
          didShowOtherWarningNotification shouldBe true
        }
      }

      should("return the basic info for migrated datasets for the provided request when some dataset's properties could not be fetched") {
        var requestNumber = 0

        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          requestNumber++
          when (requestNumber) {
            1 -> mockk {
              every { execute() } returns mockk {
                every { isSuccessful } returns false
                every { code() } returns 500
                every { errorBody() } returns mockk {
                  every { contentType() } returns null
                  every { string() } returns """{
                    "category": 2,
                    "details": [
                        "DMS2987 DATA SET CATALOGED TO CA DISK PSEUDO-VOLUME                                                                     ",
                        "DMS2987 CA DISK HAS ARCHIVED TEST.ARCHIVED                                                                              ",
                        "   ",
                        "DMS2971 DO YOU WANT TO RESTORE THE DATA SET? (Y/N)"
                    ],
                    "message": "ServletDispatcher failed - received TSO Prompt when expecting TsoServletResponse",
                    "rc": 4,
                    "reason": 3
                  }""".trimIndent()
                }
              }
            }
            2 -> mockk {
              every { execute() } returns mockk<Response<DataSetsList>> {
                every { body() } returns mockk<DataSetsList> {
                  every { items } returns mutableListOf(
                    mockk<Dataset> {
                      every { name } returns "DATASET1"
                      every { migrated } returns HasMigrated.YES
                      every { datasetOrganization } returns DatasetOrganization.PS
                    }
                  )
                  every { totalRows } returns 1
                }
                every { isSuccessful } returns true
              }
            }
            else -> fail("Unexpected list datasets request")
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)
        val result = datasetFileFetchProviderForTest.convertResponseToBody(response.body()).items

        assertSoftly {
          response.isSuccessful shouldBe true
          result?.size shouldBe 1
          didShowArciveWarningNotification shouldBe false
          didShowOtherWarningNotification shouldBe true
        }
      }

      should("return response error when it is impossible to fetch a datasets list due to a ServletDispatcher failure") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } returns mockk {
          every { execute() } returns mockk {
            every { isSuccessful } returns false
            every { code() } returns 500
            every { errorBody() } returns mockk {
              every { contentType() } returns null
              every { string() } returns """{
                "category": 2,
                "details": ["SOME MYSTICAL ERROR APPEARED DURING THE REQUEST EXECUTION"],
                "message": "ServletDispatcher failed - received TSO Prompt when expecting TsoServletResponse",
                "rc": 4,
                "reason": 3
              }""".trimIndent()
            }
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)

        val responseErrorBody = response.errorBody()?.string() ?: ""
        assertSoftly {
          response.isSuccessful shouldBe false
          response.code() shouldBe 500
          responseErrorBody shouldContain "ServletDispatcher failed"
          responseErrorBody shouldContain "SOME MYSTICAL ERROR"
        }
      }

      should("return response error when it is impossible to fetch a datasets list due to some external failure") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } returns mockk {
          every { execute() } returns mockk {
            every { isSuccessful } returns false
            every { code() } returns 500
            every { errorBody() } returns mockk {
              every { contentType() } returns null
              every {
                string()
              } returns "{\"category\": 2, \"details\": [], \"message\": \"Test error\", \"rc\": 4, \"reason\": 3 }"
            }
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, null, null)

        assertSoftly {
          response.isSuccessful shouldBe false
          response.code() shouldBe 500
          (response.errorBody()?.string() ?: "") shouldContain "Test error"
        }
      }

      should("return error response as there is an empty error response received from a server") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } returns mockk {
          every { execute() } returns mockk {
            every { isSuccessful } returns false
            every { code() } returns 500
            every { errorBody() } returns mockk {
              every { contentType() } returns null
              every { string() } returns ""
            }
          }
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(mockedQuery, progressIndicatorMock, "TEST", null)

        assertSoftly {
          response.isSuccessful shouldBe false
          response.code() shouldBe 500
          (response.errorBody()?.string() ?: "") shouldBe ""
        }
      }

      should("return error response as there is an error response received from a server") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } returns mockk {
          every { execute() } returns mockk {
            every { isSuccessful } returns false
            every { code() } returns 404
          }
        }
        val query = mockk<BatchedRemoteQuery<DSMask>> {
          every { request } returns mockk {
            every { mask } returns "TESTMASK"
            every { volser } returns "TESTVOL"
          }
          every { connectionConfig } returns mockedConnectionConfig
        }

        val response = datasetFileFetchProviderForTest
          .fetchBatch(query, progressIndicatorMock, "TEST", null)

        assertSoftly {
          response.isSuccessful shouldBe false
          response.code() shouldBe 404
        }
      }
    }

    context("cleanupUnusedFile") {
      var didClearAttributes = false
      var didUpdateAttributes = false
      var didDeleteFile = false

      val mockedVirtualFile = mockk<MFVirtualFile> {
        every {
          delete(any<DatasetFileFetchProvider>())
        } answers {
          didDeleteFile = true
        }
      }
      val mockedMaskedRequester = mockk<MaskedRequester> {
        every { connectionConfig } returns mockedConnectionConfig
      }
      val requestersMock = mutableListOf(mockedMaskedRequester)
      val mockedFileAttributes = mockk<RemoteDatasetAttributes> {
        every { requesters } returns requestersMock
      }


      val cleanupUnusedFileMethodRef = DatasetFileFetchProvider::class.java
        .getDeclaredMethod("cleanupUnusedFile", MFVirtualFile::class.java, RemoteQuery::class.java)
      cleanupUnusedFileMethodRef.isAccessible = true

      beforeEach {
        didDeleteFile = false
        didClearAttributes = false
        didUpdateAttributes = false

        every { mockedAttributesService.getAttributes(mockedVirtualFile) } returns mockedFileAttributes
        every {
          mockedAttributesService.clearAttributes(mockedVirtualFile)
        } answers {
          didClearAttributes = true
        }
        every {
          mockedAttributesService.updateAttributes(mockedVirtualFile, any<RemoteDatasetAttributes.() -> Unit>())
        } answers {
          didUpdateAttributes = true
          secondArg<RemoteDatasetAttributes.() -> Unit>().invoke(mockedFileAttributes)
        }
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file") {
        every { mockedMaskedRequester.queryVolser } returns "TESTVOL"

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          didUpdateAttributes shouldBe false
          didClearAttributes shouldBe true
          didDeleteFile shouldBe true
        }
      }

      should("cleanup unused file if connection config of the query is the same as for dataset file (different volume)") {
        every { mockedMaskedRequester.queryVolser } returns "ANOTHER"

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          didUpdateAttributes shouldBe true
          didClearAttributes shouldBe false
          didDeleteFile shouldBe false
        }
      }

      should("not cleanup unused file when there is no appropriate attributes for the file found") {
        every { mockedAttributesService.getAttributes(mockedVirtualFile) } returns null

        cleanupUnusedFileMethodRef.invoke(datasetFileFetchProviderForTest, mockedVirtualFile, mockedQuery)

        assertSoftly {
          didUpdateAttributes shouldBe false
          didClearAttributes shouldBe false
          didDeleteFile shouldBe false
        }
      }
    }

    context("fetchSingleElemAttributes") {
      var didCallListDatasets = false
      var didProvideVirtualFile = false
      var isCacheUpdatedEventTriggered = false
      var isFetchFailureTriggered = false
      var isFetchCancelledTriggered = false

      val batchedQueryMock = mockk<BatchedRemoteQuery<DSMask>> {
        every { request } returns mockk {
          every { mask } returns "TESTMASK"
          every { volser } returns "TESTVOL"
        }
        every { connectionConfig } returns mockedConnectionConfig
      }

      val fileCacheListenerMock = mockk<FileCacheListener>()
      every {
        sendTopic(FileFetchProvider.CACHE_CHANGES, any<ComponentManager>())
      } returns fileCacheListenerMock

      beforeEach {
        didCallListDatasets = false
        didProvideVirtualFile = false
        isCacheUpdatedEventTriggered = false
        isFetchFailureTriggered = false
        isFetchCancelledTriggered = false

        every {
          mockedAttributesService.getOrCreateVirtualFile(any())
        } answers {
          didProvideVirtualFile = true
          mockk()
        }
        every {
          fileCacheListenerMock.onCacheUpdated(any<BatchedRemoteQuery<DSMask>>(), any<List<MFVirtualFile>>())
        } answers {
          isCacheUpdatedEventTriggered = true
        }
        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<DSMask>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
        }
        every {
          fileCacheListenerMock.onFetchCancelled(any<BatchedRemoteQuery<DSMask>>())
        } answers {
          isFetchCancelledTriggered = true
        }
      }

      should("fetch single element attributes, refreshing cache with newly fetched values") {
        val dataSetsList = mockk<DataSetsList> {
          every { items } returns mutableListOf(
            mockk<Dataset> {
              every { name } returns "DATASET1"
              every { migrated } returns HasMigrated.NO
              every { datasetOrganization } returns DatasetOrganization.PS
            }
          )
          every { totalRows } returns 1
        }
        val responseMock = mockk<Response<DataSetsList>> {
          every { body() } returns dataSetsList
          every { isSuccessful } returns true
        }
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          didCallListDatasets = true
          mockk { every { execute() } returns responseMock }
        }

        val vFileMock = mockk<MFVirtualFile> {
          every { path } returns "test"
        }
        val cachedFilesMock = listOf(vFileMock)
        setPrivateFieldValue(
          datasetFileFetchProviderForTest,
          "cache",
          mutableMapOf(mockk<RemoteQuery<ConnectionConfig, DSMask, Unit>>() to cachedFilesMock),
          RemoteFileFetchProviderBase::class.java
        )
        every {
          mockedAttributesService.getOrCreateVirtualFile(any())
        } answers {
          didProvideVirtualFile = true
          vFileMock
        }

        datasetFileFetchProviderForTest.fetchSingleElemAttributes(batchedQueryMock, mockk(), mockk())

        assertSoftly {
          didCallListDatasets shouldBe true
          didProvideVirtualFile shouldBe true
          isCacheUpdatedEventTriggered shouldBe true
          isFetchFailureTriggered shouldBe false
          isFetchCancelledTriggered shouldBe false
        }
      }

      should("throw an error during the fetch cause zero elements received") {
        var didProduceCorrectException = false

        val dataSetsList = mockk<DataSetsList> {
          every { items } returns mutableListOf()
          every { totalRows } returns 0
        }
        val responseMock = mockk<Response<DataSetsList>> {
          every { body() } returns dataSetsList
          every { isSuccessful } returns true
        }
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          didCallListDatasets = true
          mockk { every { execute() } returns responseMock }
        }

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<DSMask>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
          didProduceCorrectException = secondArg<Throwable>() is NotificationCompatibleException
        }

        datasetFileFetchProviderForTest.fetchSingleElemAttributes(batchedQueryMock, mockk(), mockk())

        assertSoftly {
          didCallListDatasets shouldBe true
          didProvideVirtualFile shouldBe false
          isCacheUpdatedEventTriggered shouldBe false
          isFetchFailureTriggered shouldBe true
          didProduceCorrectException shouldBe true
          isFetchCancelledTriggered shouldBe false
        }
      }

      should("throw an error during the fetch cause the call is failed due to internal server error") {
        var didProduceCorrectException = false

        val responseMock = mockk<Response<DataSetsList>> {
          every { code() } returns 500
          every { message() } returns "Test error"
          every { isSuccessful } returns false
          every { errorBody() } returns mockk {
            every { charStream() } returns StringReader("{\"details\": [\"Test details error\"]}")
          }
        }
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          didCallListDatasets = true
          mockk { every { execute() } returns responseMock }
        }

        every {
          fileCacheListenerMock.onFetchFailure(any<BatchedRemoteQuery<DSMask>>(), any<Throwable>())
        } answers {
          isFetchFailureTriggered = true
          val exception = secondArg<Throwable>().castOrNull<CallException>()
          if (exception != null) {
            didProduceCorrectException = exception.headMessage.contains("Cannot retrieve")
              && exception.message?.contains("Test details error") ?: false
          }
        }

        datasetFileFetchProviderForTest.fetchSingleElemAttributes(batchedQueryMock, mockk(), mockk())

        assertSoftly {
          didCallListDatasets shouldBe true
          didProvideVirtualFile shouldBe false
          isCacheUpdatedEventTriggered shouldBe false
          isFetchFailureTriggered shouldBe true
          didProduceCorrectException shouldBe true
          isFetchCancelledTriggered shouldBe false
        }
      }

      should("throw an error during the fetch cause the call is cancelled by user") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          didCallListDatasets = true
          mockk {
            every { execute() } throws ProcessCanceledException()
          }
        }

        datasetFileFetchProviderForTest.fetchSingleElemAttributes(batchedQueryMock, mockk(), mockk())

        assertSoftly {
          didCallListDatasets shouldBe true
          didProvideVirtualFile shouldBe false
          isCacheUpdatedEventTriggered shouldBe false
          isFetchFailureTriggered shouldBe false
          isFetchCancelledTriggered shouldBe true
        }
      }

      should("throw an error during the fetch due to a certificate exception") {
        every {
          mockedApi.listDataSets(
            authorizationToken = any<String>(),
            dsLevel = any<String>(),
            volser = any<String>(),
            xIBMAttr = any<XIBMAttr>(),
            xIBMMaxItems = any<Int>(),
            start = any<String>()
          )
        } answers {
          didCallListDatasets = true
          mockk {
            every { execute() } throws CertificateException("Test certificate exception\nOther lines")
          }
        }

        datasetFileFetchProviderForTest.fetchSingleElemAttributes(batchedQueryMock, mockk(), mockk())

        assertSoftly {
          didCallListDatasets shouldBe true
          didProvideVirtualFile shouldBe false
          isCacheUpdatedEventTriggered shouldBe false
          isFetchFailureTriggered shouldBe true
          isFetchCancelledTriggered shouldBe false
        }
      }
    }
  }
})
