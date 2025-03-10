package com.anytypeio.anytype.presentation.membership.provider

import com.anytypeio.anytype.core_models.Command
import com.anytypeio.anytype.core_models.membership.Membership
import com.anytypeio.anytype.core_models.membership.MembershipStatus
import com.anytypeio.anytype.core_models.membership.MembershipTierData
import com.anytypeio.anytype.core_models.membership.TierId
import com.anytypeio.anytype.domain.account.AwaitAccountStartManager
import com.anytypeio.anytype.domain.base.AppCoroutineDispatchers
import com.anytypeio.anytype.domain.block.repo.BlockRepository
import com.anytypeio.anytype.domain.misc.DateProvider
import com.anytypeio.anytype.domain.misc.LocaleProvider
import com.anytypeio.anytype.domain.workspace.MembershipChannel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import timber.log.Timber

interface MembershipProvider {

    fun status(): Flow<MembershipStatus>
    fun activeTier(): Flow<TierId>

    class Default(
        private val dispatchers: AppCoroutineDispatchers,
        private val membershipChannel: MembershipChannel,
        private val awaitAccountStartManager: AwaitAccountStartManager,
        private val localeProvider: LocaleProvider,
        private val repo: BlockRepository,
        private val dateProvider: DateProvider
    ) : MembershipProvider {

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun status(): Flow<MembershipStatus> {
            return awaitAccountStartManager.state().flatMapLatest { state ->
                when(state) {
                    AwaitAccountStartManager.State.Started -> buildStatusFlow(
                        initial = proceedWithGettingMembership()
                    )
                    AwaitAccountStartManager.State.Init -> emptyFlow()
                    AwaitAccountStartManager.State.Stopped -> emptyFlow()
                }
            }.catch { e -> Timber.e(e) }
                .flowOn(dispatchers.io)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun activeTier(): Flow<TierId> {
            return awaitAccountStartManager.state().flatMapLatest { state ->
                when(state) {
                    AwaitAccountStartManager.State.Started -> buildActiveTierFlow(
                        initial = proceedWithGettingMembership()
                    )
                    AwaitAccountStartManager.State.Init -> emptyFlow()
                    AwaitAccountStartManager.State.Stopped -> emptyFlow()
                }
            }.catch { e -> Timber.e(e) }
                .flowOn(dispatchers.io)
        }

        private fun buildActiveTierFlow(
            initial: Membership?
        ): Flow<TierId> {
            return membershipChannel
                .observe()
                .scan(initial) { _, events ->
                    events.lastOrNull()?.membership
                }.filterNotNull()
                .map { membership ->
                    TierId(membership.tier)
                }
        }

        private fun buildStatusFlow(
            initial: Membership?
        ): Flow<MembershipStatus> {
            return membershipChannel
                .observe()
                .scan(initial) { _, events ->
                    events.lastOrNull()?.membership
                }.filterNotNull()
                .map { membership ->
                    val tiers = proceedWithGettingTiers().filter { SHOW_TEST_TIERS || !it.isTest }.sortedBy { it.id }
                    val newStatus = toMembershipStatus(
                        membership = membership,
                        tiers = tiers
                    )
                    Timber.d("MembershipProvider, newState: $newStatus")
                    newStatus
                }
        }

        private suspend fun proceedWithGettingMembership(): Membership? {
            val command = Command.Membership.GetStatus(
                noCache = true
            )
            return repo.membershipStatus(command)
        }

        private suspend fun proceedWithGettingTiers(): List<MembershipTierData> {
            val tiersParams = Command.Membership.GetTiers(
                noCache = true,
                locale = localeProvider.language()
            )
            return repo.membershipGetTiers(tiersParams)
        }

        private fun toMembershipStatus(
            membership: Membership,
            tiers: List<MembershipTierData>
        ): MembershipStatus {
            val formattedDateEnds = dateProvider.formatToDateString(
                timestamp = membership.dateEnds,
                pattern = DATE_FORMAT
            )
            return MembershipStatus(
                activeTier = TierId(membership.tier),
                status = membership.membershipStatusModel,
                dateEnds = membership.dateEnds,
                paymentMethod = membership.paymentMethod,
                anyName = membership.nameServiceName,
                tiers = tiers,
                formattedDateEnds = formattedDateEnds,
                userEmail = membership.userEmail
            )
        }

        companion object {
            const val SHOW_TEST_TIERS = false
            const val DATE_FORMAT = "d MMM yyyy"
        }
    }
}