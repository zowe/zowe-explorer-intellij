package eu.ibagroup.formainframe.testServiceImpl

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.DataOpsManager
import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.formainframe.dataops.Query
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.content.adapters.MFContentAdapter
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.log.LogFetcher
import eu.ibagroup.formainframe.dataops.log.MFLogger
import eu.ibagroup.formainframe.dataops.log.MFProcessInfo
import io.mockk.every
import io.mockk.mockk

class TestDataOpsManagerImpl(override val componentManager: ComponentManager) : DataOpsManager {

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

    override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
      TODO("Not yet implemented")
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
