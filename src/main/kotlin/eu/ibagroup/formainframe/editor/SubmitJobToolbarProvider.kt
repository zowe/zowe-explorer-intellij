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

package eu.ibagroup.formainframe.editor

import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider

private const val ACTION_GROUP = "eu.ibagroup.formainframe.explorer.actions.SubmitJobToolbarActionGroup"

/**
 * Class for displaying the submit button when edit JCL in editor
 */
class SubmitJobToolbarProvider : AbstractFloatingToolbarProvider(ACTION_GROUP) {
  override val autoHideable = true
  override val priority = 1
}
