package com.anytypeio.anytype.ui.vault

import android.content.res.Configuration
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_ui.common.ReorderHapticFeedbackType
import com.anytypeio.anytype.core_ui.common.ShimmerEffect
import com.anytypeio.anytype.core_ui.common.rememberReorderHapticFeedback
import com.anytypeio.anytype.core_ui.foundation.noRippleThrottledClickable
import com.anytypeio.anytype.core_ui.views.Caption1Regular
import com.anytypeio.anytype.core_ui.views.Title1
import com.anytypeio.anytype.core_ui.widgets.ListWidgetObjectIcon
import com.anytypeio.anytype.core_utils.insets.EDGE_TO_EDGE_MIN_SDK
import com.anytypeio.anytype.presentation.objects.ObjectIcon.Profile.Avatar
import com.anytypeio.anytype.presentation.profile.AccountProfile
import com.anytypeio.anytype.presentation.profile.ProfileIconView
import com.anytypeio.anytype.presentation.spaces.SelectSpaceViewModel
import com.anytypeio.anytype.presentation.vault.VaultSectionView
import com.anytypeio.anytype.presentation.vault.VaultSpaceView
import com.anytypeio.anytype.ui.settings.typography
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.delay
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@Composable
fun UnreadSectionHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = stringResource(R.string.vault_unread_section_title),
            style = Caption1Regular,
            color = colorResource(id = R.color.text_secondary),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun AllSectionHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(
            text = stringResource(R.string.vault_all_section_title),
            style = Caption1Regular,
            color = colorResource(id = R.color.text_secondary),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultScreenToolbar(
    profile: AccountProfile,
    spaceCountLimitReached: Boolean = false,
    onPlusClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    isLoading: Boolean
) {

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .noRippleThrottledClickable {
                        onSettingsClicked()
                    },
                contentAlignment = Alignment.Center

            ) {
                ProfileIcon(
                    modifier = Modifier.size(28.dp),
                    profile = profile
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                style = Title1,
                text = stringResource(R.string.vault_my_spaces),
                color = colorResource(id = R.color.text_primary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!spaceCountLimitReached) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(64.dp)
                        .fillMaxHeight()
                        .noRippleThrottledClickable {
                            onPlusClicked()
                        },
                    contentAlignment = Alignment.Center

                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_plus_18),
                        contentDescription = stringResource(R.string.content_description_plus_button),
                        modifier = Modifier.size(28.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (isLoading) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfileIcon(
    modifier: Modifier,
    profile: AccountProfile
) {
    when (profile) {
        is AccountProfile.Data -> {
            Box(modifier) {
                when (val icon = profile.icon) {
                    is ProfileIconView.Image -> {
                        Image(
                            painter = rememberAsyncImagePainter(icon.url),
                            contentDescription = "Custom image profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    else -> {
                        val nameFirstChar = if (profile.name.isEmpty()) {
                            stringResource(id = com.anytypeio.anytype.ui_settings.R.string.account_default_name)
                        } else {
                            profile.name.first().uppercaseChar().toString()
                        }
                        ListWidgetObjectIcon(
                            modifier = Modifier.fillMaxSize(),
                            icon = Avatar(
                                name = nameFirstChar
                            ),
                            iconSize = 28.dp
                        )
                    }
                }
            }
        }

        AccountProfile.Idle -> {
            // Draw nothing
        }
    }
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode - Not Scrolled"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Mode - Not Scrolled"
)
fun VaultScreenToolbarNotScrolledPreview() {
    VaultScreenToolbar(
        onPlusClicked = {},
        onSettingsClicked = {},
        profile = AccountProfile.Data(
            name = "John Doe",
            icon = ProfileIconView.Placeholder(name = "Jd")
        ),
        isLoading = false,
    )
}

@Composable
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "Dark Mode - Scrolled"
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Mode - Scrolled"
)
fun VaultScreenToolbarScrolledPreview() {
    VaultScreenToolbar(
        onPlusClicked = {},
        onSettingsClicked = {},
        profile = AccountProfile.Data(
            name = "John Doe",
            icon = ProfileIconView.Placeholder(name = "Jd")
        ),
        isLoading = false,
    )
}

@Composable
fun VaultScreenWithUnreadSection(
    profile: AccountProfile,
    sections: VaultSectionView,
    onSpaceClicked: (VaultSpaceView) -> Unit,
    onCreateSpaceClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onOrderChanged: (String, String) -> Unit,
    onDragEnd: () -> Unit = { /* No-op */ },
    isLoading: Boolean
) {

    var mainSpaceList by remember {
        mutableStateOf<List<VaultSpaceView>>(sections.mainSpaces)
    }

    mainSpaceList = sections.mainSpaces

    val hapticFeedback = rememberReorderHapticFeedback()

    val lazyListState = rememberLazyListState()

    val reorderableLazyState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Extract space IDs from keys (remove prefix) before passing to ViewModel
        val fromSpaceId = (from.key as String).removePrefix(MAIN_SECTION_KEY_PREFIX)
        val toSpaceId = (to.key as String).removePrefix(MAIN_SECTION_KEY_PREFIX)

        onOrderChanged(fromSpaceId, toSpaceId)
        hapticFeedback.performHapticFeedback(ReorderHapticFeedbackType.MOVE)
    }

    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(reorderableLazyState.isAnyItemDragging) {
        if (reorderableLazyState.isAnyItemDragging) {
            isDragging = true
            // Optional: Add a small delay to avoid triggering on very short drags
            delay(50)
        } else if (isDragging) {
            isDragging = false
            onDragEnd()
            hapticFeedback.performHapticFeedback(ReorderHapticFeedbackType.MOVE)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
            .background(color = colorResource(id = R.color.background_primary))
            .then(
                if (SDK_INT >= EDGE_TO_EDGE_MIN_SDK)
                    Modifier.windowInsetsPadding(WindowInsets.systemBars)
                else
                    Modifier
            ),
        backgroundColor = colorResource(id = R.color.background_primary),
        topBar = {
            Column {
                VaultScreenToolbar(
                    profile = profile,
                    onPlusClicked = onCreateSpaceClicked,
                    onSettingsClicked = onSettingsClicked,
                    spaceCountLimitReached = sections.allSpaces.size >= SelectSpaceViewModel.MAX_SPACE_COUNT,
                    isLoading = isLoading
                )
            }
        }
    ) { paddings ->
        if (sections.allSpaces.isEmpty()) {
            VaultEmptyState(
                modifier = Modifier.padding(paddings),
                onCreateSpaceClicked = onCreateSpaceClicked
            )
        } else {
            // Single LazyColumn with reorderable approach
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp)
            ) {
                // Unread Section Header and Items (Non-draggable)
                if (sections.hasUnreadSpaces) {
                    item(key = "unread_header") {
                        UnreadSectionHeader()
                    }

                    itemsIndexed(
                        items = sections.unreadSpaces,
                        key = { _, item -> "unread_${item.space.id}" },
                        contentType = { _, item ->
                            when (item) {
                                is VaultSpaceView.Chat -> TYPE_CHAT
                                is VaultSpaceView.Space -> TYPE_SPACE
                            }
                        }
                    ) { _, item ->
                        // Unread items are not draggable
                        when (item) {
                            is VaultSpaceView.Chat -> {
                                VaultChatCard(
                                    modifier = Modifier
                                        .animateItem()
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .padding(horizontal = 16.dp)
                                        .clickable {
                                            onSpaceClicked(item)
                                        },
                                    onSpaceIconClicked = {
                                        onSpaceClicked(item)
                                    },
                                    title = item.space.name.orEmpty(),
                                    icon = item.icon,
                                    previewText = item.previewText,
                                    creatorName = item.creatorName,
                                    messageText = item.messageText,
                                    messageTime = item.messageTime,
                                    chatPreview = item.chatPreview,
                                    unreadMessageCount = item.unreadMessageCount,
                                    unreadMentionCount = item.unreadMentionCount,
                                    attachmentPreviews = item.attachmentPreviews
                                )
                            }

                            is VaultSpaceView.Space -> {
                                VaultSpaceCard(
                                    modifier = Modifier.animateItem(),
                                    title = item.space.name.orEmpty(),
                                    subtitle = item.accessType,
                                    onCardClicked = {
                                        onSpaceClicked(item)
                                    },
                                    icon = item.icon,
                                )
                            }
                        }
                    }

                    if (sections.mainSpaces.isNotEmpty()) {
                        item(key = "all_section") {
                            AllSectionHeader()
                        }
                    }
                }

                // Main Section Items (Draggable using ReorderableItem)
                if (sections.mainSpaces.isNotEmpty()) {
                    itemsIndexed(
                        items = mainSpaceList,
                        key = { _, item -> "$MAIN_SECTION_KEY_PREFIX${item.space.id}" },
                        contentType = { _, item ->
                            when (item) {
                                is VaultSpaceView.Chat -> TYPE_CHAT
                                is VaultSpaceView.Space -> TYPE_SPACE
                            }
                        }
                    ) { idx, item ->
                        ReorderableItem(
                            state = reorderableLazyState,
                            key = "$MAIN_SECTION_KEY_PREFIX${item.space.id}"
                        ) { isDragging ->
                            when (item) {
                                is VaultSpaceView.Chat -> {
                                    VaultChatCard(
                                        modifier = Modifier
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        ReorderHapticFeedbackType.START
                                                    )
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        ReorderHapticFeedbackType.END
                                                    )
                                                }
                                            )
                                            .fillMaxWidth()
                                            .height(80.dp)
                                            .padding(horizontal = 16.dp)
                                            .clickable {
                                                onSpaceClicked(item)
                                            },
                                        title = item.space.name.orEmpty(),
                                        icon = item.icon,
                                        previewText = item.previewText,
                                        creatorName = item.creatorName,
                                        messageText = item.messageText,
                                        messageTime = item.messageTime,
                                        chatPreview = item.chatPreview,
                                        unreadMessageCount = item.unreadMessageCount,
                                        unreadMentionCount = item.unreadMentionCount,
                                        attachmentPreviews = item.attachmentPreviews,
                                        onSpaceIconClicked = {
                                            onSpaceClicked(item)
                                        }
                                    )
                                }

                                is VaultSpaceView.Space -> {
                                    Box(
                                        modifier = Modifier
                                            .longPressDraggableHandle(
                                                onDragStarted = {
                                                    hapticFeedback.performHapticFeedback(
                                                        ReorderHapticFeedbackType.START
                                                    )
                                                },
                                                onDragStopped = {
                                                    hapticFeedback.performHapticFeedback(
                                                        ReorderHapticFeedbackType.END
                                                    )
                                                }
                                            )
                                    ) {
                                        VaultSpaceCard(
                                            modifier = Modifier.animateItem(),
                                            title = item.space.name.orEmpty(),
                                            subtitle = item.accessType,
                                            onCardClicked = {
                                                onSpaceClicked(item)
                                            },
                                            icon = item.icon
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

const val TYPE_CHAT = "chat"
const val TYPE_SPACE = "space"

private const val MAIN_SECTION_KEY_PREFIX = "main_"

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    name = "Light Mode - Unread Section Header Only"
)
@Composable
fun UnreadSectionHeaderPreview() {
    MaterialTheme(typography = typography) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(id = R.color.background_primary))
                .padding(vertical = 16.dp)
        ) {
            UnreadSectionHeader()
        }
    }
}