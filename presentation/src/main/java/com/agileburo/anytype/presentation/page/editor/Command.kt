package com.agileburo.anytype.presentation.page.editor

import com.agileburo.anytype.core_ui.features.page.BlockView
import com.agileburo.anytype.domain.common.Id
import com.agileburo.anytype.domain.common.Url

sealed class Command {

    data class OpenDocumentIconActionMenu(
        val target: String
    ) : Command()

    data class OpenDocumentEmojiIconPicker(
        val target: String
    ) : Command()

    data class OpenGallery(
        val mediaType: String
    ) : Command()

    data class OpenBookmarkSetter(
        val target: String,
        val context: String
    ) : Command()

    object OpenAddBlockPanel : Command()

    data class OpenTurnIntoPanel(
        val target: Id
    ) : Command()

    object OpenMultiSelectTurnIntoPanel : Command()

    data class RequestDownloadPermission(
        val id: String
    ) : Command()

    object PopBackStack : Command()

    object CloseKeyboard : Command()

    data class OpenActionBar(
        val block: BlockView
    ) : Command()

    data class Browse(
        val url: Url
    ) : Command()
}