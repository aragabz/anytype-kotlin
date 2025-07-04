package com.anytypeio.anytype.presentation.sets

import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_models.Key
import com.anytypeio.anytype.core_models.ObjectWrapper
import com.anytypeio.anytype.core_models.Relation
import com.anytypeio.anytype.core_models.Relations
import com.anytypeio.anytype.core_models.Struct
import com.anytypeio.anytype.core_utils.ext.typeOf
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.domain.objects.ObjectStore
import com.anytypeio.anytype.presentation.number.NumberParser
import com.anytypeio.anytype.presentation.objects.ObjectIcon
import com.anytypeio.anytype.domain.primitives.FieldParser
import com.anytypeio.anytype.core_models.ObjectViewDetails
import com.anytypeio.anytype.domain.objects.StoreOfObjectTypes
import com.anytypeio.anytype.domain.objects.getTypeOfObject
import com.anytypeio.anytype.presentation.extension.getFileObject
import com.anytypeio.anytype.presentation.mapper.objectIcon
import com.anytypeio.anytype.presentation.objects.buildObjectViews
import com.anytypeio.anytype.presentation.relations.getDateRelationFormat
import com.anytypeio.anytype.presentation.sets.model.CellView
import com.anytypeio.anytype.presentation.sets.model.ColumnView
import com.anytypeio.anytype.presentation.sets.model.FileView
import com.anytypeio.anytype.presentation.sets.model.StatusView
import com.anytypeio.anytype.presentation.sets.model.TagView
import com.anytypeio.anytype.presentation.sets.model.Viewer
import com.anytypeio.anytype.presentation.sets.state.ObjectState
import timber.log.Timber

