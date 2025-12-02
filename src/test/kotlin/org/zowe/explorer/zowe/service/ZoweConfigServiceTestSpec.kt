/*
 * Copyright (c) 2025 IBA Group.
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
 *   Katsiaryna Tsytsenia
 */

package org.zowe.explorer.zowe.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.UsefulTestCase.assertThrows
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.zowe.explorer.config.ConfigService
import org.zowe.explorer.config.connect.ConnectionConfig
import org.zowe.explorer.config.connect.CredentialService
import org.zowe.explorer.config.connect.ui.zosmf.ConnectionDialogState
import org.zowe.explorer.config.connect.whoAmI
import org.zowe.explorer.config.ws.FilesWorkingSetConfig
import org.zowe.explorer.config.ws.JesWorkingSetConfig
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.operations.InfoOperation
import org.zowe.explorer.dataops.operations.ZOSInfoOperation
import org.zowe.explorer.telemetry.NotificationsService
import org.zowe.explorer.testutils.AppInitShouldSpec
import org.zowe.explorer.utils.crudable.Crudable
import org.zowe.explorer.utils.getResourceAsStreamWrappable
import org.zowe.explorer.utils.optional
import org.zowe.explorer.zowe.ZOWE_CONFIG_NAME
import org.zowe.kotlinsdk.InfoResponse
import org.zowe.kotlinsdk.SystemsResponse
import org.zowe.kotlinsdk.annotations.ZVersion
import org.zowe.kotlinsdk.exceptions.EmptyZoweConfigFileException
import org.zowe.kotlinsdk.zowe.config.KeytarWrapper
import org.zowe.kotlinsdk.zowe.config.ZoweConfig
import org.zowe.kotlinsdk.zowe.config.parseConfigJson
import java.io.InputStream
import java.nio.file.Path
import java.util.*
import java.util.function.Predicate
import javax.swing.Icon
import kotlin.reflect.KFunction

