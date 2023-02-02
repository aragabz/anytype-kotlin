package com.anytypeio.anytype.ui.library.views.list.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.RelationFormat
import com.anytypeio.anytype.core_ui.extensions.simpleIcon
import com.anytypeio.anytype.core_ui.widgets.ObjectIconWidget
import com.anytypeio.anytype.presentation.objects.ObjectIcon
import com.anytypeio.anytype.ui.library.views.list.items.ItemDefaults.TEXT_PADDING_START

@Composable
fun MyTypeItem(
    name: String,
    icon: ObjectIcon?,
    readOnly: Boolean = false,
    modifier: Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon = icon)
        Text(
            text = name,
            color = colorResource(id = R.color.text_primary),
            modifier = Modifier
                .padding(start = TEXT_PADDING_START)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (readOnly) {
            Image(
                painter = painterResource(id = R.drawable.ic_object_locked),
                contentDescription = "",
            )
        }
    }
}

@Composable
fun LibTypeItem(
    name: String,
    icon: ObjectIcon?,
    installed: Boolean = false,
    modifier: Modifier,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon = icon)
        Text(
            text = name,
            color = colorResource(id = R.color.text_primary),
            modifier = Modifier
                .padding(start = TEXT_PADDING_START)
        )
        Spacer(modifier = Modifier.weight(1f))
        val installedImageRes = if (installed) {
            R.drawable.ic_type_installed
        } else {
            R.drawable.ic_type_not_installed
        }
        Image(
            painter = painterResource(id = installedImageRes),
            contentDescription = installedImageRes.toString(),
        )
    }
}

@Composable
fun MyRelationItem(
    modifier: Modifier,
    name: String,
    format: RelationFormat,
    readOnly: Boolean = false,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        format.simpleIcon()?.let {
            Image(painter = painterResource(id = it), contentDescription = "")
        }
        Text(
            text = name,
            color = colorResource(id = R.color.text_primary),
            modifier = Modifier
                .padding(start = TEXT_PADDING_START)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (readOnly) {
            Image(
                painter = painterResource(id = R.drawable.ic_object_locked),
                contentDescription = ""
            )
        }
    }
}

@Composable
fun LibRelationItem(
    modifier: Modifier,
    name: String,
    format: RelationFormat,
    installed: Boolean
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        format.simpleIcon()?.let {
            Image(painter = painterResource(id = it), contentDescription = "")
        }
        Text(
            text = name,
            color = colorResource(id = R.color.text_primary),
            modifier = Modifier
                .padding(start = TEXT_PADDING_START)
        )
        Spacer(modifier = Modifier.weight(1f))
        val installedImageRes = if (installed) {
            R.drawable.ic_type_installed
        } else {
            R.drawable.ic_type_not_installed
        }
        Image(
            painter = painterResource(id = installedImageRes),
            contentDescription = installedImageRes.toString(),
        )
    }
}

@Composable
fun CreateNewTypeItem(
    modifier: Modifier,
    name: String,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_default_plus),
            contentDescription = "",
        )
        Text(
            text = stringResource(
                id = R.string.library_create_new_type,
                formatArgs = arrayOf(name)
            ),
            color = colorResource(id = R.color.text_primary),
            modifier = Modifier.padding(start = TEXT_PADDING_START)
        )
    }
}

@Composable
fun Icon(icon: ObjectIcon?) {
    icon?.let {
        AndroidView(factory = { ctx ->
            ObjectIconWidget(ctx).apply {
                setIcon(it)
            }
        })
    }
}


@Immutable
object ItemDefaults {
    val ITEM_HEIGHT = 52.dp
    val TEXT_PADDING_START = 10.dp
}
