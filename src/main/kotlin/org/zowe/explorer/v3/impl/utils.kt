/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.impl

import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.ui.SimpleTextAttributes

// TODO: move where needed, when needed

/**
 * TODO: doc
 * Validate each separate qualifier
 * @return true if valid, false otherwise
 */
private fun validateQualifier(qualifier: String, index: Int): Result<Any> {
  // Empty qualifier (e.g., "SYS1..TEST")
  if (qualifier.isEmpty()) {
    return Result.failure(Exception("Empty qualifier at position ${index + 1}"))
  }

  // Special case: **
  if (qualifier == "**") {
    return Result.success("'**' qualifier is valid")
  }

  // ** should not be a part of a qualifier
  if (qualifier.contains("**")) {
    return Result.failure(
      Exception("Double asterisk (**) can only be a standalone qualifier, found in: '$qualifier'")
    )
  }

  // Single * is allowed as a qualifier
  if (qualifier == "*") {
    return Result.success("'*' qualifier is valid")
  }

  val VALID_CHARS_REGEX = Regex("^[A-Z0-9@#$*%]+$", RegexOption.IGNORE_CASE)
  // Check allowed characters
  if (!VALID_CHARS_REGEX.matches(qualifier)) {
    return Result.failure(
      Exception(
        "Qualifier '$qualifier' contains invalid characters. Only A-Z, 0-9, @, #, $, *, % are allowed"
      )
    )
  }

  // Check that the first character is not a digit
  if (qualifier.first().isDigit()) {
    return Result.failure(Exception("Qualifier '$qualifier' cannot start with a digit"))
  }

  // Check for % signs (no more than 8 subsequent)
  if (Regex("(%{9,})").find(qualifier) != null) {
    return Result.failure(Exception("Qualifier '$qualifier' has more than 8 consecutive percent signs"))
  }

  val MAX_QUALIFIER_LENGTH = 8
  // Check for the qualifier length
  if (qualifier.length > MAX_QUALIFIER_LENGTH) {
    return Result.failure(
      Exception("Qualifier '$qualifier' exceeds maximum length of $MAX_QUALIFIER_LENGTH characters")
    )
  }

  return Result.success("Qualifier is valid")
}

/**
 * Form a base path from the provided [host]
 * @param host the host to produce the base path from
 * @param systemPathType the type of the elements system to produce the base path for
 * @param systemPathName the name of the elements system to produce the base path for
 * @return the formed base path as the list of strings
 */
fun formBasePathFromHost(host: String, systemPathType: String, systemPathName: String): List<String> {
  return listOf(host, systemPathType, systemPathName)
}

// TODO: doc
fun formUssBasePathFromHost(host: String): List<String> {
  return formBasePathFromHost(host, "files", "uss")
}

// TODO: doc
fun formDsBasePathFromHost(host: String): List<String> {
  return formBasePathFromHost(host, "files", "ds")
}

// TODO: doc
fun formJesBasePathFromHost(host: String): List<String> {
  return formBasePathFromHost(host, "jes", "jobs")
}

// TODO: doc
fun formJobFilterName(prefix: String, owner: String, jobId: String): String {
  return if (jobId.isNotEmpty()) {
    "Job ID: <b>$jobId</b>"
  } else {
    "Prefix: <b>$prefix</b> | Owner: <b>$owner</b>"
  }
}

/**
 * Color the text when there is respective supported HTML markup is present.
 * Currently supported:
 * - b - for bold parts
 * @param textToSplit the text to split to parts to color
 * @return the colored text fragments
 */
fun splitToColoredParts(textToSplit: String): List<PresentableNodeDescriptor.ColoredFragment> {
  val regex = Regex("<b>(.*?)</b>")
  val parts = mutableListOf<PresentableNodeDescriptor.ColoredFragment>()
  var lastIndex = 0

  for (match in regex.findAll(textToSplit)) {
    if (match.range.first > lastIndex) {
      parts.add(
        PresentableNodeDescriptor.ColoredFragment(
          textToSplit.substring(lastIndex, match.range.first),
          SimpleTextAttributes.REGULAR_ATTRIBUTES
        )
      )
    }
    parts.add(
      PresentableNodeDescriptor.ColoredFragment(
        match.groupValues[1],
        SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
      )
    )
    lastIndex = match.range.last + 1
  }

  if (lastIndex < textToSplit.length) {
    parts.add(
      PresentableNodeDescriptor.ColoredFragment(
        textToSplit.substring(lastIndex),
        SimpleTextAttributes.REGULAR_ATTRIBUTES
      )
    )
  }

  return parts
}