suspend fun List<ColumnView>.buildGridRow(
    showIcon: Boolean,
    obj: ObjectWrapper.Basic,
    builder: UrlBuilder,
    store: ObjectStore,
    fieldParser: FieldParser,
    storeOfObjectTypes: StoreOfObjectTypes
): Viewer.GridView.Row {

    val type = obj.type.firstOrNull()
    val name = fieldParser.getObjectName(obj)
    val emoji = obj.iconEmoji
    val image = obj.iconImage
    val done = obj.done
    val layout = obj.layout
    val objectIcon = obj.objectIcon(
        builder = builder,
        objType = storeOfObjectTypes.getTypeOfObject(obj)
    )

    val cells = mutableListOf<CellView>()
    this.map { column ->
        when (column.key) {
            Relations.NAME -> {
                cells.add(
                    CellView.Description(
                        id = obj.id,
                        relationKey = column.key,
                        text = ""
                    )
                )
            }

            Relations.TYPE -> {
                val objects = obj.map.buildObjectViews(
                    columnKey = column.key,
                    builder = builder,
                    store = store,
                    withIcon = false,
                    fieldParser = fieldParser,
                    storeOfObjectTypes = storeOfObjectTypes
                )
                cells.add(
                    CellView.Object(
                        id = obj.id,
                        relationKey = column.key,
                        objects = objects
                    )
                )
            }

            else -> {
                // TODO refact
                if (column.format == ColumnView.Format.EMOJI) return@map
                cells.add(
                    when (column.format) {
                        ColumnView.Format.SHORT_TEXT, ColumnView.Format.LONG_TEXT -> {
                            CellView.Description(
                                id = obj.id,
                                relationKey = column.key,
                                text = obj.getValue<String>(column.key).orEmpty()
                            )
                        }

                        ColumnView.Format.NUMBER -> {
                            val value = obj.getValue<Any?>(column.key)
                            CellView.Number(
                                id = obj.id,
                                relationKey = column.key,
                                number = NumberParser.parse(value)
                            )
                        }

                        ColumnView.Format.DATE -> {
                            val fieldDate = fieldParser.toDate(any = obj.getValue<Any?>(column.key))
                            CellView.Date(
                                id = obj.id,
                                relationKey = column.key,
                                timeInSecs = fieldDate?.timestamp?.time,
                                dateFormat = column.getDateRelationFormat(),
                                relativeDate = fieldDate?.relativeDate,
                                isTimeIncluded = column.isDateIncludeTime == true
                            )
                        }

                        ColumnView.Format.FILE -> {
                            val files = buildList {
                                obj.getValues<Id>(column.key).forEach { id ->
                                    val wrapper = store.get(id)
                                    if (wrapper != null) {
                                        add(
                                            FileView(
                                                id = id,
                                                name = wrapper.name.orEmpty(),
                                                mime = wrapper.fileMimeType.orEmpty(),
                                                ext = wrapper.fileExt.orEmpty(),
                                                icon = ObjectIcon.File(
                                                    mime = wrapper.fileMimeType.orEmpty(),
                                                    extensions = wrapper.fileExt.orEmpty(),
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                            CellView.File(
                                id = obj.id,
                                relationKey = column.key,
                                files = files
                            )
                        }

                        ColumnView.Format.CHECKBOX -> {
                            CellView.Checkbox(
                                id = obj.id,
                                relationKey = column.key,
                                isChecked = obj.getValue<Boolean>(column.key) ?: false
                            )
                        }

                        ColumnView.Format.URL -> {
                            CellView.Url(
                                id = obj.id,
                                relationKey = column.key,
                                url = obj.getValue<String>(column.key).orEmpty()
                            )
                        }

                        ColumnView.Format.EMAIL -> {
                            CellView.Email(
                                id = obj.id,
                                relationKey = column.key,
                                email = obj.getValue<String>(column.key).orEmpty()
                            )
                        }

                        ColumnView.Format.PHONE -> {
                            CellView.Phone(
                                id = obj.id,
                                relationKey = column.key,
                                phone = obj.getValue<String>(column.key).orEmpty()
                            )
                        }

                        ColumnView.Format.OBJECT -> {
                            val objects = obj.map.buildObjectViews(
                                columnKey = column.key,
                                builder = builder,
                                store = store,
                                fieldParser = fieldParser,
                                storeOfObjectTypes = storeOfObjectTypes
                            )
                            CellView.Object(
                                id = obj.id,
                                relationKey = column.key,
                                objects = objects
                            )
                        }

                        ColumnView.Format.TAG -> {
                            val values = obj.getValue<List<Id>>(column.key) ?: emptyList()
                            val options = values.mapNotNull {
                                val wrapper = store.get(it)
                                if (wrapper != null && !wrapper.isEmpty()) {
                                    ObjectWrapper.Option(wrapper.map)
                                } else {
                                    null
                                }
                            }
                            val tags = obj.map.buildTagViews(
                                options = options,
                                relationKey = column.key
                            )
                            CellView.Tag(
                                id = obj.id,
                                relationKey = column.key,
                                tags = tags
                            )
                        }

                        ColumnView.Format.STATUS -> {
                            val value: Id? = obj.getValue<Any>(column.key).let { value ->
                                when (value) {
                                    is Id -> value
                                    is List<*> -> value.typeOf<Id>().firstOrNull()
                                    else -> null
                                }
                            }
                            val options = buildList {
                                if (value != null) {
                                    val wrapper = store.get(value)
                                    if (wrapper != null && !wrapper.isEmpty()) {
                                        add(
                                            ObjectWrapper.Option(wrapper.map)
                                        )
                                    }
                                }
                            }
                            val status = obj.map.buildStatusViews(
                                options = options,
                                relationKey = column.key
                            )
                            CellView.Status(
                                id = obj.id,
                                relationKey = column.key,
                                status = status
                            )
                        }

                        else -> {
                            TODO()
                        }
                    }
                )
            }
        }
    }

    return Viewer.GridView.Row(
        id = obj.id,
        type = type,
        name = name,
        emoji = emoji,
        image = image?.takeIf { it.isNotBlank() }?.let { builder.thumbnail(it) },
        cells = cells,
        layout = layout,
        isChecked = done,
        showIcon = showIcon,
        objectIcon = objectIcon
    )
}

fun Struct.buildFileViews(
    relationKey: Id,
    details: ObjectViewDetails,
): List<FileView> {
    val files = mutableListOf<FileView>()
    val ids = getOrDefault(relationKey, null) ?: return emptyList()
    if (ids is Id) {
        val file = details.getFileObject(ids)
        if (file == null || file.map.isEmpty() || file.isDeleted == true || file.isArchived == true) return emptyList()
        files.add(file.toView())
    } else if (ids is List<*>) {
        ids.filterIsInstance<Id>().forEach { id ->
            val file = details.getFileObject(id)
            if (file == null || file.map.isEmpty() || file.isDeleted == true || file.isArchived == true) return@forEach
            files.add(file.toView())
        }
    }
    return files
}

private fun ObjectWrapper.File.toView() : FileView {
    return FileView(
        id = id,
        name = name.orEmpty(),
        mime = fileMimeType.orEmpty(),
        ext = fileExt.orEmpty(),
        icon = ObjectIcon.File(
            mime = fileMimeType.orEmpty(),
            extensions = fileExt.orEmpty(),
        )
    )
}

fun Struct.buildTagViews(
    options: List<ObjectWrapper.Option>,
    relationKey: Key
): List<TagView> = buildList {
    val tags: List<Id> = when (val value = get(relationKey)) {
        is Id -> listOf(value)
        is List<*> -> value.typeOf()
        else -> emptyList()
    }
    tags.forEach { id ->
        val option = options.find { it.id == id }
        if (option != null && option.isDeleted != true) {
            add(
                TagView(
                    id = option.id,
                    tag = option.name.orEmpty(),
                    color = option.color
                )
            )
        }
    }
}

fun Struct.buildStatusViews(
    options: List<ObjectWrapper.Option>,
    relationKey: Key
): List<StatusView> = buildList {
    val status: Id? = when (val value = get(relationKey)) {
        is Id -> value
        is List<*> -> value.typeOf<Id>().firstOrNull()
        else -> null
    }
    if (status != null) {
        options.forEach { o ->
            if (o.id == status && o.isDeleted != true) {
                add(
                    StatusView(
                        id = o.id,
                        status = o.name.orEmpty(),
                        color = o.color
                    )
                )
            }
        }
    }
}

@Deprecated("Part of soon-to-be-deleted api")
private fun getColumnOptions(
    options: List<Relation.Option>?,
    records: Map<String, Any?>,
    columnKey: String
): List<Relation.Option> {
    val columnOptions = arrayListOf<Relation.Option>()
    val record = records.getOrDefault(columnKey, null)
    if (record != null) {
        handleIds(record).forEach { id ->
            options
                ?.firstOrNull { it.id == id }
                ?.let { columnOptions.add(it) }
        }
    }
    return columnOptions
}

private fun handleIds(ids: Any?): List<String> = when (ids) {
    is Id -> listOf(ids)
    is List<*> -> ids.filterIsInstance<Id>()
    else -> emptyList()
}

fun Any?.filterIdsById(filter: Id): List<Id> {
    val remaining = mutableListOf<Id>()
    if (this is List<*>) {
        typeOf<Id>().forEach { id ->
            if (id != filter) remaining.add(id)
        }
    }
    return remaining.toList()
}

fun ObjectState.dataViewState(): ObjectState.DataView? {
    val state = this as? ObjectState.DataView
    if (state == null || !state.isInitialized) {
        Timber.e("State was not initialized or null")
        return null
    }
    return state
}