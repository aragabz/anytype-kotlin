package com.anytypeio.anytype.ui_settings.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.anytypeio.anytype.core_ui.foundation.noRippleClickable
import com.anytypeio.anytype.core_ui.views.Caption1Regular
import com.anytypeio.anytype.core_ui.views.HeadlineHeading
import com.anytypeio.anytype.core_ui.views.Title1
import com.anytypeio.anytype.emojifier.Emojifier
import com.anytypeio.anytype.presentation.spaces.SpaceIconView
import com.anytypeio.anytype.ui_settings.R

@Composable
fun Section(modifier: Modifier = Modifier, title: String) {
    Text(
        modifier = modifier,
        text = title,
        color = colorResource(id = R.color.text_secondary),
        style = Caption1Regular
    )
}

@Composable
fun NameBlock(
    modifier: Modifier = Modifier,
    name: String,
    onNameSet: (String) -> Unit
) {

    val nameValue = remember { mutableStateOf(name) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.padding(start = 20.dp)) {
        Text(
            text = "Name",
            style = Caption1Regular,
            color = colorResource(id = R.color.text_secondary)
        )
        SettingsTextField(
            value = nameValue.value,
            onValueChange = {
                nameValue.value = it
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    onNameSet.invoke(nameValue.value)
                    focusManager.clearFocus()
                }
            ),
        )
    }
}

@Composable
fun SpaceNameBlock() {
    Text(
        text = "Space",
        style = Title1,
        color = colorResource(id = R.color.text_primary)
    )
}

@Composable
fun SpaceImageBlock(icon: SpaceIconView, onSpaceIconClick: () -> Unit) {
    when (icon) {
        is SpaceIconView.Emoji -> {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .noRippleClickable {
                        onSpaceIconClick.invoke()
                    }
                    .background(color = colorResource(id = R.color.shape_primary))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = Emojifier.uri(icon.unicode),
                        error = painterResource(id = R.drawable.ic_home_widget_space)
                    ),
                    contentDescription = "Emoji space icon",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        is SpaceIconView.Image -> {
            Image(
                painter = rememberAsyncImagePainter(
                    model = icon.url,
                    error = painterResource(id = R.drawable.ic_home_widget_space)
                ),
                contentDescription = "Custom image space icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .noRippleClickable {
                        onSpaceIconClick.invoke()
                    }
            )
        }
        else -> {
            Image(
                painter = painterResource(id = R.drawable.ic_home_widget_space),
                contentDescription = "Placeholder space icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .noRippleClickable {
                        onSpaceIconClick.invoke()
                    }
            )
        }
    }
}

@Composable
fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {

    @OptIn(ExperimentalMaterialApi::class)
    BasicTextField(
        value = value,
        modifier = Modifier
            .padding(top = 4.dp, end = 20.dp)
            .fillMaxWidth(),
        onValueChange = onValueChange,
        enabled = true,
        readOnly = false,
        textStyle = HeadlineHeading.copy(color = colorResource(id = R.color.text_primary)),
        cursorBrush = SolidColor(colorResource(id = R.color.orange)),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = keyboardActions,
        interactionSource = remember { MutableInteractionSource() },
        singleLine = true,
        maxLines = 1,
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.OutlinedTextFieldDecorationBox(
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = null,
                label = null,
                leadingIcon = null,
                trailingIcon = null,
                singleLine = true,
                enabled = true,
                isError = false,
                interactionSource = remember { MutableInteractionSource() },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = colorResource(id = R.color.text_primary),
                    backgroundColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    errorBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    placeholderColor = colorResource(id = R.color.glyph_active),
                    cursorColor = colorResource(id = R.color.orange)
                ),
                contentPadding = PaddingValues(),
                border = {}
            )
        }
    )
}