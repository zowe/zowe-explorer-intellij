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
