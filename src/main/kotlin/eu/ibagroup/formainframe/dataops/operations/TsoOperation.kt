/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package eu.ibagroup.formainframe.dataops.operations

import eu.ibagroup.formainframe.dataops.Operation
import eu.ibagroup.r2z.TsoResponse

/**
 * Class which represents any TSO operation, e.g. start new TSO session
 * @param state - state of TSO session, usually contains startup parameters or config wrapper for the TSO session created
 * @param mode - operation mode
 * @param messageType - message type
 * @param messageData - message data
 * @param message - message to be sent
 */
class TsoOperation(var state: Any,
                   var mode: TsoOperationMode,
                   var messageType: MessageType? = null,
                   var messageData: MessageData? = null,
                   var message: String? = null)
  : Operation<TsoResponse> {

  override val resultClass = TsoResponse::class.java
}

/**
 * Enum class which represents a possible operation types
 */
enum class TsoOperationMode(val mode : String) {
  START("start"),
  SEND_MESSAGE("send"),
  GET_MESSAGES("get"),
  STOP("stop");

  override fun toString(): String {
    return mode
  }
}

/**
 * Enum class which represents a possible message types
 */
enum class MessageType(val type: String) {
  TSO_MESSAGE("TSO MESSAGE"),
  TSO_PROMPT("TSO PROMPT"),
  TSO_RESPONSE("TSO RESPONSE");

  override fun toString() : String {
    return type
  }
}

/**
 * Enum class which represents a possible message data types
 */
enum class MessageData(val data: String) {
  DATA_DATA("DATA"),
  DATA_HIDDEN("HIDDEN"),
  DATA_ACTION("ACTION");

  override fun toString(): String {
    return data
  }
}