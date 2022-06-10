package com.anytypeio.anytype.presentation.objects.menu

import com.anytypeio.anytype.core_models.Id
import kotlinx.coroutines.flow.Flow

interface ObjectMenuOptionsProvider {

    data class Options(
        val hasIcon: Boolean,
        val hasCover: Boolean,
        val hasLayout: Boolean,
        val hasRelations: Boolean,
        val hasHistory: Boolean
    ) {
        companion object {
            val ALL = Options(
                hasIcon = true,
                hasCover = true,
                hasLayout = true,
                hasRelations = true,
                hasHistory = true
            )
        }
    }

    fun provide(ctx: Id, isLocked: Boolean): Flow<Options>
}