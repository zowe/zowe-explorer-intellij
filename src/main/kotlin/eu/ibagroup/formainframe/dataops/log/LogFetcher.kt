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

package eu.ibagroup.formainframe.dataops.log

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * Base interface for performing fetching mainframe process log.
 *
 * ATTENTION!!!
 * Each mainframe process will have its own LogFetcher instance.
 * LogFetcher class can be registered through LogFetcherFactory and
 * DataOpsManager will create several log fetcher through this factory
 * for each process. That's why it is possible to store temporary process
 * log data inside log fetcher if it is needed.
 *
 * @author Valentine Krus
 */
interface LogFetcher<PInfo: MFProcessInfo> {

  companion object {
    @JvmField
    val EP = ExtensionPointName.create<LogFetcherFactory>("eu.ibagroup.formainframe.logFetcher")
  }

  /**
   * Fetches log from mainframe by data represented in process info.
   * @param mfProcessInfo necessary process information to request process log (connection, jobid, jobname, etc.).
   * @return parts of log. For example job log consists of several spool logs.
   * If process log is not divided into parts will return array with single element.
   */
  fun fetchLog(mfProcessInfo: PInfo): Array<String>

  /**
   * Indicates if process log finished (in most cases indicates if process finished).
   * @param mfProcessInfo necessary process information to request process status.
   * @return true if process finished and false otherwise.
   */
  fun isLogFinished(mfProcessInfo: PInfo): Boolean

  /**
   * Forms log postfix for displaying some additional information.
   * @param mfProcessInfo necessary process information to request process status or log.
   * @return formed postfix
   */
  fun logPostfix(mfProcessInfo: PInfo): String = ""

  val mfProcessInfoClass: Class<out MFProcessInfo>
}
