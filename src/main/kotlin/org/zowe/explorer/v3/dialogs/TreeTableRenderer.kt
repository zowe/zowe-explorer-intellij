/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.explorer.v3.dialogs

import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * Renders a hierarchical key-value tree inside an IntelliJ Kotlin UI DSL [Panel].
 *
 * Each tree node is described by a [RowModel] — a collapsible section with a title,
 * a collapsed-state summary, and a list of key-value entries. Entries can hold either
 * a plain [RowValue.Text] or a [RowValue.Nested] sub-tree, allowing arbitrary depth.
 *
 * Visual features:
 * - Indentation guide lines connecting parent and child rows
 * - Hover highlighting on every row
 * - Collapsible toggle (`+`/`-`) with a summary shown when collapsed
 * - Stripe background on toggle rows for visual grouping
 * - Fixed-width key column sized to the widest key in each section
 *
 * Usage — call [renderRowModel] as an extension on a UI DSL [Panel]:
 * ```
 * val renderer = TreeTableRenderer(JBUI.scale(24), JBUI.scale(16), guideColor, stripeColor)
 * panel {
 *   with(renderer) {
 *     renderRowModel(myModel, isFirst = true)
 *   }
 * }
 * ```
 *
 * @param tableScale base size in pixels for row height, indent step, and toggle width
 * @param gapsScale extra padding added to the key column width
 * @param guideColor color of the vertical/horizontal indent guide lines
 * @param stripeColor background color of collapsible toggle rows
 */
