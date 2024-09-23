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

package eu.ibagroup.formainframe.dataops

import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.content.adapters.DefaultContentAdapter
import eu.ibagroup.formainframe.dataops.content.adapters.MFContentAdapter
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.log.AbstractMFLoggerBase
import eu.ibagroup.formainframe.dataops.log.LogFetcher
import eu.ibagroup.formainframe.dataops.log.MFLogger
import eu.ibagroup.formainframe.dataops.log.MFProcessInfo
import eu.ibagroup.formainframe.dataops.operations.OperationRunner
import eu.ibagroup.formainframe.dataops.operations.mover.names.CopyPasteNameResolver
import eu.ibagroup.formainframe.dataops.operations.mover.names.DefaultNameResolver
import eu.ibagroup.formainframe.utils.associateListedBy
import eu.ibagroup.formainframe.utils.findAnyNullable
import eu.ibagroup.formainframe.utils.log

/**
 * Data operation manager implementation class.
 * Provides functions to obtain files/folders from mainframe and perform operations on it
 */
class DataOpsManagerImpl : DataOpsManager {

  /**
   * Creates a list of components
   * @return MutableList of components
   */
  private fun <Component> List<DataOpsComponentFactory<Component>>.buildComponents(): MutableList<Component> {
    return buildComponents(this@DataOpsManagerImpl)
  }

  override val componentManager: ComponentManager
    get() = ApplicationManager.getApplication()

  private val attributesServiceDelegate = lazy {
    @Suppress("UNCHECKED_CAST")
    AttributesService.EP.extensionList.buildComponents() as MutableList<AttributesService<FileAttributes, VirtualFile>>
  }
  private val attributesServices by attributesServiceDelegate