class ZoweConfigServiceTestSpec : AppInitShouldSpec("zowe/service/ZoweConfigService", {
  context("all functions") {
    var errorNotificationTrigerredCount = 0

    val projectMock = mockk<Project> {
      every { name } returns "test_project_name"
      every { basePath } returns "test/project/base/path"
      every { isDisposed } returns true
    }

    mockkStatic(::whoAmI as KFunction<*>)
    mockkStatic(VirtualFileManager::getInstance)

    val configServiceCrudableMock = mockk<Crudable>()
    val configService = ConfigService.getService()
    every { configService.crudable } returns configServiceCrudableMock

    val credentialService = CredentialService.getService()

    val notificationsService = NotificationsService.getService()

    val dataOpsManager = DataOpsManager.getService()

    beforeEach {
      errorNotificationTrigerredCount = 0

      every {
        configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
      } answers {
        emptyList<ConnectionConfig>().stream()
      }
      every { configServiceCrudableMock.addOrUpdate(any<ConnectionConfig>()) } returns Optional.ofNullable(null)

      every {
        notificationsService.notifyError(any<Throwable>(), any<Project>(), any<String>(), any<String>(), any<String>())
      } answers {
        errorNotificationTrigerredCount += 1
      }

      every { credentialService.getUsernameByKey(any<String>()) } returns "test"
      every { credentialService.getPasswordByKey(any<String>()) } returns "test".toCharArray()
      every { credentialService.setCredentials(any<String>(), any<String>(), any<CharArray>()) } returns Unit
      every { credentialService.clearCredentials(any<String>()) } returns Unit

      every { dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>()) } returns Unit

      every { whoAmI(any<ConnectionConfig>()) } returns "USERID"

      every {
        VirtualFileManager.getInstance()
      } answers {
        mockk {
          every {
            findFileByNioPath(any<Path>())
          } answers {
            mockk {
              every { inputStream } returns mockk {
                every { close() } answers {}
              }
              every { path } returns "test"
            }
          }
        }
      }
    }

    context("findAllZosmfExistingConnection") {
      should("find connection configs both for local and global Zowe Configs") {
        val localConnectionConfigMock = mockk<ConnectionConfig> {
          every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}"
          every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
        }
        val globalConnectionConfigMock = mockk<ConnectionConfig> {
          every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-zosmf"
          every {
            zoweConfigPath
          } returns System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
        }

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(
            mockk {
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}1"
              every { zoweConfigPath } returns "other/test/project/$ZOWE_CONFIG_NAME"
            },
            localConnectionConfigMock,
            globalConnectionConfigMock
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)
        val resultLocal = zoweConfigService.findAllZosmfExistingConnection(ZoweConfigType.LOCAL)
        val resultGlobal = zoweConfigService.findAllZosmfExistingConnection(ZoweConfigType.GLOBAL)

        assertSoftly { resultLocal.size shouldBe 1 }
        assertSoftly { resultLocal[0] shouldBe localConnectionConfigMock }
        assertSoftly { resultGlobal.size shouldBe 1 }
        assertSoftly { resultGlobal[0] shouldBe globalConnectionConfigMock }
      }

      should("return empty list if there is no connection configs found for the currently opened project") {
        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}1"
              every { zoweConfigPath } returns "other/test/project/$ZOWE_CONFIG_NAME"
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)
        val result = zoweConfigService.findAllZosmfExistingConnection(ZoweConfigType.LOCAL)

        assertSoftly { result.isEmpty() shouldBe true }
      }
    }

    context("addOrUpdateZoweConfig") {
      var setCredentialsCalledCount = 0
      var addOrUpdateCalledCount = 0
      var infoOperationCount = 0
      var zosInfoOperationCount = 0

      mockkStatic(NotificationGroupManager::getInstance)

      val parseConfigJsonRef: (InputStream) -> ZoweConfig = ::parseConfigJson
      mockkStatic(parseConfigJsonRef as KFunction<*>)

      beforeEach {
        setCredentialsCalledCount = 0
        addOrUpdateCalledCount = 0
        infoOperationCount = 0
        zosInfoOperationCount = 0

        every {
          configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
        } answers {
          emptyList<ConnectionConfig>().stream()
        }
        every {
          configServiceCrudableMock.addOrUpdate(any<ConnectionConfig>())
        } answers {
          addOrUpdateCalledCount += 1
          firstArg<ConnectionConfig>().optional
        }

        val testUsername = "TSTUSR"
        val testPassword = "TSTPWD"
        every { credentialService.getUsernameByKey(any<String>()) } returns testUsername
        every { credentialService.getPasswordByKey(any<String>()) } returns testPassword.toCharArray()
        every {
          credentialService.setCredentials(any<String>(), any<String>(), any<CharArray>())
        } answers {
          setCredentialsCalledCount += 1
        }
      }

      should("update an existing connection config for the local Zowe config") {
        val testProfileName = "test_profile"

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (firstArg<Operation<*>>()) {
            is InfoOperation -> {
              infoOperationCount += 1
              mockk<SystemsResponse>()
            }

            is ZOSInfoOperation -> {
              zosInfoOperationCount += 1
              mockk<InfoResponse> {
                every { zosVersion } returns "04.28.00"
              }
            }

            else -> {
              mockk<Any>()
            }
          }
        }

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-custom/${projectMock.name}"
            },
            mockk {
              every { uuid } returns "test_uuid"
              every { zVersion } returns ZVersion.ZOS_2_5
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testProfileName/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val localZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testProfileName
              every { basePath } returns "test/base/path"
              every { host } returns "test.com"
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)
        zoweConfigService.localZoweConfig = localZoweConfig

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = false, checkConnection = false, ZoweConfigType.LOCAL)

        assertSoftly { setCredentialsCalledCount shouldBe 1 }
        assertSoftly { infoOperationCount shouldBe 1 }
        assertSoftly { zosInfoOperationCount shouldBe 1 }
        assertSoftly { addOrUpdateCalledCount shouldBe 1 }
      }

      should("cancel testing Zowe config connections") {
        val testFailProfileName5 = "test_profile_name_fail5"
        var extractSecurePropertiesCalledCount = 0
        var cancelationCount = 0

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (val operation = firstArg<Operation<*>>()) {
            is InfoOperation -> {
              infoOperationCount += 1
              if (operation.connectionConfig.uuid == "throw") {
                cancelationCount += 1
                throw ProcessCanceledException()
              } else {
                mockk<SystemsResponse>()
              }
            }

            else -> {
              mockk<Any>()
            }
          }
        }

        val globalZoweConfig: ZoweConfig = mockk {
          every {
            extractSecureProperties(any<Array<String>>(), any<KeytarWrapper>())
          } answers {
            extractSecurePropertiesCalledCount += 1
          }
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName5
              every { basePath } returns "test/base/path/"
              every { host } returns "testFailHost5"
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns null
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        every { parseConfigJsonRef(any<InputStream>()) } returns globalZoweConfig

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { uuid } returns "throw"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-$testFailProfileName5"
              every { zoweConfigPath } returns System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = true, checkConnection = true, ZoweConfigType.GLOBAL)
        assertSoftly { cancelationCount shouldBe 1 }
        assertSoftly { setCredentialsCalledCount shouldBe 1 }
        assertSoftly { infoOperationCount shouldBe 1 }

      }

      should("add a new connection for the local Zowe config, scanning a project, with failed connections and their check") {
        val testSuccessProfileName = "test_profile_name_success"
        val testFailProfileName1 = "test_profile_name_fail1"
        val testFailProfileName2 = "test_profile_name_fail2"
        val testFailHost1 = "test1.com"
        val testFailHost2 = "test2.com"

        var extractSecurePropertiesCalledCount = 0
        var isCorrectConnectionErrorNotificationTrigerred = false

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (firstArg<Operation<*>>()) {
            is InfoOperation -> {
              infoOperationCount += 1
              if (infoOperationCount == 1) {
                throw Exception()
              } else {
                mockk<SystemsResponse>()
              }
            }

            is ZOSInfoOperation -> {
              zosInfoOperationCount += 1
              if (zosInfoOperationCount == 1) {
                throw Exception()
              } else {
                mockk<InfoResponse> {
                  every { zosVersion } returns "04.29.00"
                }
              }
            }

            else -> {
              mockk<Any>()
            }
          }
        }

        every { NotificationGroupManager.getInstance() } returns mockk {
          every { getNotificationGroup(any<String>()) } returns mockk {
            every { createNotification(any<String>(), any<String>(), any<NotificationType>()) } answers {
              val title = firstArg<String>()
              val details = secondArg<String>()
              val notificationType = thirdArg<NotificationType>()

              mockk {
                every { addAction(any<AnAction>()) } returns mockk()
                every {
                  notify(any<Project>())
                } answers {
                  if (
                    notificationType == NotificationType.ERROR
                    && title.contains("Unsuccessfully tested profiles:")
                    && details.contains(testFailProfileName1)
                    && details.contains(testFailProfileName2)
                  ) {
                    isCorrectConnectionErrorNotificationTrigerred = true
                  }
                }
              }
            }
          }
        }

        val localZoweConfig: ZoweConfig = mockk {
          every {
            extractSecureProperties(any<Array<String>>(), any<KeytarWrapper>())
          } answers {
            extractSecurePropertiesCalledCount += 1
          }
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName1
              every { basePath } returns "test/base/path/"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns null
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName2
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost2
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testSuccessProfileName
              every { basePath } returns "test/base/path"
              every { host } returns "test3.com"
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }
        every { parseConfigJsonRef(any<InputStream>()) } returns localZoweConfig

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { uuid } returns "test_uuid_fail1"
              every { zVersion } returns ZVersion.ZOS_2_3
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName1/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { uuid } returns "test_uuid_fail2"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName2/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { name } returns "invalid-config-name"
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = true, checkConnection = true, ZoweConfigType.LOCAL)

        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
        assertSoftly { setCredentialsCalledCount shouldBe 3 }
        assertSoftly { extractSecurePropertiesCalledCount shouldBe 1 }
        assertSoftly { infoOperationCount shouldBe 3 }
        assertSoftly { zosInfoOperationCount shouldBe 2 }
        assertSoftly { isCorrectConnectionErrorNotificationTrigerred shouldBe true }
        assertSoftly { addOrUpdateCalledCount shouldBe 1 }
      }

      should("add a new connection for the global Zowe config without a check") {
        val testProfileName = "test_profile_name"

        every { whoAmI(any<ConnectionConfig>()) } returns null

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (firstArg<Operation<*>>()) {
            is InfoOperation -> {
              infoOperationCount += 1
              mockk<SystemsResponse>()
            }

            is ZOSInfoOperation -> {
              zosInfoOperationCount += 1
              mockk<InfoResponse> {
                every { zosVersion } returns "04.27.00"
              }
            }

            else -> {
              mockk<Any>()
            }
          }
        }

        val globalZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testProfileName
              every { basePath } returns "test/base/path"
              every { host } returns "test3.com"
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { uuid } returns "test_uuid"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testProfileName"
              every {
                zoweConfigPath
              } returns System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)
        zoweConfigService.globalZoweConfig = globalZoweConfig

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = false, checkConnection = false, ZoweConfigType.GLOBAL)

        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
        assertSoftly { setCredentialsCalledCount shouldBe 1 }
        assertSoftly { infoOperationCount shouldBe 1 }
        assertSoftly { zosInfoOperationCount shouldBe 1 }
        assertSoftly { addOrUpdateCalledCount shouldBe 1 }
      }

      should("try to add a new connection for the global Zowe config, scanning a project, with failed connections and their check") {
        val testSuccessProfileName = "test_profile_name_success"
        val testSuccessProfileName1 = "test_profile_name_success1"
        val testFailProfileName1 = "test_profile_name_fail1"
        val testFailProfileName2 = "test_profile_name_fail2"
        val testFailProfileName3 = "test_profile_name_fail3"
        val testFailProfileName4 = "test_profile_name_fail4"
        val testFailProfileName5 = "test_profile_name_fail5"
        val testFailProfileName6 = "test_profile_name_fail6"
        val testFailProfileName7 = "test_profile_name_fail7"
        val testFailProfileName8 = "test_profile_name_fail8"
        val testFailProfileName9 = "test_profile_name_fail9"
        val testFailProfileName10 = "test_profile_name_fail10"
        val testFailProfileName11 = "test_profile_name_fail11"
        val testFailHost1 = "test1.com"
        val testSuccessHost = "test3.com"

        var extractSecurePropertiesCalledCount = 0
        var isCorrectConnectionErrorNotificationTrigerred = false

        every {
          dataOpsManager.performOperation(any<Operation<Any>>(), any<ProgressIndicator>())
        } answers {
          when (firstArg<Operation<*>>()) {
            is InfoOperation -> {
              infoOperationCount += 1
              if (infoOperationCount <= 10) {
                throw Exception()
              } else {
                mockk<SystemsResponse>()
              }
            }

            is ZOSInfoOperation -> {
              zosInfoOperationCount += 1
              mockk<InfoResponse> {
                every { zosVersion } returns "04.27.00"
              }
            }

            else -> {
              mockk<Any>()
            }
          }
        }

        every { NotificationGroupManager.getInstance() } returns mockk {
          every { getNotificationGroup(any<String>()) } returns mockk {
            every { createNotification(any<String>(), any<String>(), any<NotificationType>()) } answers {
              val title = firstArg<String>()
              val details = secondArg<String>()
              val notificationType = thirdArg<NotificationType>()

              mockk {
                every { addAction(any<AnAction>()) } returns mockk()
                every {
                  notify(any<Project>())
                } answers {
                  if (
                    notificationType == NotificationType.ERROR
                    && title.contains("Unsuccessfully tested profiles:")
                    && details.contains(testFailProfileName1)
                    && details.contains(testFailProfileName2)
                    && details.contains(testFailProfileName3)
                    && details.contains(testFailProfileName4)
                    && details.contains("...")
                  ) {
                    isCorrectConnectionErrorNotificationTrigerred = true
                  }
                }
              }
            }
          }
        }

        val globalZoweConfig: ZoweConfig = mockk {
          every {
            extractSecureProperties(any<Array<String>>(), any<KeytarWrapper>())
          } answers {
            extractSecurePropertiesCalledCount += 1
          }
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName1
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR1"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName2
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD1"
              every { profileName } returns testFailProfileName3
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName4
              every { basePath } returns "test/base/path/"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName5
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "12345"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR1"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName6
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "http"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName7
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName8
              every { basePath } returns "test/base/path7"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns null
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName9
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1048
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName10
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 601
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testFailProfileName11
              every { basePath } returns "test/base/path"
              every { host } returns testFailHost1
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns false
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testSuccessProfileName
              every { basePath } returns "test/base/path"
              every { host } returns testSuccessHost
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            },
            mockk {
              every { user } returns "TSTUSR"
              every { password } returns "TSTPWD"
              every { profileName } returns testSuccessProfileName1
              every { basePath } returns "test/base/path"
              every { host } returns testSuccessHost
              every { zosmfPort } returns "1234"
              every { protocol } returns "https"
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }
        every { parseConfigJsonRef(any<InputStream>()) } returns globalZoweConfig

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>(
            mockk {
              every { uuid } returns "test_uuid_fail1"
              every { zVersion } returns ZVersion.ZOS_2_3
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName1/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { uuid } returns "test_uuid_fail2"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName2/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { uuid } returns "test_uuid_fail3"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName3/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { uuid } returns "test_uuid_fail4"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testFailProfileName4/${projectMock.name}"
              every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
            },
            mockk {
              every { uuid } returns "test_uuid_succ"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-${testSuccessProfileName}"
              every { zoweConfigPath } returns System.getProperty("user.home")
                .replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
            },
            mockk {
              every { uuid } returns "test_uuid_succ1"
              every { zVersion } returns ZVersion.ZOS_2_4
              every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.GLOBAL}-${testSuccessProfileName1}"
              every { zoweConfigPath } returns System.getProperty("user.home")
                .replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
            }
          )
            .filter(secondArg<Predicate<ConnectionConfig>>()::test)
            .stream()
        }

        every {
          configServiceCrudableMock.addOrUpdate(any<ConnectionConfig>())
        } answers {
          addOrUpdateCalledCount += 1
          Optional.empty<ConnectionConfig>()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = true, checkConnection = true, ZoweConfigType.GLOBAL)

        assertSoftly { setCredentialsCalledCount shouldBe 13 }
        assertSoftly { extractSecurePropertiesCalledCount shouldBe 1 }
        assertSoftly { infoOperationCount shouldBe 11 }
        assertSoftly { zosInfoOperationCount shouldBe 1 }
        assertSoftly { isCorrectConnectionErrorNotificationTrigerred shouldBe true }
        assertSoftly { addOrUpdateCalledCount shouldBe 2 }
      }

      should("produce an error notification cause the Zowe config file is not found") {
        every {
          VirtualFileManager.getInstance()
        } answers {
          mockk {
            every {
              findFileByNioPath(any<Path>())
            } returns null
          }
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = true, checkConnection = false, ZoweConfigType.LOCAL)

        assertSoftly { errorNotificationTrigerredCount shouldBe 1 }
      }

      should("produce an error notification during a Zowe config JSON parse") {
        every { parseConfigJsonRef(any<InputStream>()) } answers { throw Exception() }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService
          .addOrUpdateZoweConfig(scanProject = true, checkConnection = false, ZoweConfigType.LOCAL)

        assertSoftly { errorNotificationTrigerredCount shouldBe 2 }
      }
    }

    context("deleteZoweConfig") {
      val testUuid = "test_uuid"
      val localConnectionConfigMock = mockk<ConnectionConfig> {
        every { uuid } returns testUuid
        every { name } returns "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}"
        every { zoweConfigPath } returns "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
      }

      var didWarningMessageForDeleteAppear = false
      var didCorrectWarningMessageForDeleteAppear = false
      var isClearCredentialsCalled = false
      var didDeleteZoweConnection = false

      val showOkCancelDialogMock: (String, String, String, String, Icon?) -> Int = Messages::showOkCancelDialog
      mockkStatic(showOkCancelDialogMock as KFunction<*>)

      beforeEach {
        didWarningMessageForDeleteAppear = false
        didCorrectWarningMessageForDeleteAppear = false
        isClearCredentialsCalled = false
        didDeleteZoweConnection = false

        every {
          showOkCancelDialogMock(any<String>(), any<String>(), any<String>(), any<String>(), any<Icon>())
        } answers {
          didWarningMessageForDeleteAppear = true
          if (!firstArg<String>().contains("wrong")) {
            didCorrectWarningMessageForDeleteAppear = true
          }
          Messages.OK
        }

        every {
          credentialService.clearCredentials(any<String>())
        } answers {
          isClearCredentialsCalled = true
        }

        every {
          configServiceCrudableMock.delete(any<ConnectionConfig>())
        } answers {
          didDeleteZoweConnection = true
          Optional.empty()
        }
      }

      should("delete the Zowe connection together with the files and JES working sets that use the connection") {
        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfigMock).stream()
        }
        every {
          configServiceCrudableMock.getAll(any<Class<*>>())
        } answers {
          when ((invocation.args[0] as Class<*>).name) {
            FilesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<FilesWorkingSetConfig> {
                  every { name } returns "right_test_name"
                  every { connectionConfigUuid } returns testUuid
                },
                mockk<FilesWorkingSetConfig> {
                  every { name } returns "wrong_test_name"
                  every { connectionConfigUuid } returns "test_other_uuid"
                }
              ).stream()
            }
            JesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<JesWorkingSetConfig> {
                  every { name } returns "right_test_name"
                  every { connectionConfigUuid } returns testUuid
                },
                mockk<JesWorkingSetConfig> {
                  every { name } returns "wrong_test_name"
                  every { connectionConfigUuid } returns "test_other_uuid"
                }
              ).stream()
            }
            else -> {
              fail("Unrecognized getAll call")
            }
          }
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService.deleteZoweConfig(ZoweConfigType.LOCAL)

        assertSoftly { didWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { didCorrectWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { isClearCredentialsCalled shouldBe true }
        assertSoftly { didDeleteZoweConnection shouldBe true }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("delete the Zowe connection without files of JES working sets") {
        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfigMock).stream()
        }
        every {
          configServiceCrudableMock.getAll(any<Class<*>>())
        } answers {
          when ((invocation.args[0] as Class<*>).name) {
            FilesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<FilesWorkingSetConfig> {
                  every { name } returns "wrong_test_name"
                  every { connectionConfigUuid } returns "test_other_uuid"
                }
              ).stream()
            }
            JesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<JesWorkingSetConfig> {
                  every { name } returns "wrong_test_name"
                  every { connectionConfigUuid } returns "test_other_uuid"
                }
              ).stream()
            }
            else -> {
              fail("Unrecognized getAll call")
            }
          }
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService.deleteZoweConfig(ZoweConfigType.LOCAL)

        assertSoftly { didWarningMessageForDeleteAppear shouldBe false }
        assertSoftly { didCorrectWarningMessageForDeleteAppear shouldBe false }
        assertSoftly { isClearCredentialsCalled shouldBe true }
        assertSoftly { didDeleteZoweConnection shouldBe true }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("delete the Zowe connection with files working set only") {
        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfigMock).stream()
        }
        every {
          configServiceCrudableMock.getAll(any<Class<*>>())
        } answers {
          when ((invocation.args[0] as Class<*>).name) {
            FilesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<FilesWorkingSetConfig> {
                  every { name } returns "right_test_name"
                  every { connectionConfigUuid } returns testUuid
                },
              ).stream()
            }
            JesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<JesWorkingSetConfig> {
                  every { name } returns "wrong_test_name"
                  every { connectionConfigUuid } returns "test_other_uuid"
                }
              ).stream()
            }
            else -> {
              fail("Unrecognized getAll call")
            }
          }
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService.deleteZoweConfig(ZoweConfigType.LOCAL)

        assertSoftly { didWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { didCorrectWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { isClearCredentialsCalled shouldBe true }
        assertSoftly { didDeleteZoweConnection shouldBe true }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("not delete the Zowe connection with JES working set cause user declined removal of the JES working set") {
        every {
          showOkCancelDialogMock(any<String>(), any<String>(), any<String>(), any<String>(), any<Icon>())
        } answers {
          didWarningMessageForDeleteAppear = true
          if (!firstArg<String>().contains("wrong")) {
            didCorrectWarningMessageForDeleteAppear = true
          }
          Messages.CANCEL
        }

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfigMock).stream()
        }
        every {
          configServiceCrudableMock.getAll(any<Class<*>>())
        } answers {
          when ((invocation.args[0] as Class<*>).name) {
            FilesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<FilesWorkingSetConfig> {
                  every { name } returns "right_test_name"
                  every { connectionConfigUuid } returns testUuid
                },
              ).stream()
            }
            JesWorkingSetConfig::class.java.name -> {
              listOf<Any>(
                mockk<JesWorkingSetConfig> {
                  every { name } returns "right_test_name"
                  every { connectionConfigUuid } returns testUuid
                },
              ).stream()
            }
            else -> {
              fail("Unrecognized getAll call")
            }
          }
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService.deleteZoweConfig(ZoweConfigType.LOCAL)

        assertSoftly { didWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { didCorrectWarningMessageForDeleteAppear shouldBe true }
        assertSoftly { isClearCredentialsCalled shouldBe false }
        assertSoftly { didDeleteZoweConnection shouldBe false }
        assertSoftly { errorNotificationTrigerredCount shouldBe 0 }
      }

      should("throw an exception without the Zowe connection removal cause there is no z/OSMF connections") {
        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>().stream()
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)

        zoweConfigService.deleteZoweConfig(ZoweConfigType.LOCAL)

        assertSoftly { didWarningMessageForDeleteAppear shouldBe false }
        assertSoftly { didCorrectWarningMessageForDeleteAppear shouldBe false }
        assertSoftly { isClearCredentialsCalled shouldBe false }
        assertSoftly { didDeleteZoweConnection shouldBe false }
        assertSoftly { errorNotificationTrigerredCount shouldBe 1 }
      }
    }

    context("addZoweConfigFile") {
      val testIsAllowSelfSigned = true

      var zoweConnectionConfig = ConnectionConfig()

      var didSaveNewSecurePropertiesCalled = false

      mockkObject(ZoweConfig)

      val runWriteActionRef: (() -> Unit) -> Unit = ::runWriteAction
      mockkStatic(runWriteActionRef as KFunction<*>)
      every {
        runWriteActionRef(any<() -> Unit>())
      } answers {
        firstArg<() -> Unit>()()
      }

      beforeEach {
        zoweConnectionConfig = ConnectionConfig()
        zoweConnectionConfig.name = "Test not a Zowe connection"

        didSaveNewSecurePropertiesCalled = false

        every {
          ZoweConfig.saveNewSecureProperties(any<String>(), any<MutableMap<String, Any?>>(), any<KeytarWrapper>())
        } answers {
          didSaveNewSecurePropertiesCalled = true
        }
      }

      should("add Zowe config file, updating the contents with the correct parameters") {
        val testHost = "test.com"
        val testPort = "1234"

        var didCorrectlyChangeContent = false

        every {
          configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
        } returns listOf(zoweConnectionConfig).stream()

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every { zoweConfigService["createZoweSchemaJsonIfNotExists"]() } returns Unit
        every {
          zoweConfigService["saveChangedZoweConfig"](any<String>(), any<ByteArray>())
        } answers {
          didCorrectlyChangeContent = (String(secondArg<ByteArray>()) == "\"$testHost\":$testPort:${!testIsAllowSelfSigned}")
          Unit
        }

        mockkStatic("org.zowe.explorer.utils.MiscUtilsKt")
        every {
          getResourceAsStreamWrappable(any<ClassLoader>(),any<String>())
        } returns mockk {
          every { close() } answers {}
          every { readAllBytes() } returns "<HOST>:<PORT>:<SSL>".toByteArray()
        }

        val connectionDialogState = ConnectionDialogState(
          connectionUrl = "https://$testHost:$testPort",
          isAllowSsl = testIsAllowSelfSigned,
          username = "TSTUSR",
          password = "TSTPWD".toCharArray()
        )

        zoweConfigService.addZoweConfigFile(connectionDialogState)

        assertSoftly { didSaveNewSecurePropertiesCalled shouldBe true }
        assertSoftly { didCorrectlyChangeContent shouldBe true }
      }

      should("add Zowe config file, updating the contents with the localhost:10443, cause host and port are not recognized") {
        var didCorrectlyChangeContent = false

        every {
          configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
        } returns listOf(zoweConnectionConfig).stream()

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every { zoweConfigService["createZoweSchemaJsonIfNotExists"]() } returns Unit
        every {
          zoweConfigService["saveChangedZoweConfig"](any<String>(), any<ByteArray>())
        } answers {
          didCorrectlyChangeContent = (String(secondArg<ByteArray>()) == "\"localhost\":10443:${!testIsAllowSelfSigned}")
          Unit
        }

        mockkStatic("org.zowe.explorer.utils.MiscUtilsKt")
        every {
          getResourceAsStreamWrappable(any<ClassLoader>(),any<String>())
        } returns mockk {
          every { close() } answers {}
          every { readAllBytes() } returns "<HOST>:<PORT>:<SSL>".toByteArray()
        }

        val connectionDialogState = ConnectionDialogState(
          connectionUrl = "invalid_url",
          isAllowSsl = testIsAllowSelfSigned,
          username = "TSTUSR",
          password = "TSTPWD".toCharArray()
        )

        zoweConfigService.addZoweConfigFile(connectionDialogState)

        assertSoftly { didSaveNewSecurePropertiesCalled shouldBe true }
        assertSoftly { didCorrectlyChangeContent shouldBe true }
      }

      should("throw an exception cause there is no Zowe config file or it cannot be opened") {
        every {
          configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
        } returns listOf(zoweConnectionConfig).stream()

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every { zoweConfigService["createZoweSchemaJsonIfNotExists"]() } returns Unit

        mockkStatic("org.zowe.explorer.utils.MiscUtilsKt")
        every { getResourceAsStreamWrappable(any<ClassLoader>(),any<String>()) } returns null

        val connectionDialogState = ConnectionDialogState(
          connectionUrl = "http://test.com",
          isAllowSsl = testIsAllowSelfSigned,
          username = "TSTUSR",
          password = "TSTPWD".toCharArray()
        )

        assertThrows(Exception::class.java, "$ZOWE_CONFIG_NAME is not found") {
          zoweConfigService.addZoweConfigFile(connectionDialogState)
        }
        assertSoftly { didSaveNewSecurePropertiesCalled shouldBe false }
      }
    }

    context("checkAndRemoveOldZoweConnection") {
      should("remove Zowe config path and change the related connection name to transform Zowe connection to a simple plugin connection") {
        var updateCalledCount = 0
        var didCorrectlyChangeConnectionConfig = false

        val testConnectionConfigName = "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}"
        val zoweConnectionConfig = ConnectionConfig()
        zoweConnectionConfig.name = testConnectionConfigName
        zoweConnectionConfig.zoweConfigPath = "${projectMock.basePath}/${ZOWE_CONFIG_NAME}"

        every {
          configServiceCrudableMock.getAll(any<Class<out ConnectionConfig>>())
        } returns listOf(zoweConnectionConfig).stream()
        every {
          configServiceCrudableMock.update(any<ConnectionConfig>())
        } answers {
          updateCalledCount += 1
          val newConnectionConfig = firstArg<ConnectionConfig>()
          didCorrectlyChangeConnectionConfig = (
            newConnectionConfig.name == "${testConnectionConfigName}1" && newConnectionConfig.zoweConfigPath == null
          )
          firstArg<ConnectionConfig>().optional
        }

        val zoweConfigService = ZoweConfigServiceImpl(projectMock)
        zoweConfigService.checkAndRemoveOldZoweConnection(ZoweConfigType.LOCAL)

        assertSoftly { updateCalledCount shouldBe 1 }
        assertSoftly { didCorrectlyChangeConnectionConfig shouldBe true }
      }
    }

    context("getZoweConfigState") {
      val testProtocol = "https"
      val testHost = "test.com"
      val testPort = "1234"
      val testBasePath = "/test/base/path"
      val testUsername = "TSTUSR"
      val testPassword = "TSTPWD"
      val testProfileName = "test_profile_name.test_profile_name_inner"
      val testIsAllowSelfSigned = false

      beforeEach {
        every { credentialService.getUsernameByKey(any<String>()) } returns testUsername
        every { credentialService.getPasswordByKey(any<String>()) } returns testPassword.toCharArray()
      }

      should("return SYNCHRONIZED config state for the local Zowe config") {
        val localConnectionConfig = ConnectionConfig()
        localConnectionConfig.name = "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testProfileName/${projectMock.name}"
        localConnectionConfig.zoweConfigPath = "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
        localConnectionConfig.url = "$testProtocol://$testHost:$testPort$testBasePath"
        localConnectionConfig.isAllowSelfSigned = testIsAllowSelfSigned

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfig).stream()
        }

        val localZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns testUsername
              every { password } returns testPassword
              every { profileName } returns testProfileName
              every { basePath } returns testBasePath
              every { host } returns testHost
              every { zosmfPort } returns testPort
              every { protocol } returns testProtocol
              every { rejectUnauthorized } returns !testIsAllowSelfSigned
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every {
          zoweConfigService["findExistingConnection"](any<ZoweConfigType>(), any<String>())
        } returns localConnectionConfig
        zoweConfigService.localZoweConfig = localZoweConfig

        val zoweConfigState = zoweConfigService.getZoweConfigState(false, ZoweConfigType.LOCAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.SYNCHRONIZED }
      }

      should("return NEED_TO_UPDATE config state for the global Zowe config cause the config is changed") {
        val globalConnectionConfig = ConnectionConfig()
        globalConnectionConfig.name = "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-$testProfileName"
        globalConnectionConfig.zoweConfigPath = System.getProperty("user.home").replace("((\\*)|(/*))$", "") + "/.zowe/" + ZOWE_CONFIG_NAME
        globalConnectionConfig.url = "$testProtocol://$testHost:$testPort$testBasePath"
        globalConnectionConfig.isAllowSelfSigned = true

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(globalConnectionConfig).stream()
        }

        val globalZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns testUsername
              every { password } returns testPassword
              every { profileName } returns testProfileName
              every { basePath } returns testBasePath
              every { host } returns testHost
              every { zosmfPort } returns testPort
              every { protocol } returns testProtocol
              every { rejectUnauthorized } returns true
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every {
          zoweConfigService["findExistingConnection"](any<ZoweConfigType>(), any<String>())
        } returns globalConnectionConfig
        zoweConfigService.globalZoweConfig = globalZoweConfig

        val zoweConfigState = zoweConfigService.getZoweConfigState(false, ZoweConfigType.GLOBAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NEED_TO_UPDATE }
      }

      should("return NEED_TO_ADD config state for the local Zowe config cause there is no connection configs at all") {
        val localConnectionConfig = ConnectionConfig()
        localConnectionConfig.name = "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}"
        localConnectionConfig.zoweConfigPath = "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
        localConnectionConfig.url = "$testProtocol://$testHost:$testPort$testBasePath"
        localConnectionConfig.isAllowSelfSigned = testIsAllowSelfSigned

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf<ConnectionConfig>().stream()
        }

        val localZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns testUsername
              every { password } returns testPassword
              every { profileName } returns testProfileName
              every { basePath } returns testBasePath
              every { host } returns testHost
              every { zosmfPort } returns testPort
              every { protocol } returns testProtocol
              every { rejectUnauthorized } returns !testIsAllowSelfSigned
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every {
          zoweConfigService["findExistingConnection"](any<ZoweConfigType>(), any<String>())
        } returns localConnectionConfig
        zoweConfigService.localZoweConfig = localZoweConfig

        val zoweConfigState = zoweConfigService.getZoweConfigState(false, ZoweConfigType.LOCAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NEED_TO_ADD }
      }

      should("return NEED_TO_ADD config state for the local Zowe config cause there is no related connection config") {
        val localConnectionConfig = ConnectionConfig()
        localConnectionConfig.name = "$ZOWE_PROJECT_PREFIX${ZoweConfigType.LOCAL}-zosmf/${projectMock.name}"
        localConnectionConfig.zoweConfigPath = "${projectMock.basePath}/$ZOWE_CONFIG_NAME"
        localConnectionConfig.url = "$testProtocol://$testHost:$testPort$testBasePath"
        localConnectionConfig.isAllowSelfSigned = testIsAllowSelfSigned

        every {
          configServiceCrudableMock.find(any<Class<out ConnectionConfig>>(), any<Predicate<in ConnectionConfig>>())
        } answers {
          listOf(localConnectionConfig).stream()
        }

        val localZoweConfig: ZoweConfig = mockk {
          every {
            getListOfZosmfConnections()
          } returns listOf(
            mockk {
              every { user } returns testUsername
              every { password } returns testPassword
              every { profileName } returns testProfileName
              every { basePath } returns testBasePath
              every { host } returns testHost
              every { zosmfPort } returns testPort
              every { protocol } returns testProtocol
              every { rejectUnauthorized } returns !testIsAllowSelfSigned
              every { encoding } returns 1047
              every { responseTimeout } returns 600
            }
          )
        }

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every {
          zoweConfigService["findExistingConnection"](any<ZoweConfigType>(), any<String>())
        } returns null
        zoweConfigService.localZoweConfig = localZoweConfig

        val zoweConfigState = zoweConfigService.getZoweConfigState(false, ZoweConfigType.LOCAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NEED_TO_ADD }
      }

      should("return NOT_EXISTS config state for the local Zowe config cause there is no local config found") {
        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        every { zoweConfigService["scanForZoweConfig"](any<ZoweConfigType>()) } returns null
        zoweConfigService.localZoweConfig = null

        val zoweConfigState = zoweConfigService.getZoweConfigState(true, ZoweConfigType.LOCAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NOT_EXISTS }
      }

      should("return NOT_EXISTS config state for the global Zowe config cause there is no global config found") {
        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)
        zoweConfigService.globalZoweConfig = null

        val zoweConfigState = zoweConfigService.getZoweConfigState(false, ZoweConfigType.GLOBAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NOT_EXISTS }
      }

      should("return NOT_EXISTS config state for the empty local Zowe config file") {
        val parseConfigJsonRef: (InputStream) -> ZoweConfig = ::parseConfigJson
        mockkStatic(parseConfigJsonRef as KFunction<*>)
        every { parseConfigJsonRef(any<InputStream>()) } answers { throw EmptyZoweConfigFileException() }

        val zoweConfigService = spyk(ZoweConfigServiceImpl(projectMock), recordPrivateCalls = true)

        val zoweConfigState = zoweConfigService.getZoweConfigState(true, ZoweConfigType.LOCAL)

        assertSoftly { zoweConfigState shouldBe ZoweConfigState.NOT_EXISTS }
      }
    }
  }
})
