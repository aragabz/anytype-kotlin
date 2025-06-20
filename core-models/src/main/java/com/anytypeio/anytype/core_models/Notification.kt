package com.anytypeio.anytype.core_models

import com.anytypeio.anytype.core_models.multiplayer.SpaceMemberPermissions
import com.anytypeio.anytype.core_models.primitives.SpaceId

data class Notification(
    val id: Id,
    val createTime: Long,
    val status: NotificationStatus,
    val isLocal: Boolean,
    val payload: NotificationPayload,
    val space: SpaceId,
    val aclHeadId: String? = null
) {

    sealed class Event {
        abstract val notification: Notification?
        data class Update(
            override val notification: Notification?
        ) : Event()

        data class Send(
            override val notification: Notification?
        ) : Event()
    }
}

sealed class NotificationPayload {
    data class GalleryImport(
        val processId: Id,
        val errorCode: ImportErrorCode,
        val spaceId: SpaceId,
        val name: String
    ) : NotificationPayload()

    data class RequestToJoin(
        val spaceId: SpaceId,
        val spaceName: String,
        val identity: Id,
        val identityName: String,
        val identityIcon: String
    ) : NotificationPayload()

    data class RequestToLeave(
        val spaceId: SpaceId,
        val spaceName: String,
        val identity: String,
        val identityName: String,
        val identityIcon: String
    ) : NotificationPayload()

    data class ParticipantRequestApproved(
        val spaceId: SpaceId,
        val spaceName: String,
        val permissions: SpaceMemberPermissions? = null
    ) : NotificationPayload()

    data class ParticipantWithoutApprovalRequestApproved(
        val spaceId: SpaceId,
        val spaceName: String
    ) : NotificationPayload()


    data class ParticipantRemove(
        val spaceId: SpaceId,
        val spaceName: String,
        val identity: String,
        val identityName: String,
        val identityIcon: String
    ) : NotificationPayload()

    data class ParticipantRequestDecline(
        val spaceId: SpaceId,
        val spaceName: String
    ) : NotificationPayload()

    data class ParticipantPermissionsChange(
        val spaceId: SpaceId,
        val spaceName: String,
        val permissions: SpaceMemberPermissions
    ) : NotificationPayload()

    data class Unsupported(val type: String) : NotificationPayload()

    data object Test : NotificationPayload()
}

enum class NotificationStatus {
    CREATED, SHOWN, READ, REPLIED
}

enum class NotificationActionType {
    CLOSE
}