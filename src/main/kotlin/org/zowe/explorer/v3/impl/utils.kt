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
 * Form a base path from the provided [connectionProfile]
 * @param connectionProfile the connection profile path to produce the base path from
 * @param systemPathType the type of the elements system to produce the base path for
 * @param systemPathName the name of the elements system to produce the base path for
 * @return the formed base path as the list of strings
 */
fun formBasePath(connectionProfile: String, systemPathType: String, systemPathName: String): List<String> {
  return listOf(connectionProfile, systemPathType, systemPathName)
}

/**
 * Form a USS base path from the provided [connectionProfile]
 * @param connectionProfile the connection profile path
 * @return the formed base path for USS entries
 */
fun formUssBasePath(connectionProfile: String): List<String> {
  return formBasePath(connectionProfile, "files", "uss")
}

/**
 * Form a datasets base path from the provided [connectionProfile]
 * @param connectionProfile the connection profile path
 * @return the formed base path for dataset entries
 */
fun formDsBasePath(connectionProfile: String): List<String> {
  return formBasePath(connectionProfile, "files", "ds")
}

/**
 * Form a JES base path from the provided [connectionProfile]
 * @param connectionProfile the connection profile path
 * @return the formed base path for JES entries
 */
fun formJesBasePath(connectionProfile: String): List<String> {
  return formBasePath(connectionProfile, "jes", "jobs")
}

/**
 * Form a human-readable display label for a JES job filter.
 * When a job ID is present it takes precedence and is shown alone,
 * otherwise the prefix and owner are shown. The result is a display label only;
 * use [formJobFilterKey] for the filter's unique identity
 * @param prefix the job name prefix to search jobs by
 * @param owner the job owner to search jobs by
 * @param jobId the job ID to search a job by (mutually exclusive with prefix + owner)
 * @return the formed display label with HTML `b` markup
 */
fun formJobFilterName(prefix: String, owner: String, jobId: String): String {
  return if (jobId.isNotEmpty()) {
    "Job ID: <b>$jobId</b>"
  } else {
    "Prefix: <b>$prefix</b> | Owner: <b>$owner</b>"
  }
}

/**
 * Form a unique key for a JES job filter within a profile.
 * The key is assembled as `"owner.prefix(id)"`, matching the entry key used in the
 * `jobFilters` map of a `jes_ij` profile in the Zowe Team Config. Unlike
 * [formJobFilterName], it identifies a filter by all three of its fields, so filters
 * that share only some of them (e.g. the same job ID) remain distinct
 * @param owner the job owner to search jobs by
 * @param prefix the job name prefix to search jobs by
 * @param jobId the job ID to search a job by
 * @return the unique job filter key
 */
fun formJobFilterKey(owner: String, prefix: String, jobId: String): String {
  return "$owner.$prefix($jobId)"
}

/**
 * Form the base path under which a single JES job filter stores its fetched jobs.
 * Each filter gets its own path — the JES base path extended with the unique
 * [formJobFilterKey] — so that jobs fetched by one filter never leak into another
 * filter that shares the same connection profile (e.g. two `Prefix: *` filters with
 * different owners must not display each other's jobs)
 * @param connectionProfile the connection profile path
 * @param owner the job owner to search jobs by
 * @param prefix the job name prefix to search jobs by
 * @param jobId the job ID to search a job by
 * @return the formed, filter-specific base path for the filter's job entries
 */
fun formJesFilterBasePath(connectionProfile: String, owner: String, prefix: String, jobId: String): List<String> {
  return formJesBasePath(connectionProfile) + formJobFilterKey(owner, prefix, jobId)
}

/**
 * Form a tooltip for a JES job node from the combined job name+ID string and owner.
 * The [elemName] is expected in the `"JOBNAME(JOBID)"` format
 * @param elemName the combined job name and ID
 * @param owner the job owner
 * @return the formatted tooltip string
 */
fun formJobTooltip(elemName: String, owner: String): String {
  val openParen = elemName.indexOf('(')
  val closeParen = elemName.indexOf(')')
  val jobName = if (openParen > 0) elemName.substring(0, openParen) else elemName
  val jobId = if (openParen > 0 && closeParen > openParen) elemName.substring(openParen + 1, closeParen) else ""
  return "Job name: $jobName, ID: $jobId, owner: $owner"
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

/**
 * If [name] already exists in [existingNames], appends an incrementing suffix
 * (e.g. `profile_1`, `profile_2`) until a unique key is found
 */
fun resolveUniqueName(name: String, existingNames: Set<String>): String {
  if (name !in existingNames) return name
  var counter = 1
  while ("${name}_$counter" in existingNames) counter++
  return "${name}_$counter"
}
