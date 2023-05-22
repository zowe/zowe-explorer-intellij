/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
package eu.ibagroup.formainframe.config.connect.ui

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import java.awt.Dimension
import javax.swing.BoxLayout


/**
 * Configurable to display several other configurables inside one tab.
 * @param configurables - list of configurables to display.
 * @author Valiantsin Krus
 */
class CollectedConfigurable(
  val configurables: List<BoundSearchableConfigurable>
) : BoundSearchableConfigurable("Connections", "mainframe") {

  /** Min width of one configuration panel inside. */
  private val PANEL_MIN_HEIGHT = 240

  override fun createPanel(): DialogPanel {
    val mainPanel = DialogPanel()
    mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
    configurables.forEach {
      val p = it.createComponent()
      // TODO: use next line instead in v1.*.*-223 and greater:
      // p.minimumSize = Dimension(p.width, PANEL_MIN_HEIGHT)
      p?.minimumSize = Dimension(p?.width ?: 0, PANEL_MIN_HEIGHT)
      // TODO: use next line instead in v1.*.*-223 and greater:
      // p.preferredSize = Dimension(p.width, PANEL_MIN_HEIGHT)
      p?.preferredSize = Dimension(p?.width ?: 0, PANEL_MIN_HEIGHT)
      mainPanel.add(p)
    }
    return mainPanel
  }

  override fun apply() {
    configurables.forEach { it.apply() }
  }

  override fun reset() {
    configurables.forEach { it.reset() }
  }

  override fun cancel() {
    configurables.forEach { it.cancel() }
  }

  override fun isModified(): Boolean {
    return configurables.fold(false) { acc, c -> acc || c.isModified }
  }

  override fun disposeUIResources() {
    super.disposeUIResources()
    configurables.forEach { it.disposeUIResources() }
  }
}