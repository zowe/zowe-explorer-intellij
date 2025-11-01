/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.components.files

import org.zowe.explorer.v3.components.files.operations.LoadDatasetMaskNodesOperation
import org.zowe.explorer.v3.components.files.operations.LoadDatasetMaskNodesOperationData
import org.zowe.explorer.v3.components.files.operations.RefreshDatasetMaskNodesOperation
import org.zowe.explorer.v3.components.files.operations.RefreshDatasetMaskNodesOperationData
import org.zowe.explorer.v3.components.formDsBasePathFromHost
import org.zowe.explorer.v3.icons.ZoweExplorerIcons
import org.zowe.explorer.v3.state.config.ConfigType
import org.zowe.explorer.v3.state.config.cache.ConfigCacheService
import org.zowe.explorer.v3.state.config.connection.HttpConnectionConfig
import org.zowe.explorer.v3.tree.nodes.ExplorerTreeNode
import org.zowe.explorer.v3.tree.nodes.PlainFilterNodeDescriptor

// TODO: doc
class DatasetMaskNodeDescriptor(
  displayName: String,
  connectionConfigUuid: String
) : PlainFilterNodeDescriptor(
  displayName,
  basePath = formDsBasePathFromConnectionConfig(connectionConfigUuid),
  "Data set mask",
  ZoweExplorerIcons.datasetMask,
  connectionConfigUuid
) {
  companion object {
    fun formDsBasePathFromConnectionConfig(connectionConfigUuid: String): List<String> {
      val connectionConfig = ConfigCacheService.getService()
        .getConfigFromCache(ConfigType.HTTP_CONNECTION_CONFIG_V1, connectionConfigUuid)
        ?: throw Exception("Connection config is not found for node $this")
      val host = (connectionConfig as HttpConnectionConfig).host
      return formDsBasePathFromHost(host)
    }
  }

  override fun generateLoadNodesOperation(node: ExplorerTreeNode): LoadDatasetMaskNodesOperation {
    return LoadDatasetMaskNodesOperation(
      LoadDatasetMaskNodesOperationData(node, basePath, fetchFilter)
    )
  }

  override fun generateRefreshNodesOperation(node: ExplorerTreeNode): RefreshDatasetMaskNodesOperation {
    return RefreshDatasetMaskNodesOperation(
      RefreshDatasetMaskNodesOperationData(node, basePath, fetchFilter)
    )
  }

  // TODO: move to validation
  private fun isOnlyWildcards(input: String): Boolean {
    return input.replace("*", "")
      .replace(".", "")
      .isEmpty()
  }

  /**
   * TODO: doc
   * E.g.: SYS1 -> SYS1.**
   */
  private fun normalizeFilter(): String {
    return if (!fetchFilter.contains('*') && !fetchFilter.contains('%')) "$fetchFilter.**"
      else fetchFilter
  }

  /**
   * TODO: doc
   * Check a single qualifier with wildcards matches with the value
   */
  private fun checkMatchesQualifier(pattern: String, value: String): Boolean {
    val regexPattern = buildString {
      append('^')
      var i = 0
      while (i < pattern.length) {
        when (pattern[i]) {
          '*' -> append(".*") // * = zero or more symbols
          '%' -> append(".") // % = exactly one symbol
          else -> {
            val char = pattern[i]
            if (char in "\\[]{}()+?.^$|") {
              append("\\")
            }
            append(char)
          }
        }
        i++
      }
      append('$')
    }
    return Regex(regexPattern, RegexOption.IGNORE_CASE).matches(value)
  }

  /**
   * TODO: doc
   * Recursively compare data set qualifiers with the pattern
   */
  private fun matchQualifiers(
    pattern: List<String>,
    dataset: List<String>,
    patternIdx: Int = 0,
    datasetIdx: Int = 0
  ): Boolean {
    // Both lists are ended - success
    if (patternIdx >= pattern.size && datasetIdx >= dataset.size) {
      return true
    }

    // Pattern is ended, but the data set is not - considering as '**' at the end
    if (patternIdx >= pattern.size) {
      return true
    }

    // Data set qualifiers are ended, but patterns are not
    if (datasetIdx >= dataset.size) {
      // Check if only '**' are left
      for (i in patternIdx until pattern.size) {
        if (pattern[i] != "**") {
          return false
        }
      }
      return true
    }

    return when (val currentPattern = pattern[patternIdx]) {
      // ** - zero or more qualifiers
      "**" -> {
        // Try to match as many qualifiers as possible
        for (skip in 0..dataset.size - datasetIdx) {
          if (matchQualifiers(pattern, dataset, patternIdx + 1, datasetIdx + skip)) {
            return true
          }
        }
        false
      }

      // * - one qualifier minimum
      "*" -> {
        // Data set must have at least one qualifier in this position
        matchQualifiers(pattern, dataset, patternIdx + 1, datasetIdx + 1)
      }

      // Plain qualifier with possible wildcards inside
      else -> {
        checkMatchesQualifier(currentPattern, dataset[datasetIdx]) &&
          matchQualifiers(pattern, dataset, patternIdx + 1, datasetIdx + 1)
      }
    }
  }

// TODO: move to validation on the filter creation
//    if (datasetFilter.isEmpty()) {
//      throw Exception("Data set mask cannot be empty")
//    }
//    if (isOnlyWildcards(datasetFilter)) {
//      throw Exception("Invalid data set mask '$datasetFilter': it cannot consist only of '*' and '**'")
//    }

  /**
   * Check if a data set name matches the filter
   * @param elemName the data set name to check
   * @return true if matches the filter, false otherwise
   */
  override fun checkMatchesFilter(elemName: String): Boolean {
    val patternQualifiers = normalizeFilter().split('.')
    val datasetQualifiers = elemName.split('.')
    return matchQualifiers(patternQualifiers, datasetQualifiers)
  }
}