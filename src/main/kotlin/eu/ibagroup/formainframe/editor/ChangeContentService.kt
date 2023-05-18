/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.editor

import com.intellij.openapi.editor.Editor

/**
 * Service that  should be used to process editor content of MF Files.
 * @author Valiantsin Krus
 */
interface ChangeContentService {

  /** Initializes service. Should be called somewhere at list once. */
  fun initialize()

  /**
   * Processes content of file in editor.
   * @param editor editor in which document of file is opened.
   * @param file source file that should be processed.
   */
  fun processMfContent(editor: Editor)
}