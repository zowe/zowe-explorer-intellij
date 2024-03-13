/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.testutils.testServiceImpl

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import org.zowe.explorer.dataops.DataOpsManager
import org.zowe.explorer.dataops.Operation
import org.zowe.explorer.dataops.Query
import org.zowe.explorer.dataops.attributes.AttributesService
import org.zowe.explorer.dataops.attributes.FileAttributes
import org.zowe.explorer.dataops.content.adapters.MFContentAdapter
import org.zowe.explorer.dataops.content.synchronizer.ContentSynchronizer
import org.zowe.explorer.dataops.fetch.FileFetchProvider
import org.zowe.explorer.dataops.log.LogFetcher
import org.zowe.explorer.dataops.log.MFLogger
import org.zowe.explorer.dataops.log.MFProcessInfo
import org.zowe.explorer.dataops.operations.mover.names.CopyPasteNameResolver
import io.mockk.every
import io.mockk.mockk

open class TestDataOpsManagerImpl(override val componentManager: ComponentManager) : DataOpsManager {

  /**
   * Test instance for the DataOpsManager.
   * Defines default behaviour of the service at initialization stage.
   * All the test class methods use this implementation, so it makes this easier to redefine in a test case
   */
  var testInstance: DataOpsManager = object : DataOpsManager {
    override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
      attributesClass: Class<out A>,
      vFileClass: Class<out F>
    ): AttributesService<A, F> {
      TODO("Not yet implemented")
    }

    override fun tryToGetAttributes(file: VirtualFile): FileAttributes {
      throw NotImplementedError("An operation should be implemented during the test execution")
    }

    override fun tryToGetFile(attributes: FileAttributes): VirtualFile? {
      TODO("Not yet implemented")
    }

    override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
      requestClass: Class<out R>,
      queryClass: Class<out Query<*, *>>,
      vFileClass: Class<out File>
    ): FileFetchProvider<R, Q, File> {
      TODO("Not yet implemented")
    }

    override fun isSyncSupported(file: VirtualFile): Boolean {
      TODO("Not yet implemented")
    }

    override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer {
      val contentSynchronizerMock = mockk<ContentSynchronizer>()
      every { contentSynchronizerMock.synchronizeWithRemote(any(), any()) } returns Unit
      return contentSynchronizerMock
    }

    override fun getMFContentAdapter(file: VirtualFile): MFContentAdapter {
      TODO("Not yet implemented")
    }

    override fun getNameResolver(source: VirtualFile, destination: VirtualFile): CopyPasteNameResolver {
      TODO("Not yet implemented")
    }

    override fun isOperationSupported(operation: Operation<*>): Boolean {
      TODO("Not yet implemented")
    }

    override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
      TODO("Not yet implemented")
    }

    override fun <PInfo : MFProcessInfo, LFetcher : LogFetcher<PInfo>> createMFLogger(
      mfProcessInfo: PInfo,
      consoleView: ConsoleView
    ): MFLogger<LFetcher> {
      TODO("Not yet implemented")
    }

    override val componentManager: ComponentManager
      get() = TODO("Not yet implemented")

    override fun dispose() {
      TODO("Not yet implemented")
    }

  }

  override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>,
    vFileClass: Class<out F>
  ): AttributesService<A, F> {
    return this.testInstance.getAttributesService(attributesClass, vFileClass)
  }

  override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
    return this.testInstance.tryToGetAttributes(file)
  }

  override fun tryToGetFile(attributes: FileAttributes): VirtualFile? {
    return this.testInstance.tryToGetFile(attributes)
  }

  override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File> {
    return this.testInstance.getFileFetchProvider(requestClass, queryClass, vFileClass)
  }

  override fun isSyncSupported(file: VirtualFile): Boolean {
    return this.testInstance.isSyncSupported(file)
  }

  override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
    return this.testInstance.getContentSynchronizer(file)
  }

  override fun getMFContentAdapter(file: VirtualFile): MFContentAdapter {
    return this.testInstance.getMFContentAdapter(file)
  }

  override fun getNameResolver(source: VirtualFile, destination: VirtualFile): CopyPasteNameResolver {
    return this.testInstance.getNameResolver(source, destination)
  }

  override fun isOperationSupported(operation: Operation<*>): Boolean {
    return this.testInstance.isOperationSupported(operation)
  }

  override fun <R : Any> performOperation(operation: Operation<R>, progressIndicator: ProgressIndicator): R {
    return this.testInstance.performOperation(operation, progressIndicator)
  }

  override fun <PInfo : MFProcessInfo, LFetcher : LogFetcher<PInfo>> createMFLogger(
    mfProcessInfo: PInfo,
    consoleView: ConsoleView
  ): MFLogger<LFetcher> {
    return this.testInstance.createMFLogger(mfProcessInfo, consoleView)
  }

  override fun dispose() {
    return this.testInstance.dispose()
  }
}
