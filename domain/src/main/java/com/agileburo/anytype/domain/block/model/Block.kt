package com.agileburo.anytype.domain.block.model

import com.agileburo.anytype.domain.block.model.Block.Content.Text.Mark
import com.agileburo.anytype.domain.block.model.Block.Content.Text.Style
import com.agileburo.anytype.domain.common.Hash
import com.agileburo.anytype.domain.common.Id
import com.agileburo.anytype.domain.common.Url

/**
 * Represents block as basic data structure.
 * @property id block's id
 * @property children block's children ids
 * @property fields block's fields
 * @property content block's content
 */
data class Block(
    val id: String,
    val children: List<String>,
    val content: Content,
    val fields: Fields
) {

    /**
     * Block fields containing useful block properties.
     * @property map map containing fields
     */
    data class Fields(val map: Map<String?, Any?>) {

        val name: String by map
        val icon: String? by map

        fun hasName() = map.containsKey(NAME_KEY)

        companion object {
            fun empty(): Fields = Fields(emptyMap())
            const val NAME_KEY = "name"
        }
    }

    /**
     * Block's content.
     */
    sealed class Content {

        fun asText() = this as Text
        fun asLink() = this as Link
        fun asDashboard() = this as Dashboard
        fun asDivider() = this as Divider
        fun asFile() = this as File

        /**
         * Textual block.
         * @property text content text
         * @property marks markup related to [text],
         * @property isChecked whether this block is checked or not (see [Style.CHECKBOX])
         * @property color text color, which should be applied to the whole block (as opposed to [Mark.Type.TEXT_COLOR])
         * @property backgroundColor background color for the whole block, as opposed to [Mark.Type.BACKGROUND_COLOR]
         */
        data class Text(
            val text: String,
            val style: Style,
            val marks: List<Mark>,
            val isChecked: Boolean? = null,
            val color: String? = null,
            val backgroundColor: String? = null
        ) : Content() {

            /**
             * Toggles checked/unchecked state.
             * Does not modify this instance's checked/unchecked state (preserves immutability)
             * @return new checked/unchecked state without modifying
             */
            fun toggleCheck(): Boolean = isChecked == null || isChecked == false

            /**
             * @return true if this is a title block.
             */
            fun isTitle() = style == Style.TITLE

            /**
             * @return true if this text block is a list item.
             */
            fun isList(): Boolean {
                return style == Style.BULLET || style == Style.CHECKBOX || style == Style.NUMBERED
            }

            /**
             * Mark as a part of markup.
             * @property type markup type
             * @property param optional parameter (i.e. text color, url, etc)
             * @property range text range for markup (start == start char index, end == end char index + 1).
             */
            data class Mark(
                val range: IntRange,
                val type: Type,
                val param: Any? = null
            ) {
                enum class Type {
                    STRIKETHROUGH,
                    KEYBOARD,
                    ITALIC,
                    BOLD,
                    UNDERSCORED,
                    LINK,
                    TEXT_COLOR,
                    BACKGROUND_COLOR
                }
            }

            enum class Style {
                P, H1, H2, H3, H4, TITLE, QUOTE, CODE_SNIPPET, BULLET, NUMBERED, TOGGLE, CHECKBOX
            }
        }

        data class Layout(val type: Type) : Content() {
            enum class Type { ROW, COLUMN }
        }

        data class Image(
            val path: String
        ) : Content()

        data class Dashboard(val type: Type) : Content() {
            enum class Type { MAIN_SCREEN, ARCHIVE }
        }

        data class Page(val style: Style) : Content() {
            enum class Style { EMPTY, TASK, SET }
        }

        /**
         * A link to some other block.
         * @property target id of the target block
         * @property type type of the link
         * @property fields fields storing additional properties
         */
        data class Link(
            val target: Id,
            val type: Type,
            val fields: Fields
        ) : Content() {
            enum class Type { PAGE, DATA_VIEW, DASHBOARD, ARCHIVE }
        }

        /**
         * Page icon.
         * @property name conventional emoji short name.
         */
        data class Icon(
            val name: String
        ) : Content()

        /**
         * File block.
         * @property hash file hash
         * @property name filename
         * @property mime mime type
         * @property size file size (in bytes)
         * @property type file type
         * @property state file state
         */
        data class File(
            val hash: String? = null,
            val name: String? = null,
            val mime: String? = null,
            val size: Long? = null,
            val type: Type? = null,
            val state: State? = null
        ) : Content() {
            enum class Type { NONE, FILE, IMAGE, VIDEO }
            enum class State { EMPTY, UPLOADING, DONE, ERROR }
        }

        /**
         * @property url url associated with this bookmark
         * @property title optional bookmark title
         * @property description optional bookmark's content description
         * @property image optional hash of bookmark's image
         * @property favicon optional hash of bookmark's favicon
         */
        data class Bookmark(
            val url: Url,
            val title: String?,
            val description: String?,
            val image: Hash?,
            val favicon: Hash?
        ) : Content()

        object Divider : Content()
    }

    /**
     * Block prototype used as a model or a blueprint for a block to create.
     */
    sealed class Prototype {
        /**
         * Prototype of the textual block.
         * @param style style for a block to create
         */
        data class Text(
            val style: Content.Text.Style
        ) : Prototype()

        data class Page(
            val style: Content.Page.Style
        ) : Prototype()

        object Divider : Prototype()

        data class File(
            val type: Content.File.Type,
            val state: Content.File.State
        ) : Prototype()
    }
}