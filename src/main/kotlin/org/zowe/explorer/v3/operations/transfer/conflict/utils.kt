/*
 * Copyright (c) 2024-2025 IBA Group.
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

package org.zowe.explorer.v3.operations.transfer.conflict

import org.zowe.explorer.explorer.ui.ConflictResolution

/**
 * Creates an HTML message from an items list
 * Message structure:
 *   startMessage
 *     Next, a string is constructed from the items list.
 *     Elements are added to the string until its length does not exceed the limit.
 *     If not all elements were added to the string, then "and more..." is added to the end of the string
 *   finishMessage.
 * @param startMessage beginning of the message
 * @param items list of items to display
 * @param finishMessage end of message
 * @param limit the maximum allowed length for a converted list of elements.
 * @return created HTML message
 */
fun createHtmlMessageWithItemsList(
  startMessage: String, items: List<String>, finishMessage: String, limit: Int = 130
): String {
  val pTag = "<p style=\"margin-left: 10px\">"
  val itemsMerged = items.joinToString(", ")
  val result = if (itemsMerged.length > limit)
    itemsMerged.substring(0, limit - 3).plus("...</p>${pTag}and more...")
  else
    itemsMerged
  val itemsString = pTag.plus(result).plus("</p>")
  return "<html><span>$startMessage\n</span>\n$itemsString\n<span>$finishMessage</span></html>"
}

/**
 * Merge conflicts between source files and conflicts between source files and destination files
 * because some resolved conflicts may overwrite others.
 * If the source file with new name should overwrite the file in the destination,
 * then two options are set for it (new name and overwrite).
 */
fun mergeConflictResolutions(
  sourceConflictResolutions: List<ConflictResolution>, destinationConflictResolutions: List<ConflictResolution>
): List<ConflictResolution> {
  destinationConflictResolutions.forEach { destConflictResolution ->
    sourceConflictResolutions
      .firstOrNull {
        it.sourceFile == destConflictResolution.sourceFile &&
          it.destinationFile == destConflictResolution.destinationFile
      }
      ?.let { sourceConflictResolution ->
        val newName = sourceConflictResolution.newName
        if (newName != null && destConflictResolution.shouldOverwrite()) {
          destConflictResolution.resolveByUsingNewNameWithOverwrite(newName)
        }
      }
  }

  val nonOverriddenSourceConflictResolutions = sourceConflictResolutions.filter { sourceConflictResolution ->
    destinationConflictResolutions.none {
      it.sourceFile == sourceConflictResolution.sourceFile &&
        it.destinationFile == sourceConflictResolution.destinationFile
    }
  }

  return destinationConflictResolutions.plus(nonOverriddenSourceConflictResolutions)
}
