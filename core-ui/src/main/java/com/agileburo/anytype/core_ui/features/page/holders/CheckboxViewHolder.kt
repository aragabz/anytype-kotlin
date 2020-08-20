package com.agileburo.anytype.core_ui.features.page.holders

import android.text.Editable
import android.view.View
import android.widget.ImageView
import androidx.core.view.updatePadding
import com.agileburo.anytype.core_ui.R
import com.agileburo.anytype.core_ui.common.ThemeColor
import com.agileburo.anytype.core_ui.extensions.color
import com.agileburo.anytype.core_ui.features.page.BlockTextEvent
import com.agileburo.anytype.core_ui.features.page.BlockView
import com.agileburo.anytype.core_ui.features.page.marks
import com.agileburo.anytype.core_ui.features.page.models.BlockTextViewHolder
import com.agileburo.anytype.core_ui.features.page.models.Item
import com.agileburo.anytype.core_ui.tools.*
import com.agileburo.anytype.core_ui.widgets.text.TextInputWidget
import com.agileburo.anytype.core_utils.ext.dimen
import kotlinx.android.synthetic.main.item_block_checkbox.view.*

class CheckboxViewHolder constructor(
    view: View,
    textWatcher: BlockTextWatcher,
    mentionWatcher: BlockTextMentionWatcher,
    backspaceWatcher: BlockTextBackspaceWatcher,
    enterWatcher: BlockTextEnterWatcher,
    actionMenu: BlockTextMenu
) : BlockTextViewHolder(
    view, textWatcher, mentionWatcher, backspaceWatcher, enterWatcher, actionMenu
) {

    private val checkbox: ImageView = itemView.checkboxIcon
    override val content: TextInputWidget get() = itemView.checkboxContent

    fun bind(
        item: Item,
        isChecked: Boolean,
        event: (BlockTextEvent.CheckboxEvent) -> Unit
    ) {
        checkbox.isActivated = isChecked
        setTextColor(item.color, isChecked)
        checkbox.setOnClickListener { onCheckClick(item.color, event, item.id, item.mode) }
    }

    override fun indentize(indent: Int) {
        checkbox.updatePadding(left = indent * dimen(R.dimen.indent))
    }

    override fun getMentionSizes(): Pair<Int, Int> = with(itemView) {
        Pair(
            first = resources.getDimensionPixelSize(R.dimen.mention_span_image_size_default),
            second = resources.getDimensionPixelSize(R.dimen.mention_span_image_padding_default)
        )
    }

    /**
     * Override default behaviour, use [setTextColor(textColor: String?, isSelected: Boolean)]
     */
    override fun setTextColor(textColor: String?) {}

    private fun setTextColor(textColor: String?, isSelected: Boolean) {
        if (isSelected) {
            content.setTextColor(itemView.context.color(R.color.checkbox_state_checked))
        } else {
            if (textColor != null)
                content.setTextColor(
                    ThemeColor.values().first { value -> value.title == textColor }.text
                )
            else
                content.setTextColor(content.context.color(R.color.black))
        }
    }

    private fun onCheckClick(
        textColor: String?,
        event: (BlockTextEvent.CheckboxEvent) -> Unit,
        id: String,
        mode: BlockView.Mode
    ) {
        if (mode == BlockView.Mode.EDIT) {
            checkbox.isActivated = !checkbox.isActivated
            setTextColor(
                textColor = textColor,
                isSelected = checkbox.isActivated
            )
            event.invoke(BlockTextEvent.CheckboxEvent.Click(id))
        }
    }

    override fun onTextEvent(
        event: (BlockTextEvent) -> Unit,
        id: String,
        item: Item,
        editable: Editable
    ) {
        item.apply {
            text = editable.toString()
            marks = editable.marks()
        }
        event(BlockTextEvent.TextEvent.Pattern(id, item))
    }

    override fun select(isSelected: Boolean) {
        TODO("Not yet implemented")
    }
}