  /**
   * Returns instance of object which performs actions with attributes of files/folders
   * @param A object that contains info about attributes of file/folder
   * @param F object that represents file or folder
   * @return instance of service which can perform actions on attributes
   */
  override fun <A : FileAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>,
    vFileClass: Class<out F>
  ): AttributesService<A, F> {
    @Suppress("UNCHECKED_CAST")
    return attributesServices.find {
      it.attributesClass.isAssignableFrom(attributesClass) && it.vFileClass.isAssignableFrom(vFileClass)
    } as AttributesService<A, F>? ?: throw IllegalArgumentException(
      "Cannot find AttributesService for attributeClass=${attributesClass.name} and vFileClass=${vFileClass.name}"
    )
  }

  /**
   * Returns attributes of file/folder
   * @param file object that represents file or folder
   * @return attributes of file/folder
   */
  override fun tryToGetAttributes(file: VirtualFile): FileAttributes? {
    return attributesServices
      .stream()
      .filter { it.vFileClass.isAssignableFrom(file::class.java) }
      .map { it.getAttributes(file) }
      .filter { it != null }
      .findAnyNullable()
  }

  /**
   * Returns instance of object that represents file/folder
   * @param attributes object that contains info about attributes of file/folder
   * @return object that represents file/folder
   */
  override fun tryToGetFile(attributes: FileAttributes): VirtualFile? {
    return attributesServices.stream()
      .map { it.getVirtualFile(attributes) }
      .filter { it != null }
      .findAnyNullable()
  }

  private val fileFetchProvidersDelegate = lazy {
    FileFetchProvider.EP.extensionList.buildComponents()
  }
  private val fileFetchProviders by fileFetchProvidersDelegate

  /**
   * Returns instance of file fetch provider which fetches content of files/folders from mainframe
   * @param R root object which requests instance of fetch provider
   * @param Q query which contains fetch operation inside
   * @param File file which needs to be fetched
   * @return instance of object which can fetch content of files/folders
   */
  override fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File> {
    @Suppress("UNCHECKED_CAST")
    return fileFetchProviders.find {
      it.requestClass.isAssignableFrom(requestClass)
        && it.queryClass.isAssignableFrom(queryClass)
        && it.vFileClass.isAssignableFrom(vFileClass)
    } as FileFetchProvider<R, Q, File>? ?: throw IllegalArgumentException(
      "Cannot find FileFetchProvider for " +
        "requestClass=${requestClass.name}; queryClass=${queryClass.name}; vFileClass=${vFileClass.name}"
    )
  }

  private val contentSynchronizersDelegate = lazy {
    ContentSynchronizer.EP.extensionList.buildComponents()
  }

  private val contentSynchronizers by contentSynchronizersDelegate

  private val mfContentAdaptersDelegate = lazy {
    MFContentAdapter.EP.extensionList.buildComponents()
  }
  private val mfContentAdapters by mfContentAdaptersDelegate

  private val nameResolversDelegate = lazy {
    CopyPasteNameResolver.EP.extensionList.buildComponents()
  }
  private val nameResolvers by nameResolversDelegate

  override fun getNameResolver(source: VirtualFile, destination: VirtualFile): CopyPasteNameResolver {
    return nameResolvers.firstOrNull { it.accepts(source, destination) } ?: DefaultNameResolver()
  }

  /**
   * Checks if sync with mainframe is supported for provided object
   * @param file object on mainframe that should be checked on availability of synchronization
   * @return is sync possible for provided object
   */
  override fun isSyncSupported(file: VirtualFile): Boolean {
    return contentSynchronizers.firstOrNull { it.accepts(file) } != null
  }

  /**
   * Returns instance of object responsible for synchronization process of file with mainframe
   * @param file object on mainframe that should be synchronized
   * @return instance of object responsible for synchronization
   */
  override fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer? {
    return contentSynchronizers.firstOrNull { it.accepts(file) }
  }

  /**
   * Returns instance of content adapter to mainframe
   * @param file object that represents file/folder on mainframe
   * @return instance of content adapter to mainframe
   */
  override fun getMFContentAdapter(file: VirtualFile): MFContentAdapter {
    return mfContentAdapters.firstOrNull { it.accepts(file) } ?: DefaultContentAdapter(this)
  }

  private val operationRunners by lazy {
    @Suppress("UNCHECKED_CAST")
    val operationRunnersList =
      OperationRunner.EP.extensionList.buildComponents() as MutableList<OperationRunner<Operation<*>, *>>
    operationRunnersList.associateListedBy { it.operationClass }
  }

  /**
   * Returns instance of log fetcher object
   * @param processInfo object that contains unique info about connection to mainframe
   * @return instance of log fetcher object
   */
  private fun createLogFetcher(processInfo: MFProcessInfo): LogFetcher<*>? {
    return LogFetcher.EP.extensionList.firstOrNull { it.acceptsProcessInfo(processInfo) }?.buildComponent(this)
  }

  /**
   * Checks if operation can be executed
   * @param operation object that represents operation
   * @return is operation can be performed
   */
  override fun isOperationSupported(operation: Operation<*>): Boolean {
    return operationRunners[operation::class.java]?.any { it.canRun(operation) } == true
  }

  /**
   * Perform operation on mainframe
   * @param operation operation that needs to be executed
   * @param progressIndicator interrupts operation if the computation is canceled
   * @return result of operation
   */
  override fun <R : Any> performOperation(
    operation: Operation<R>,
    progressIndicator: ProgressIndicator
  ): R {
    val opRunner = operationRunners[operation::class.java]?.find { it.canRun(operation) } ?: throw NoSuchElementException("Operation $operation not found").also {
      log<DataOpsManagerImpl>().info(it)
    }
    var startOpMessage = "Operation '${opRunner.operationClass.simpleName}' has been started"
    if (operation is Query<*, *>) {
      startOpMessage += "\nRequest params: ${operation.request}"
    }
    val result = runCatching {
      opRunner.log.info(startOpMessage)
      opRunner
        .run(operation, progressIndicator)
    }.onSuccess {
      opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has been completed successfully")
    }.onFailure {
      opRunner.log.info("Operation '${opRunner.operationClass.simpleName}' has failed", it)
      throw it
    }
    @Suppress("UNCHECKED_CAST")
    return result.getOrNull() as R
  }

  /**
   * @see DataOpsManager.createMFLogger
   */
  override fun <PInfo : MFProcessInfo, LFetcher : LogFetcher<PInfo>> createMFLogger(
    mfProcessInfo: PInfo,
    consoleView: ConsoleView
  ): MFLogger<LFetcher> {
    val logFetcher = createLogFetcher(mfProcessInfo)
      ?: throw IllegalArgumentException("Unsupported Log Information $mfProcessInfo")

    @Suppress("UNCHECKED_CAST")
    val resultFetcher: LFetcher = logFetcher as LFetcher
    return object : AbstractMFLoggerBase<PInfo, LFetcher>(mfProcessInfo, consoleView) {
      override val logFetcher: LFetcher = resultFetcher
    }.also {
      Disposer.register(this, it)
    }
  }

  /**
   * Clear the attributes service, file fetch providers, content synchronizers and mf content adapters
   * if they are already initialized
   */
  override fun dispose() {
    if (attributesServiceDelegate.isInitialized()) attributesServices.clear()
    if (fileFetchProvidersDelegate.isInitialized()) fileFetchProviders.clear()
    if (contentSynchronizersDelegate.isInitialized()) contentSynchronizers.clear()
    if (mfContentAdaptersDelegate.isInitialized()) mfContentAdapters.clear()
    Disposer.dispose(this)
  }
}
