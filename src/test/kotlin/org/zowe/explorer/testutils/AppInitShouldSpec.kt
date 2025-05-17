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

package org.zowe.explorer.testutils

import com.intellij.application.options.CodeStyle
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectImpl
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.AnnotationOrderRootType
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.ProjectRootManagerImpl
import com.intellij.openapi.roots.impl.libraries.LibraryTableTracker
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.IconLoader.clearCacheInTests
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.impl.VirtualFilePointerTracker
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl
import com.intellij.testFramework.*
import com.intellij.testFramework.common.Cleanup
import com.intellij.testFramework.common.ThreadUtil
import com.intellij.ui.IconManager
import com.intellij.util.containers.ContainerUtil
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.ShouldSpec
import java.nio.file.Path
import java.util.UUID

/**
 * [ShouldSpec] wrapper for IntelliJ platform specific test cases.
 * The idea is to provide a partly initialized [Application] instance to be able to use and mock base platform parts
 */
abstract class AppInitShouldSpec(testSpecName: String, body: ShouldSpec.() -> Unit = {}) : MockkAwareShouldSpec(body) {
  /**
   * This class is a duplicate of the [LightPlatformTestCase.SimpleLightProjectDescriptor].
   * Is needed to put a client property to omit startup activities initialization
   */
  class ZoweLightProjectDescriptor(moduleTypeId: String, sdk: Sdk?) : LightProjectDescriptor() {
    private var myModuleTypeId: String? = null
    private var mySdk: Sdk? = null

    init {
      myModuleTypeId = moduleTypeId
      mySdk = sdk
    }

    override fun getSdk(): Sdk? {
      return mySdk
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false

      val that = other as ZoweLightProjectDescriptor

      if (myModuleTypeId != that.myModuleTypeId) return false
      return areJdksEqual(that.mySdk)
    }

    override fun hashCode(): Int {
      return myModuleTypeId.hashCode()
    }

    private fun areJdksEqual(newSdk: Sdk?): Boolean {
      if (mySdk == null || newSdk == null) return mySdk === newSdk
      if (mySdk?.name != newSdk.name) return false

      val rootTypes = arrayOf(OrderRootType.CLASSES, AnnotationOrderRootType.getInstance())
      for (rootType in rootTypes) {
        val myUrls = mySdk?.rootProvider?.getUrls(rootType) ?: arrayOf()
        val newUrls = newSdk.rootProvider.getUrls(rootType)
        if (ContainerUtil.newHashSet(*myUrls) != ContainerUtil.newHashSet(*newUrls)) return false
      }
      return true
    }

    override fun setUpProject(project: Project, handler: SetupHandler) {
      // Absolutely needed here, otherwise the tests are stuck forever in 231.*
      project.putUserData(ProjectImpl.RUN_START_UP_ACTIVITIES, false)
      super.setUpProject(project, handler)
    }
  }

  class OpenLifecycleTestCase(private val testSpecName: String) : LightPlatformTestCase() {
    fun beforeSpec() {
      name = "test_$testSpecName"
      setUp()
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
      return ZoweLightProjectDescriptor(moduleTypeId, projectJDK)
    }

    /**
     * After spec is a cleanup functions, copied from [LightPlatformTestCase] and [UsefulTestCase],
     * with removed leaking thread checks.
     * The Kotest library is designed to run on coroutines, and IntelliJ Platform
     * considers them as leaking threads, that prevents the normal tear down process to happen
     */
    fun afterSpec() {
      val project = project

      if (ApplicationManager.getApplication() != null) {
        CodeStyle.dropTemporarySettings(project)
      }
      val codeStyleSettingsTracker = getPrivateFieldValue(
        this,
        "myCodeStyleSettingsTracker",
        LightPlatformTestCase::class.java
      ) as CodeStyleSettingsTracker
      codeStyleSettingsTracker.checkForSettingsDamage()
      if (project != null) {
        org.zowe.explorer.utils.runInEdtAndWait {
          TestApplicationManager.tearDownProjectAndApp(project)
          (ProjectRootManager.getInstance(project) as ProjectRootManagerImpl).clearScopesCachesForModules()
        }
      }
      org.zowe.explorer.utils.runInEdtAndWait {
        checkEditorsReleased()
      }

      if (isIconRequired) {
        IconManager.deactivate()
        clearCacheInTests()
      }
      disposeRootDisposable()
      GlobalState.checkSystemStreams()
      Cleanup.cleanupSwingDataStructures()
      Disposer.setDebugMode(true)
      val tempDir = getPrivateFieldValue(
        this,
        "myTempDir",
        UsefulTestCase::class.java
      )
      val originalTempDir = getPrivateFieldValue(
        this,
        "ORIGINAL_TEMP_DIR",
        UsefulTestCase::class.java
      ) as String
      FileUtil.resetCanonicalTempPathCache(originalTempDir)
      val removeGlobalTempDirectoryMethod = UsefulTestCase::class.java
        .getDeclaredMethod("removeGlobalTempDirectory", Path::class.java)
      removeGlobalTempDirectoryMethod.isAccessible = true
      try {
        org.zowe.explorer.utils.runInEdtAndWait {
          removeGlobalTempDirectoryMethod(this, tempDir)
        }
      } catch (e: Throwable) {
        ThreadUtil.printThreadDump()
        throw e
      }
//      ThrowableRunnable<Throwable> { waitForAppLeakingThreads(10, TimeUnit.SECONDS) },
      clearFields(this)
      val sdkParentDisposable = getPrivateFieldValue(
        this,
        "mySdkParentDisposable",
        LightPlatformTestCase::class.java
      ) as Disposable
      Disposer.dispose(sdkParentDisposable)
      val oldSdks = getPrivateFieldValue(
        this,
        "myOldSdks",
        LightPlatformTestCase::class.java
      ) as SdkLeakTracker
      oldSdks.checkForJdkTableLeaks()
//        ThrowableRunnable<Throwable> {
//          if (myThreadTracker != null) {
//            myThreadTracker.checkLeak()
//          }
//        },
      if (project != null) {
        InjectedLanguageManagerImpl.checkInjectorsAreDisposed(project)
      }
      val virtualFilePointerTracker = getPrivateFieldValue(
        this,
        "myVirtualFilePointerTracker",
        LightPlatformTestCase::class.java
      ) as VirtualFilePointerTracker
      virtualFilePointerTracker.assertPointersAreDisposed()
      val libraryTableTracker = getPrivateFieldValue(
        this,
        "myLibraryTableTracker",
        LightPlatformTestCase::class.java
      ) as LibraryTableTracker
      libraryTableTracker.assertDisposed()
      resetAllFields()
    }
  }

  companion object {
    var currentTestUuid: UUID? = null
  }

  private val platformTestCase: OpenLifecycleTestCase = OpenLifecycleTestCase(testSpecName)

  override suspend fun beforeSpec(spec: Spec) {
    currentTestUuid = UUID.randomUUID()
    platformTestCase.beforeSpec()
    super.beforeSpec(spec)
  }

  override suspend fun afterSpec(spec: Spec) {
    super.afterSpec(spec)
    platformTestCase.afterSpec()
  }
}
