package com.agileburo.anytype.ui.page.modals.actions

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.agileburo.anytype.R
import com.agileburo.anytype.core_ui.common.toSpannable
import com.agileburo.anytype.core_ui.extensions.color
import com.agileburo.anytype.core_ui.features.page.BlockView
import com.agileburo.anytype.core_utils.ext.setReadOnly

class BulletedBlockActionToolbar : BlockActionToolbar() {

    lateinit var block: BlockView.Bulleted

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        block = arguments?.getParcelable(ARG_BLOCK)!!
    }

    override fun blockLayout() = R.layout.item_block_bulleted
    override fun getBlock(): BlockView = block

    override fun initUi(view: View) {
        view.findViewById<EditText>(R.id.bulletedListContent).apply {
            setReadOnly(true)
            if (block.marks.isNotEmpty()) {
                setText(block.toSpannable(), TextView.BufferType.SPANNABLE)
            } else {
                setText(block.text)
            }
            val textColor = block.color
            if (textColor != null) {
                setBlockTextColor(this, textColor)
            } else {
                setTextColor(context.color(R.color.black))
            }
        }
    }
}