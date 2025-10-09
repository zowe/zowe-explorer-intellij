/*
 * Copyright (c) 2024 IBA Group.
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

package org.zowe.explorer.v3

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.messages.Topic
import kotlinx.coroutines.delay

/**
 * Finds the endpoint for the topic listeners across the application
 * @param topic the topic to search the endpoint for
 * @return the listener interface to produce messages to every related subscriber
 */
fun <L : Any> getListenerEndpoint(topic: Topic<L>): L {
  return ApplicationManager.getApplication()
    .messageBus
    .syncPublisher(topic)
}

/**
 * Perform a function with a periodic check of its result.
 * Each check is performed in a progressive way, starting with 10 milliseconds.
 * Each 10 delays, the delay time is doubled, until the end of the function or until the delay is more than 10 seconds
 * @param operationFun the function to execute each time, must return true if it is still active, and false if it is finished
 */
suspend fun performWithProgressiveDelay(operationFun: () -> Boolean) {
  var currDelayMillis = 10L
  var cumulativeDelaysCount = 0
  val killSwitch = 10000L
  while (operationFun()) {
    if (currDelayMillis > killSwitch)
      throw Exception("Operation is not completed, delay time is too long ($currDelayMillis)")
    delay(currDelayMillis)
    cumulativeDelaysCount++
    if (cumulativeDelaysCount == 10) {
      cumulativeDelaysCount = 0
      currDelayMillis *= 2
    }
  }
}
