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

package eu.ibagroup.formainframe.explorer.hints

import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.Component
import java.awt.Point
import javax.swing.event.HyperlinkListener

/**
 * Class which represents hint as popup tooltip (balloon) for user.
 */
class Hint (
    val text: String,
    val messageType: MessageType = MessageType.INFO,
    val hyperlinkListener: HyperlinkListener? = null
) {

    var position = Balloon.Position.below

    private var balloon: Balloon? = null

    private var hideOnClickOutside = true

    private var hideOnLinkClick = true

    /** Sets whether to hide hint when clicked outside of it. */
    fun setHideOnClickOutside(hide: Boolean): Hint {
        hideOnClickOutside = hide
        return this
    }

    /** Sets whether to hide hint when clicked on a link. */
    fun setHideOnLinkClick(hide: Boolean): Hint {
        hideOnLinkClick = hide
        return this
    }

    /**
     * Creates a balloon and sets parameters.
     * Changes "\n" to tag <br>.
     * @return instance of [Balloon].
     */
    private fun createBalloon(): Balloon {
        val text = text.replace("\n", "<br>")
        return JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(text, messageType, hyperlinkListener)
            .setHideOnClickOutside(hideOnClickOutside)
            .setHideOnLinkClick(hideOnLinkClick)
            .createBalloon()
    }

    /**
     * Shows a hint on the component at the specified point.
     * @component component on which the hint will be shown.
     * @pointProvider function that provides a point to show the hint.
     */
    fun show(component: Component, pointProvider: (Component, Balloon) -> Point) {
        val balloon = createBalloon().also { this.balloon = it }
        val relativePoint = RelativePoint(component, pointProvider(component, balloon))
        balloon.show(relativePoint, position)
    }

    companion object {
        @JvmField
        val TOP_MIDDLE: (Component, Any) -> Point = { it, _ -> Point(it.width / 2, 0) }

        @JvmField
        val LEFT_MIDDLE: (Component, Any) -> Point = { it, _ -> Point(0, it.height / 2) }

        @JvmField
        val RIGHT_MIDDLE: (Component, Any) -> Point = { it, _ -> Point(it.width, it.height / 2) }

        @JvmField
        val BOTTOM_MIDDLE: (Component, Any) -> Point = { it, _ -> Point(it.width / 2, it.height) }

        @JvmField
        val BOTTOM_LEFT: (Component, Any) -> Point = { it, _ -> Point(0, it.height) }
    }
}
