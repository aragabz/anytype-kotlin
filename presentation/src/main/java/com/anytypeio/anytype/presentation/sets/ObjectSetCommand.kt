package com.anytypeio.anytype.presentation.sets

import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_models.Key

sealed class ObjectSetCommand {

    sealed class Modal : ObjectSetCommand() {

        data class Menu(
            val ctx: Id,
            val space: Id,
            val isArchived: Boolean,
            val isFavorite: Boolean,
            val isReadOnly: Boolean
        ) : Modal()

        data class OpenSettings(
            val ctx: Id,
            val dv: Id,
            val viewer: Id
        ) : Modal()

        data class ModifyViewerFilters(
            val ctx: Id,
            val viewer: Id
        ) : Modal()

        data class ModifyViewerSorts(
            val ctx: Id,
            val viewer: Id
        ) : Modal()

        data class EditGridTextCell(
            val ctx: Id,
            val space: Id,
            val relationKey: Id,
            val recordId: Id
        ) : Modal()

        data class EditGridDateCell(
            val ctx: Id,
            val space: Id,
            val relationKey: Key,
            val objectId: Id
        ) : Modal()

        data class EditObjectCell(
            val ctx: Id,
            val space: Id,
            val target: Id,
            val relationKey: Key
        ) : Modal()

        data class EditTagOrStatusCell(
            val ctx: Id,
            val space: Id,
            val target: Id,
            val relationKey: Key
        ) : Modal()

        data class SetNameForCreatedObject(val ctx: Id, val space: Id, val target: Id) : Modal()

        data class CreateBookmark(val ctx: Id, val space: Id) : Modal()

        data class OpenIconActionMenu(
            val target: Id,
            val space: Id
        ) : Modal()

        data class OpenCoverActionMenu(
            val ctx: Id,
            val space: Id
        ) : Modal()

        data class OpenDataViewSelectQueryScreen(
            val selectedTypes: List<Id>
        ) : Modal()

        data object OpenEmptyDataViewSelectQueryScreen: Modal()

        data class EditIntrinsicTextRelation(
            val ctx: Id,
            val space: Id,
            val relation: Key
        ) : Modal()

        data class EditObjectRelationValue(
            val ctx: Id,
            val space: Id,
            val relation: Key
        ) : Modal()

        data class EditTagOrStatusRelationValue(
            val ctx: Id,
            val space: Id,
            val relation: Key
        ) : Modal()

        data class OpenSelectTypeScreen(
            val excludedTypes: List<Id>
        ) : Modal()

        data class ShowObjectSetTypePopupMenu(
            val ctx: Id,
            val anchor: Int
        ) : Modal()

        data class ShowObjectSetRelationPopupMenu(
            val ctx: Id,
            val anchor: Int
        ) : Modal()
    }

    sealed class Intent : ObjectSetCommand() {
        data class GoTo(val url: String) : Intent()
        data class MailTo(val email: String) : Intent()
        data class Call(val phone: String) : Intent()
        data object OpenAppStore : Intent()
    }

    data object ShowOnlyAccessError : ObjectSetCommand()

    data class Browse(val url: String) : ObjectSetCommand()
}