package com.anytypeio.anytype.data.auth.repo.block

import com.anytypeio.anytype.core_models.Block
import com.anytypeio.anytype.core_models.Command
import com.anytypeio.anytype.core_models.DVFilter
import com.anytypeio.anytype.core_models.DVSort
import com.anytypeio.anytype.core_models.DVViewer
import com.anytypeio.anytype.core_models.DVViewerType
import com.anytypeio.anytype.core_models.DocumentInfo
import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_models.ObjectInfoWithLinks
import com.anytypeio.anytype.core_models.ObjectType
import com.anytypeio.anytype.core_models.Payload
import com.anytypeio.anytype.core_models.Position
import com.anytypeio.anytype.core_models.Relation
import com.anytypeio.anytype.core_models.RelationFormat
import com.anytypeio.anytype.core_models.Response
import com.anytypeio.anytype.core_models.SearchResult
import com.anytypeio.anytype.core_models.Url

interface BlockRemote {

    suspend fun create(command: Command.Create): Pair<String, Payload>
    suspend fun replace(command: Command.Replace): Pair<String, Payload>
    suspend fun duplicate(command: Command.Duplicate): Pair<List<Id>, Payload>
    suspend fun split(command: Command.Split): Pair<Id, Payload>

    suspend fun merge(command: Command.Merge): Payload
    suspend fun unlink(command: Command.Unlink): Payload
    suspend fun updateTextColor(command: Command.UpdateTextColor): Payload
    suspend fun updateBackgroundColor(command: Command.UpdateBackgroundColor): Payload
    suspend fun updateAlignment(command: Command.UpdateAlignment) : Payload

    suspend fun createDocument(command: Command.CreateDocument): Triple<String, String, Payload>
    suspend fun updateDocumentTitle(command: Command.UpdateTitle)
    suspend fun updateText(command: Command.UpdateText)
    suspend fun updateTextStyle(command: Command.UpdateStyle) : Payload
    suspend fun setTextIcon(command: Command.SetTextIcon): Payload

    suspend fun setLinkAppearance(command: Command.SetLinkAppearance): Payload

    suspend fun updateCheckbox(command: Command.UpdateCheckbox): Payload
    suspend fun move(command: Command.Move): Payload
    suspend fun createPage(
        ctx: Id?,
        emoji: String?,
        isDraft: Boolean?,
        type: String?,
        template: Id?
    ): Id
    suspend fun createPage(command: Command.CreateNewDocument): String
    suspend fun openPage(id: String): Payload
    suspend fun openProfile(id: String): Payload
    suspend fun openObjectSet(id: String): Payload
    suspend fun openObjectPreview(id: Id) : Payload
    suspend fun closePage(id: String)
    suspend fun openDashboard(contextId: String, id: String): Payload
    suspend fun closeDashboard(id: String)
    suspend fun setDocumentEmojiIcon(command: Command.SetDocumentEmojiIcon): Payload
    suspend fun setDocumentImageIcon(command: Command.SetDocumentImageIcon): Payload
    suspend fun setDocumentCoverColor(ctx: String, color: String): Payload
    suspend fun setDocumentCoverGradient(ctx: String, gradient: String): Payload
    suspend fun setDocumentCoverImage(ctx: String, hash: String): Payload
    suspend fun removeDocumentCover(ctx: String): Payload
    suspend fun removeDocumentIcon(ctx: Id): Payload
    suspend fun uploadBlock(command: Command.UploadBlock): Payload
    suspend fun setupBookmark(command: Command.SetupBookmark) : Payload
    suspend fun createAndFetchBookmarkBlock(command: Command.CreateBookmark): Payload
    suspend fun createBookmarkObject(url: Url) : Id
    suspend fun fetchBookmarkObject(ctx: Id, url: Url)
    suspend fun undo(command: Command.Undo) : Payload
    suspend fun redo(command: Command.Redo) : Payload
    suspend fun turnIntoDocument(command: Command.TurnIntoDocument): List<Id>
    suspend fun paste(command: Command.Paste) : Response.Clipboard.Paste
    suspend fun copy(command: Command.Copy) : Response.Clipboard.Copy

    suspend fun uploadFile(command: Command.UploadFile): String
    suspend fun downloadFile(command: Command.DownloadFile): String

    suspend fun getObjectInfoWithLinks(pageId: String): ObjectInfoWithLinks

    suspend fun getListPages(): List<DocumentInfo>

    suspend fun setRelationKey(command: Command.SetRelationKey): Payload

    suspend fun updateDivider(command: Command.UpdateDivider): Payload

    suspend fun setFields(command: Command.SetFields): Payload

    suspend fun getObjectTypes(): List<ObjectType>
    suspend fun createObjectType(prototype: ObjectType.Prototype): ObjectType

    suspend fun createSet(
        contextId: String,
        targetId: String?,
        position: Position?,
        objectType: String?
    ): Response.Set.Create

    suspend fun setActiveDataViewViewer(
        context: Id,
        block: Id,
        view: Id,
        offset: Int,
        limit: Int
    ): Payload

    suspend fun addNewRelationToDataView(
        context: Id,
        target: Id,
        name: String,
        format: Relation.Format,
        limitObjectTypes: List<Id>
    ): Pair<Id, Payload>

    suspend fun addRelationToDataView(ctx: Id, dv: Id, relation: Id): Payload
    suspend fun deleteRelationFromDataView(ctx: Id, dv: Id, relation: Id): Payload

