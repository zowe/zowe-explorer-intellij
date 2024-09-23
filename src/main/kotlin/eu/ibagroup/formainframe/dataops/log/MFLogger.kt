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

/**
 * Base interface for fetching and display mainframe processes log
 * such as job logs, or rex execution log, etc.
 * @author Valentine Krus
 */
interface MFLogger<LFetcher: LogFetcher<out MFProcessInfo>> {
  /**
   * Instance of low-level LogFetcher object.
   */
  val logFetcher: LFetcher

  /**
   * Starts logging in current thread.
   */
  fun startLoggingSync()

  /**
   * Starts logging in background.
   */
  fun startLogging()

  /**
   * Sets handler for mainframe process finish event.
   * @param finishHandler handler that will be invoked after mainframe process finishing.
   */
  fun onLogFinished(finishHandler: () -> Unit)

  /**
   * Sets handler for event after requesting next portion of mainframe log.
   * @param nextLogHandler handler that will be invoked after next request to fetching log from mainframe.
   */
  fun onNextLog(nextLogHandler: (Array<String>) -> Unit)

  /**
   * Stops logging process (doesn't stop real mainframe process).
   */
  fun stopLogging()
}