class TreeTableRenderer(
  private val tableScale: Int,
  private val gapsScale: Int,
  private val guideColor: Color,
  private val stripeColor: Color
) {

  /** A value in a tree-table row — either a leaf [Text] or a [Nested] sub-tree. */
  sealed class RowValue {
    data class Text(val text: String) : RowValue()
    data class Nested(val model: RowModel) : RowValue()
  }

  /**
   * Describes a collapsible section in the tree table.
   *
   * @param title section header displayed when expanded and on the toggle row
   * @param summaryProvider produces a one-line summary from the collected leaf values when collapsed
   * @param entries ordered key-value pairs rendered as child rows
   * @param defaultExpanded whether the section starts expanded
   */
  data class RowModel(
    val title: String,
    val summaryProvider: (Map<String, String>) -> String,
    val entries: List<Pair<String, RowValue>>,
    val defaultExpanded: Boolean = false
  ) {
    fun directKeys(): List<String> = entries.map { it.first }
  }

  private abstract class HoverMouseAdapter(private val component: JComponent) : MouseAdapter() {
    var hovered = false
      private set
    var locked = false
      set(v) {
        field = v
        if (!v && component.mousePosition == null && hovered) {
          hovered = false
          component.repaint()
        }
      }

    override fun mouseEntered(e: MouseEvent) {
      if (!hovered) {
        hovered = true
        component.repaint()
      }
    }

    override fun mouseExited(e: MouseEvent) {
      if (locked) return
      if (component.mousePosition == null && hovered) {
        hovered = false
        component.repaint()
      }
    }
  }

  private open class TableRowPanel(private val minRowHeight: Int) : JPanel(BorderLayout(0, 0)) {
    override fun getPreferredSize(): Dimension {
      val pref = super.getPreferredSize()
      return Dimension(pref.width, maxOf(pref.height, minRowHeight))
    }

    override fun getMaximumSize() = Dimension(Int.MAX_VALUE, preferredSize.height)
  }

  private class MutablePredicate(initial: Boolean = false) : ComponentPredicate() {
    private val listeners = mutableListOf<(Boolean) -> Unit>()
    var value = initial
      set(v) {
        field = v
        listeners.forEach { it(v) }
      }

    override fun invoke() = value
    override fun addListener(listener: (Boolean) -> Unit) { listeners.add(listener) }

    fun and(other: ComponentPredicate): ComponentPredicate {
      val self = this
      return object : ComponentPredicate() {
        override fun invoke() = self() && other()
        override fun addListener(listener: (Boolean) -> Unit) {
          self.addListener { listener(invoke()) }
          other.addListener { listener(invoke()) }
        }
      }
    }
  }

  companion object {
    private fun installRecursively(container: java.awt.Container, listener: java.awt.event.MouseListener) {
      for (i in 0 until container.componentCount) {
        val child = container.getComponent(i)
        child.addMouseListener(listener)
        if (child is java.awt.Container) installRecursively(child, listener)
      }
    }

    private fun paintComponentHoverable(g: Graphics, hovered: Boolean, width: Int, height: Int) {
      if (hovered) {
        val g2 = g.create() as Graphics2D
        g2.color = JBUI.CurrentTheme.ActionButton.hoverBackground()
        g2.composite = AlphaComposite.SrcOver
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
  }

  private fun keyColumnWidth(model: RowModel): Int {
    val keys = model.directKeys()
    if (keys.isEmpty()) return 0
    return JLabel().let { tmp ->
      val fm = tmp.getFontMetrics(tmp.font)
      keys.maxOf { fm.stringWidth(it) }
    } + gapsScale
  }

  private fun paintGuideLines(g: Graphics, indentLevel: Int, isFirstChild: Boolean, isLastChild: Boolean, height: Int) {
    if (indentLevel <= 0) return
    val g2 = g.create() as Graphics2D
    g2.color = guideColor
    g2.stroke = java.awt.BasicStroke(JBUIScale.scale(1.0f))
    val x = (indentLevel - 1) * tableScale + tableScale / 2
    val startY = if (isFirstChild) tableScale / 4 else 0
    if (isLastChild) {
      g2.drawLine(x, startY, x, height / 2)
      g2.drawLine(x, height / 2, x + tableScale / 4, height / 2)
    } else {
      g2.drawLine(x, startY, x, height)
    }
    g2.dispose()
  }

  private fun styledRow(
    key: String,
    valueComponent: JComponent,
    indentLevel: Int = 1,
    keyColumnWidth: Int,
    isLastChild: Boolean = false,
    isFirstChild: Boolean = false
  ): JComponent {
    val indent = tableScale * indentLevel
    val contentPad = tableScale / 3
    val renderer = this
    return object : TableRowPanel(tableScale) {
      private val hoverAdapter = object : HoverMouseAdapter(this) {}

      init {
        isOpaque = false
        border = JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        add(
          JPanel().apply {
            isOpaque = false
            preferredSize = Dimension(indent + contentPad, 0)
          },
          BorderLayout.WEST
        )
        add(
          JPanel(BorderLayout(0, 0)).apply {
            isOpaque = false
            add(
              JLabel(key).apply {
                border = JBUI.Borders.empty(0, 4)
                preferredSize = Dimension(keyColumnWidth, 0)
                if (indentLevel <= 1) font = font.deriveFont(Font.BOLD)
              },
              BorderLayout.WEST
            )
            add(valueComponent, BorderLayout.CENTER)
          },
          BorderLayout.CENTER
        )
        addMouseListener(hoverAdapter)
        installRecursively(this, hoverAdapter)
        putClientProperty("hoverAdapter", hoverAdapter)
      }

      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        paintComponentHoverable(g, hoverAdapter.hovered, width, height)
      }

      override fun paintChildren(g: Graphics) {
        super.paintChildren(g)
        renderer.paintGuideLines(g, indentLevel, isFirstChild, isLastChild, height)
        g.color = JBColor.border()
        val separatorX = indent + contentPad + keyColumnWidth
        g.drawLine(separatorX, 0, separatorX, height)
      }
    }
  }

  private fun toggleRow(
    title: String,
    summaryProvider: () -> String,
    indentLevel: Int,
    predicate: MutablePredicate,
    isFirst: Boolean = false,
    isFirstChild: Boolean = false
  ): JComponent {
    val indent = tableScale * indentLevel
    val toggleLabel = JLabel(if (predicate.value) "-" else "+").apply {
      horizontalAlignment = SwingConstants.CENTER
      preferredSize = Dimension(tableScale, 0)
    }
    val textLabel = object : JLabel(if (predicate.value) title else summaryProvider()) {
      override fun getPreferredSize(): Dimension {
        val s = super.getPreferredSize()
        return Dimension(JBUIScale.scale(40), s.height)
      }
      override fun getMinimumSize(): Dimension {
        val s = super.getMinimumSize()
        return Dimension(0, s.height)
      }
    }.apply {
      border = JBUI.Borders.empty(0, 4)
      if (indentLevel <= 1) font = font.deriveFont(Font.BOLD)
    }

    val renderer = this
    return object : TableRowPanel(tableScale) {
      private val hoverAdapter = object : HoverMouseAdapter(this) {
        override fun mouseClicked(e: MouseEvent) {
          predicate.value = !predicate.value
          toggleLabel.text = if (predicate.value) "-" else "+"
          textLabel.text = if (predicate.value) title else summaryProvider()
          repaint()
        }
      }

      init {
        isOpaque = false
        border = if (isFirst)
          JBUI.Borders.customLine(JBColor.border(), 1, 0, 1, 0)
        else
          JBUI.Borders.customLine(JBColor.border(), 0, 0, 1, 0)
        add(
          JPanel(BorderLayout(0, 0)).apply {
            isOpaque = false
            preferredSize = Dimension(indent + tableScale, 0)
            if (indentLevel > 0) {
              add(
                JPanel().apply {
                  isOpaque = false
                  preferredSize = Dimension(indent, 0)
                },
                BorderLayout.WEST
              )
            }
            add(toggleLabel, BorderLayout.CENTER)
          },
          BorderLayout.WEST
        )
        add(textLabel, BorderLayout.CENTER)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addMouseListener(hoverAdapter)
        predicate.addListener { repaint() }
      }

      override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.color = stripeColor
        g.fillRect(indent, 0, width - indent, height)
        paintComponentHoverable(g, hoverAdapter.hovered, width, height)
      }

      override fun paintChildren(g: Graphics) {
        super.paintChildren(g)
        renderer.paintGuideLines(g, indentLevel, isFirstChild, true, height)
      }
    }
  }

  /**
   * Recursively renders a [RowModel] as rows inside a Kotlin UI DSL [Panel].
   *
   * Call via `with(renderer) { renderRowModel(model) }`.
   *
   * @param model the tree section to render
   * @param indentLevel current nesting depth (0 = root)
   * @param parentVisible predicate controlling visibility of this section (used for nested collapse)
   * @param isFirst whether this is the first section in the tree (adds a top border)
   * @param isFirstChild whether this is the first child of a parent section (adjusts guide line start)
   */
  fun Panel.renderRowModel(
    model: RowModel,
    indentLevel: Int = 0,
    parentVisible: ComponentPredicate? = null,
    isFirst: Boolean = false,
    isFirstChild: Boolean = false
  ) {
    val expanded = MutablePredicate(model.defaultExpanded)
    val contentVisible = if (parentVisible != null) expanded.and(parentVisible) else expanded
    val colWidth = keyColumnWidth(model)

    val currentValues = mutableMapOf<String, String>()
    fun collectValues(entries: List<Pair<String, RowValue>>) {
      for ((key, value) in entries) {
        when (value) {
          is RowValue.Text -> currentValues[key] = value.text
          is RowValue.Nested -> collectValues(value.model.entries)
        }
      }
    }
    collectValues(model.entries)

    val toggle = toggleRow(model.title, { model.summaryProvider(currentValues) }, indentLevel, expanded, isFirst, isFirstChild)
    row {
      cell(toggle).align(AlignX.FILL)
    }.let { if (parentVisible != null) it.visibleIf(parentVisible) else it }

    val lastIdx = model.entries.lastIndex
    for ((idx, entry) in model.entries.withIndex()) {
      val (key, value) = entry
      val isLast = idx == lastIdx
      when (value) {
        is RowValue.Text -> {
          val labelComp = object : JLabel(value.text) {
            override fun getPreferredSize(): Dimension {
              val s = super.getPreferredSize()
              return Dimension(JBUIScale.scale(40), s.height)
            }
            override fun getMinimumSize(): Dimension {
              val s = super.getMinimumSize()
              return Dimension(0, s.height)
            }
          }.apply {
            border = JBUI.Borders.empty(0, tableScale / 3, 0, 4)
          }
          val stRow = styledRow(key, labelComp, indentLevel + 1, colWidth, isLast, idx == 0)
          row {
            cell(stRow).align(AlignX.FILL)
          }.visibleIf(contentVisible)
        }
        is RowValue.Nested -> {
          renderRowModel(value.model, indentLevel + 1, contentVisible, isFirstChild = idx == 0)
        }
      }
    }
  }
}