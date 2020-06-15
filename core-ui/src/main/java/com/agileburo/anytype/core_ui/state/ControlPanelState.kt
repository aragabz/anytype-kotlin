package com.agileburo.anytype.core_ui.state

import com.agileburo.anytype.core_ui.common.Alignment
import com.agileburo.anytype.core_ui.features.page.styling.StylingMode
import com.agileburo.anytype.core_ui.features.page.styling.StylingType

/**
 * Control panels are UI-elements that allow user to interact with blocks on a page.
 * Each panel is currently represented as a toolbar.
 * @property focus block currently associated with the control panel (if not present, control panel is not active)
 * @property mainToolbar block-toolbar state (main toolbar state)
 * @property stylingToolbar styling toolbar state
 */
data class ControlPanelState(
    val focus: Focus? = null,
    val mainToolbar: Toolbar.Main,
    val stylingToolbar: Toolbar.Styling,
    val multiSelect: Toolbar.MultiSelect
) {

    fun isNotVisible(): Boolean = !mainToolbar.isVisible

    sealed class Toolbar {

        /**
         * General property that defines whether this toolbar is visible or not.
         */
        abstract val isVisible: Boolean

        /**
         * Main toolbar allowing user-interface for CRUD-operations on block/page content.
         * @property isVisible defines whether the toolbar is visible or not
         */
        data class Main(
            override val isVisible: Boolean
        ) : Toolbar()

        /**
         * Basic color toolbar state.
         * @property isVisible defines whether the toolbar is visible or not
         */
        data class Styling(
            val target: Target? = null,
            override val isVisible: Boolean,
            val mode: StylingMode?,
            val type: StylingType?
        ) : Toolbar() {

            data class Target(
                val text: String,
                val color: String?,
                val background: String?,
                val alignment: Alignment?,
                val isBold: Boolean,
                val isItalic: Boolean,
                val isCode: Boolean,
                val isStrikethrough: Boolean
            )
        }

        data class MultiSelect(
            override val isVisible: Boolean
        ) : Toolbar()
    }

    /**
     * Block currently associated with this panel.
     * @property id id of the focused block
     */
    data class Focus(
        val id: String,
        val type: Type
    ) {
        enum class Type {
            P, H1, H2, H3, H4, TITLE, QUOTE, CODE_SNIPPET, BULLET, NUMBERED, TOGGLE, CHECKBOX, BOOKMARK
        }
    }

    companion object {

        /**
         * Factory function for creating initial state.
         */
        fun init(): ControlPanelState = ControlPanelState(
            mainToolbar = Toolbar.Main(
                isVisible = false
            ),
            multiSelect = Toolbar.MultiSelect(
                isVisible = false
            ),
            stylingToolbar = Toolbar.Styling(
                isVisible = false,
                type = null,
                mode = null
            ),
            focus = null
        )
    }
}