    suspend fun updateDataViewViewer(
        context: String,
        target: String,
        viewer: DVViewer
    ): Payload

    suspend fun turnInto(
        context: String,
        targets: List<String>,
        style: Block.Content.Text.Style
    ): Payload
    suspend fun duplicateDataViewViewer(
        context: String,
        target: String,
        viewer: DVViewer
    ): Payload

    suspend fun createDataViewRecord(
        context: Id, target: Id, template: Id?, prefilled: Map<Id, Any>
    ): Map<String, Any?>

    suspend fun updateDataViewRecord(
        context: Id,
        target: Id,
        record: Id,
        values: Map<String, Any?>
    )

    suspend fun addDataViewViewer(
        ctx: String,
        target: String,
        name: String,
        type: DVViewerType
    ): Payload

    suspend fun removeDataViewViewer(
        ctx: String,
        dataview: String,
        viewer: String
    ): Payload

    suspend fun addDataViewRelationOption(
        ctx: Id,
        dataview: Id,
        relation: Id,
        record: Id,
        name: String,
        color: String
    ): Pair<Payload, Id?>

    suspend fun addObjectRelationOption(
        ctx: Id,
        relation: Id,
        name: Id,
        color: String
    ): Pair<Payload, Id?>

    suspend fun searchObjects(
        sorts: List<DVSort>,
        filters: List<DVFilter>,
        fulltext: String,
        offset: Int,
        limit: Int,
        keys: List<Id>
    ): List<Map<String, Any?>>

    suspend fun searchObjectsWithSubscription(
        subscription: Id,
        sorts: List<DVSort>,
        filters: List<DVFilter>,
        keys: List<String>,
        source: List<String>,
        offset: Long,
        limit: Int,
        beforeId: Id?,
        afterId: Id?,
    ): SearchResult

    suspend fun searchObjectsByIdWithSubscription(
        subscription: Id,
        ids: List<Id>,
        keys: List<String>
    ): SearchResult

    suspend fun cancelObjectSearchSubscription(subscriptions: List<Id>)

    suspend fun relationListAvailable(ctx: Id): List<Relation>
    suspend fun addRelationToObject(ctx: Id, relation: Id) : Payload
    suspend fun deleteRelationFromObject(ctx: Id, relation: Id): Payload
    suspend fun addNewRelationToObject(
        ctx: Id,
        name: String,
        format: RelationFormat,
        limitObjectTypes: List<Id>
    ) : Pair<Id, Payload>

    suspend fun debugSync(): String
    suspend fun debugLocalStore(path: String): String

    suspend fun updateDetail(
        ctx: Id,
        key: String,
        value: Any?
    ): Payload

    suspend fun updateBlocksMark(command: Command.UpdateBlocksMark): Payload

    suspend fun addRelationToBlock(command: Command.AddRelationToBlock): Payload

    suspend fun setObjectTypeToObject(ctx: Id, typeId: Id): Payload

    suspend fun addToFeaturedRelations(ctx: Id, relations: List<Id>): Payload
    suspend fun removeFromFeaturedRelations(ctx: Id, relations: List<Id>): Payload

    suspend fun setObjectIsFavorite(ctx: Id, isFavorite: Boolean) : Payload
    suspend fun setObjectIsArchived(ctx: Id, isArchived: Boolean) : Payload

    suspend fun setObjectListIsArchived(targets: List<Id>, isArchived: Boolean)
    suspend fun deleteObjects(targets: List<Id>)

    suspend fun setObjectLayout(ctx: Id, layout: ObjectType.Layout) : Payload

    suspend fun clearFileCache()

    suspend fun duplicateObject(id: Id): Id

    suspend fun applyTemplate(ctx: Id, template: Id)

    suspend fun createTable(
        ctx: String,
        target: String,
        position: Position,
        rows: Int,
        columns: Int
    ): Payload

    suspend fun fillTableRow(ctx: String, targetIds: List<String>): Payload

    suspend fun objectToSet(ctx: Id, source: List<String>): Id

    suspend fun blockDataViewSetSource(ctx: Id, block: Id, sources: List<String>): Payload

    suspend fun clearBlockContent(ctx: Id, blockIds: List<Id>) : Payload

    suspend fun clearBlockStyle(ctx: Id, blockIds: List<Id>) : Payload

    suspend fun fillTableColumn(ctx: Id, blockIds: List<Id>): Payload

    suspend fun createTableRow(
        ctx: Id,
        targetId: Id,
        position: Position
    ): Payload

    suspend fun setTableRowHeader(
        ctx: Id,
        targetId: Id,
        isHeader: Boolean
    ): Payload

    suspend fun createTableColumn(
        ctx: Id,
        targetId: Id,
        position: Position
    ): Payload

    suspend fun deleteTableColumn(
        ctx: Id,
        targetId: Id
    ): Payload

    suspend fun deleteTableRow(
        ctx: Id,
        targetId: Id
    ): Payload

    suspend fun duplicateTableColumn(
        ctx: Id,
        targetId: Id,
        blockId: Id,
        position: Position
    ): Payload

    suspend fun duplicateTableRow(
        ctx: Id,
        targetId: Id,
        blockId: Id,
        position: Position
    ): Payload

    suspend fun sortTable(
        ctx: Id,
        columnId: String, type: Block.Content.DataView.Sort.Type
    ): Payload

    suspend fun expandTable(
        ctx: Id,
        targetId: Id,
        columns: Int,
        rows: Int
    ): Payload
}