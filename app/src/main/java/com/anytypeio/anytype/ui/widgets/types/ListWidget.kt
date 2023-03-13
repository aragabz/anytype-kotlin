package com.anytypeio.anytype.ui.widgets.types

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.ObjectWrapper
import com.anytypeio.anytype.core_ui.foundation.noRippleClickable
import com.anytypeio.anytype.presentation.home.InteractionMode
import com.anytypeio.anytype.presentation.widgets.DropDownMenuAction
import com.anytypeio.anytype.presentation.widgets.WidgetId
import com.anytypeio.anytype.presentation.widgets.WidgetView
import com.anytypeio.anytype.presentation.widgets.WidgetView.ListOfObjects.Type
import com.anytypeio.anytype.ui.widgets.menu.WidgetMenu

@Composable
fun ListWidgetCard(
    item: WidgetView.ListOfObjects,
    mode: InteractionMode,
    onWidgetObjectClicked: (ObjectWrapper.Basic) -> Unit,
    onListWidgetHeaderClicked: (WidgetId) -> Unit,
    onDropDownMenuAction: (DropDownMenuAction) -> Unit,
    onToggleExpandedWidgetState: (WidgetId) -> Unit
) {
    val isCardMenuExpanded = remember {
        mutableStateOf(false)
    }
    val isHeaderMenuExpanded = remember {
        mutableStateOf(false)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp)
            .alpha(if (isCardMenuExpanded.value || isHeaderMenuExpanded.value) 0.8f else 1f)
            .background(
                shape = RoundedCornerShape(16.dp),
                color = colorResource(id = R.color.dashboard_card_background)
            )
            .then(
                if (mode is InteractionMode.Edit)
                    Modifier.noRippleClickable {
                        isCardMenuExpanded.value = !isCardMenuExpanded.value
                    }
                else
                    Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 6.dp)
        ) {
            WidgetHeader(
                title = when(item.type) {
                    Type.Favorites -> stringResource(id = R.string.favorites)
                    Type.Recent -> stringResource(id = R.string.recent)
                    Type.Sets -> stringResource(id = R.string.sets)
                },
                isCardMenuExpanded = isCardMenuExpanded,
                isHeaderMenuExpanded = isHeaderMenuExpanded,
                onWidgetHeaderClicked = {
                    if (mode is InteractionMode.Default) {
                        onListWidgetHeaderClicked(item.id)
                    }
                },
                onExpandElement = { onToggleExpandedWidgetState(item.id) },
                isExpanded = item.isExpanded,
                isInEditMode = mode is InteractionMode.Edit,
                onDropDownMenuAction = onDropDownMenuAction
            )
            if (item.elements.isNotEmpty()) {
                item.elements.forEachIndexed { idx, element ->
                    ListWidgetElement(
                        onWidgetObjectClicked = onWidgetObjectClicked,
                        obj = element.obj,
                        icon = element.icon,
                        mode = mode
                    )
                    Divider(
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(end = 16.dp, start = 8.dp)
                    )
                    if (idx == item.elements.lastIndex) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // TODO
//                if (item.isExpanded) {
//                    if (item.tabs.isNotEmpty())
//                        EmptyWidgetPlaceholder(R.string.empty_list_widget)
//                    else
//                        EmptyWidgetPlaceholder(text = R.string.empty_list_widget_no_view)
//                }
            }
        }
        WidgetMenu(
            isExpanded = isCardMenuExpanded,
            onDropDownMenuAction = onDropDownMenuAction,
            canEditWidgets = mode !is InteractionMode.Edit
        )
    }
}