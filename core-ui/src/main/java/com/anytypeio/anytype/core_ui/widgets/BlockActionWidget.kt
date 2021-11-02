package com.anytypeio.anytype.core_ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anytypeio.anytype.core_ui.R
import com.anytypeio.anytype.presentation.editor.editor.actions.ActionItemType
import kotlinx.android.synthetic.main.widget_block_action.view.*

class BlockActionWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : CardView(context, attrs) {

    var actionListener : (ActionItemType) -> Unit = {}

    private val blockActionAdapter = Adapter { action -> actionListener(action) }

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_block_action, this)
        blockActionRecycler.apply {
            adapter = blockActionAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    fun bind(actions: List<ActionItemType>) {
        blockActionAdapter.submitList(actions)
    }

    class Adapter(
        val onActionClicked: (ActionItemType) -> Unit
    ) : ListAdapter<ActionItemType, Adapter.VH>(Differ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(parent).apply {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onActionClicked(getItem(pos))
                }
            }
        }
        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

        class VH(parent: ViewGroup) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_block_action,
                parent,
                false
            )
        ) {

            private val title = itemView.findViewById<TextView>(R.id.tvActionTitle)
            private val icon = itemView.findViewById<ImageView>(R.id.blockActionIcon)

            fun bind(action: ActionItemType) {
                when(action) {
                    ActionItemType.Delete -> {
                        title.setText(R.string.toolbar_action_delete)
                        icon.setImageResource(R.drawable.ic_block_action_delete)
                    }
                    ActionItemType.Duplicate -> {
                        title.setText(R.string.toolbar_action_duplicate)
                        icon.setImageResource(R.drawable.ic_block_action_duplicate)
                    }
                    ActionItemType.AddBelow -> {
                        title.setText(R.string.block_action_add_below)
                        icon.setImageResource(R.drawable.ic_block_action_add_below)
                    }
                    ActionItemType.SAM -> {
                        title.setText(R.string.move)
                        icon.setImageResource(R.drawable.ic_block_action_move)
                    }
                    ActionItemType.Style -> {
                        title.setText(R.string.style)
                        icon.setImageResource(R.drawable.ic_block_action_style)
                    }
                    ActionItemType.MoveTo -> {
                        title.setText(R.string.move_to)
                        icon.setImageResource(R.drawable.ic_block_action_move_to)
                    }
                    ActionItemType.Download -> {
                        title.setText(R.string.download)
                        icon.setImageResource(R.drawable.ic_block_action_download)
                    }
                    else -> {
                        title.text = action::class.simpleName
                    }
                }
            }
        }
    }

    object Differ : DiffUtil.ItemCallback<ActionItemType>() {
        override fun areItemsTheSame(
            oldItem: ActionItemType,
            newItem: ActionItemType
        ): Boolean = oldItem == newItem
        override fun areContentsTheSame(
            oldItem: ActionItemType,
            newItem: ActionItemType
        ): Boolean = oldItem == newItem
    }
}