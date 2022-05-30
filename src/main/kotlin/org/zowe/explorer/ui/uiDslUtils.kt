/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */

package org.zowe.explorer.ui

import com.intellij.ui.layout.*
import java.awt.Dimension
import javax.swing.JLabel

fun LayoutBuilder.flowableRow(init: InnerCell.() -> Unit): Row {
  return row { cell { init() } }
}

fun Cell.labelOfWidth(text: String, width: Int): CellBuilder<JLabel> {
  return label(text).apply {
    component.preferredSize = Dimension(width, component.height)
  }
}
