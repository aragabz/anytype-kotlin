package com.anytypeio.anytype.core_ui.features.editor.holders.placeholders

import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.core_ui.databinding.ItemBlockMediaPlaceholderBinding
import com.anytypeio.anytype.presentation.editor.editor.listener.ListenerType

class BookmarkPlaceholder(binding: ItemBlockMediaPlaceholderBinding) : MediaPlaceholder(binding) {

    override fun placeholderClick(target: String, clicked: (ListenerType) -> Unit) {
        clicked(ListenerType.Bookmark.Placeholder(target))
    }

    override fun setup() {
        title.text = itemView.resources.getString(R.string.hint_add_a_web_bookmark)
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_bookmark_placeholder, 0, 0, 0)
    }
}