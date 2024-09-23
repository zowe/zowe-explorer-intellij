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
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.toMutableSmartList
import eu.ibagroup.formainframe.dataops.attributes.AttributesService
import eu.ibagroup.formainframe.dataops.attributes.FileAttributes
import eu.ibagroup.formainframe.dataops.content.adapters.MFContentAdapter
import eu.ibagroup.formainframe.dataops.content.synchronizer.ContentSynchronizer
import eu.ibagroup.formainframe.dataops.fetch.FileFetchProvider
import eu.ibagroup.formainframe.dataops.log.LogFetcher
import eu.ibagroup.formainframe.dataops.log.MFLogger
import eu.ibagroup.formainframe.dataops.log.MFProcessInfo
import eu.ibagroup.formainframe.dataops.operations.mover.names.CopyPasteNameResolver

interface DataOpsManager : Disposable {

  companion object {
    @JvmStatic
    fun getService(): DataOpsManager = service()
  }

  fun <A : FileAttributes, F : VirtualFile> getAttributesService(
    attributesClass: Class<out A>, vFileClass: Class<out F>
  ): AttributesService<A, F>

  fun tryToGetAttributes(file: VirtualFile): FileAttributes?

  fun tryToGetFile(attributes: FileAttributes): VirtualFile?

  fun <R : Any, Q : Query<R, Unit>, File : VirtualFile> getFileFetchProvider(
    requestClass: Class<out R>,
    queryClass: Class<out Query<*, *>>,
    vFileClass: Class<out File>
  ): FileFetchProvider<R, Q, File>

  /**
   * Checks if the [ContentSynchronizer] instance capable to specified file exists.
   * @param file virtual file to check if capable synchronizer exists.
   * @return true if capable [ContentSynchronizer] instance exists or false otherwise
   */
  fun isSyncSupported(file: VirtualFile): Boolean

  /**
   * Finds [ContentSynchronizer] instance that can synchronize content for passed file.
   * @param file virtual file to find [ContentSynchronizer] instance
   * @return founded [ContentSynchronizer] instance or null if no one content synchronizer doesn't accept passed file.
   */
  fun getContentSynchronizer(file: VirtualFile): ContentSynchronizer?

  fun getMFContentAdapter(file: VirtualFile): MFContentAdapter

  fun getNameResolver(source: VirtualFile, destination: VirtualFile): CopyPasteNameResolver

  fun isOperationSupported(operation: Operation<*>): Boolean

  @Throws(Throwable::class)
  fun <R : Any> performOperation(
    operation: Operation<R>,
    progressIndicator: ProgressIndicator = DumbProgressIndicator.INSTANCE
  ): R

  /**
   * Creates mainframe process logger for corresponding MFProcessInfo and LogFetcher.
   * @param mfProcessInfo information about mainframe process that identifies it.
   * @param consoleView console to log into
   * @return instance of MFLogger.
   * @see MFLogger
   */
  fun <PInfo : MFProcessInfo, LFetcher : LogFetcher<PInfo>> createMFLogger(
    mfProcessInfo: PInfo,
    consoleView: ConsoleView
  ): MFLogger<LFetcher>

  val componentManager: ComponentManager
}

inline fun <reified A : FileAttributes, reified F : VirtualFile> DataOpsManager.getAttributesService(): AttributesService<A, F> {
  return getAttributesService(A::class.java, F::class.java)
}

fun <C> List<DataOpsComponentFactory<C>>.buildComponents(dataOpsManager: DataOpsManager): MutableList<C> {
  return map { it.buildComponent(dataOpsManager) }.toMutableSmartList()
}
