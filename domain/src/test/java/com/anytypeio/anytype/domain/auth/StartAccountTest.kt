package com.anytypeio.anytype.domain.auth

import com.anytypeio.anytype.core_models.Account
import com.anytypeio.anytype.core_models.AccountSetup
import com.anytypeio.anytype.core_models.AccountStatus
import com.anytypeio.anytype.core_models.Config
import com.anytypeio.anytype.core_models.CoroutineTestRule
import com.anytypeio.anytype.core_models.FeaturesConfig
import com.anytypeio.anytype.domain.auth.interactor.StartAccount
import com.anytypeio.anytype.domain.auth.repo.AuthRepository
import com.anytypeio.anytype.domain.base.Either
import com.anytypeio.anytype.domain.config.ConfigStorage
import com.anytypeio.anytype.domain.config.FeaturesConfigProvider
import com.anytypeio.anytype.test_utils.MockDataFactory
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertTrue

class StartAccountTest {

    @ExperimentalCoroutinesApi
    @get:Rule
    var rule = CoroutineTestRule()

    @Mock
    lateinit var repo: AuthRepository

    @Mock
    lateinit var featuresConfigProvider: FeaturesConfigProvider

    @Mock
    lateinit var configStorage: ConfigStorage

    lateinit var startAccount: StartAccount

    val config = Config(
        home = MockDataFactory.randomUuid(),
        gateway = MockDataFactory.randomUuid(),
        profile = MockDataFactory.randomUuid()
    )

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        startAccount = StartAccount(
            repository = repo,
            configStorage = configStorage,
            featuresConfigProvider = featuresConfigProvider
        )
    }

    @Test
    fun `should select account, set it as current user account and save it`() = runBlocking {

        val id = MockDataFactory.randomString()
        val path = MockDataFactory.randomString()

        val params = StartAccount.Params(
            id = id,
            path = path
        )

        val account = Account(
            id = id,
            name = MockDataFactory.randomString(),
            avatar = null,
            color = null
        )

        val featuresConfig = FeaturesConfig(
            enableDataView = false,
            enableDebug = false,
            enablePrereleaseChannel = false
        )

        repo.stub {
            onBlocking {
                startAccount(
                    id = id,
                    path = path
                )
            } doReturn AccountSetup(
                account = account,
                features = featuresConfig,
                status = AccountStatus.Active,
                config = config
            )
        }

        startAccount.run(params)

        verify(repo, times(1)).startAccount(
            id = id,
            path = path
        )

        verify(repo, times(1)).saveAccount(account)

        verify(repo, times(1)).setCurrentAccount(account.id)

        verifyNoMoreInteractions(repo)
    }

    @Test
    fun `should return unit when use case is successfully completed`() = runBlocking {

        val id = MockDataFactory.randomString()
        val path = MockDataFactory.randomString()

        val params = StartAccount.Params(
            id = id,
            path = path
        )

        val account = Account(
            id = id,
            name = MockDataFactory.randomString(),
            avatar = null,
            color = null
        )

        val featuresConfig = FeaturesConfig(
            enableDataView = false,
            enableDebug = false,
            enablePrereleaseChannel = false
        )

        repo.stub {
            onBlocking {
                startAccount(
                    id = id,
                    path = path
                )
            } doReturn AccountSetup(
                account = account,
                features = featuresConfig,
                status = AccountStatus.Active,
                config = config
            )
        }

        val result = startAccount.run(params)

        assertTrue { result == Either.Right(Pair(account.id, AccountStatus.Active)) }
    }

    @Test
    fun `should set default flavour config`() = runBlocking {

        val id = MockDataFactory.randomString()
        val path = MockDataFactory.randomString()

        val params = StartAccount.Params(
            id = id,
            path = path
        )

        val account = Account(
            id = id,
            name = MockDataFactory.randomString(),
            avatar = null,
            color = null
        )

        val featuresConfig = FeaturesConfig(
            enableDataView = null,
            enableDebug = null,
            enablePrereleaseChannel = null
        )

        repo.stub {
            onBlocking {
                startAccount(
                    id = id,
                    path = path
                )
            } doReturn AccountSetup(
                account = account,
                features = featuresConfig,
                status = AccountStatus.Active,
                config = config
            )
        }

        val result = startAccount.run(params)

        verify(featuresConfigProvider, times(1)).set(
            enableDataView = false,
            enableDebug = false,
            enableChannelSwitch = false,
            enableSpaces = false
        )

        assertTrue { result == Either.Right(Pair(account.id, AccountStatus.Active)) }
    }

    @Test
    fun `should set proper flavour config`() = runBlocking {

        val id = MockDataFactory.randomString()
        val path = MockDataFactory.randomString()

        val params = StartAccount.Params(
            id = id,
            path = path
        )

        val account = Account(
            id = id,
            name = MockDataFactory.randomString(),
            avatar = null,
            color = null
        )

        val featuresConfig = FeaturesConfig(
            enableDataView = true,
            enableDebug = false,
            enablePrereleaseChannel = true
        )

        repo.stub {
            onBlocking {
                startAccount(
                    id = id,
                    path = path
                )
            } doReturn AccountSetup(
                account = account,
                features = featuresConfig,
                status = AccountStatus.Active,
                config = config
            )
        }

        val result = startAccount.run(params)

        verify(featuresConfigProvider, times(1)).set(
            enableDataView = true,
            enableDebug = false,
            enableChannelSwitch = true,
            enableSpaces = false
        )

        assertTrue { result == Either.Right(Pair(account.id, AccountStatus.Active)) }
    }
}