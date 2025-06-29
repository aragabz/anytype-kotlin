package com.anytypeio.anytype.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.NavOptions.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.anytypeio.anytype.BuildConfig
import com.anytypeio.anytype.R
import com.anytypeio.anytype.analytics.base.EventsDictionary
import com.anytypeio.anytype.app.AnytypeNotificationService
import com.anytypeio.anytype.app.AnytypeNotificationService.Companion.NOTIFICATION_TYPE
import com.anytypeio.anytype.app.DefaultAppActionManager
import com.anytypeio.anytype.core_models.Relations
import com.anytypeio.anytype.core_models.ThemeMode
import com.anytypeio.anytype.core_models.Wallpaper
import com.anytypeio.anytype.core_models.multiplayer.SpaceUxType
import com.anytypeio.anytype.core_models.primitives.SpaceId
import com.anytypeio.anytype.core_utils.ext.Mimetype
import com.anytypeio.anytype.core_utils.ext.parseActionSendMultipleUris
import com.anytypeio.anytype.core_utils.ext.parseActionSendUri
import com.anytypeio.anytype.core_utils.ext.toast
import com.anytypeio.anytype.core_utils.insets.EDGE_TO_EDGE_MIN_SDK
import com.anytypeio.anytype.core_utils.intents.ActivityCustomTabsHelper
import com.anytypeio.anytype.core_utils.tools.FeatureToggles
import com.anytypeio.anytype.device.AnytypePushService
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.domain.base.BaseUseCase
import com.anytypeio.anytype.domain.theme.GetTheme
import com.anytypeio.anytype.middleware.discovery.MDNSProvider
import com.anytypeio.anytype.navigation.Navigator
import com.anytypeio.anytype.other.DefaultDeepLinkResolver
import com.anytypeio.anytype.presentation.editor.cover.CoverGradient
import com.anytypeio.anytype.presentation.home.OpenObjectNavigation
import com.anytypeio.anytype.presentation.main.MainViewModel
import com.anytypeio.anytype.presentation.main.MainViewModel.Command
import com.anytypeio.anytype.presentation.main.MainViewModelFactory
import com.anytypeio.anytype.presentation.navigation.AppNavigation
import com.anytypeio.anytype.presentation.notifications.NotificationAction
import com.anytypeio.anytype.presentation.notifications.NotificationCommand
import com.anytypeio.anytype.presentation.wallpaper.WallpaperColor
import com.anytypeio.anytype.presentation.wallpaper.WallpaperView
import com.anytypeio.anytype.ui.chats.ChatFragment
import com.anytypeio.anytype.ui.date.DateObjectFragment
import com.anytypeio.anytype.ui.editor.CreateObjectFragment
import com.anytypeio.anytype.ui.editor.EditorFragment
import com.anytypeio.anytype.ui.gallery.GalleryInstallationFragment
import com.anytypeio.anytype.ui.home.HomeScreenFragment
import com.anytypeio.anytype.ui.multiplayer.RequestJoinSpaceFragment
import com.anytypeio.anytype.ui.multiplayer.ShareSpaceFragment
import com.anytypeio.anytype.ui.multiplayer.SpaceJoinRequestFragment
import com.anytypeio.anytype.ui.notifications.NotificationsFragment
import com.anytypeio.anytype.ui.payments.MembershipFragment
import com.anytypeio.anytype.ui.primitives.ObjectTypeFragment
import com.anytypeio.anytype.ui.profile.ParticipantFragment
import com.anytypeio.anytype.ui.sets.ObjectSetFragment
import com.anytypeio.anytype.ui.sharing.SharingFragment
import com.anytypeio.anytype.ui_settings.appearance.ThemeApplicator
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class MainActivity : AppCompatActivity(R.layout.activity_main), AppNavigation.Provider {

    var deepLink: String? = null

    private val vm by viewModels<MainViewModel> { factory }

    private val navigator by lazy { Navigator() }

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var factory: MainViewModelFactory

    @Inject
    lateinit var getTheme: GetTheme

    @Inject
    lateinit var themeApplicator: ThemeApplicator

    @Inject
    lateinit var mdnsProvider: MDNSProvider

    @Inject
    lateinit var featureToggles: FeatureToggles

    val container: FragmentContainerView get() = findViewById(R.id.fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWindowInsets()
        inject()
        setupTheme()

        if (savedInstanceState != null) vm.onRestore()

//        setFragmentLifecycleCallbacks()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.wallpaper.collect { setWallpaper(it) }
                }
                launch {
                    vm.toasts.collect { toast(it) }
                }
                launch {
                    vm.dispatcher.collect { command ->
                        proceedWithNotificationCommand(command)
                    }
                }
                launch {
                    vm.commands.collect { command ->
                        when (command) {
                            is Command.ShowDeletedAccountScreen -> {
                                navigator.deletedAccountScreen(
                                    deadline = command.deadline
                                )
                            }
                            is Command.LogoutDueToAccountDeletion -> {
                                navigator.logout()
                            }
                            is Command.OpenCreateNewType -> {
                                findNavController(R.id.fragment)
                                    .navigate(
                                        R.id.action_global_createObjectFragment,
                                        bundleOf(
                                            CreateObjectFragment.TYPE_KEY to command.type
                                        )
                                    )
                            }
                            is Command.Sharing.Text -> {
                                SharingFragment.text(command.data).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Sharing.Image -> {
                                SharingFragment.image(command.uri).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Sharing.Images -> {
                                SharingFragment.images(command.uris).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Sharing.Videos -> {
                                SharingFragment.videos(command.uris).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Sharing.Files -> {
                                SharingFragment.files(command.uris).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Sharing.File -> {
                                SharingFragment.file(command.uri).show(
                                    supportFragmentManager,
                                    SHARE_DIALOG_LABEL
                                )
                            }
                            is Command.Error -> {
                                toast(command.msg)
                            }
                            is Command.Notifications -> {
                                NotificationsFragment().show(supportFragmentManager, null)
                            }
                            is Command.RequestNotificationPermission -> {
                                runCatching {
                                    val controller = findNavController(R.id.fragment)
                                    val currentDestination = controller.currentDestination
                                    if (currentDestination?.id != R.id.requestNotificationPermissionDialog) {
                                        controller.navigate(R.id.requestNotificationPermissionDialog)
                                    } else {
                                        Timber.w("Request permission dialog already in stack.")
                                    }
                                }.onFailure {
                                    Timber.e(it, "Error while navigation")
                                }
                            }
                            is Command.Navigate -> {
                                proceedWithOpenObjectNavigation(command.destination)
                            }
                            is Command.Deeplink.DeepLinkToObjectNotWorking -> {
                                toast(getString(R.string.multiplayer_deeplink_to_your_object_error))
                            }
                            is Command.LaunchChat -> {
                                runCatching {
                                    val controller = findNavController(R.id.fragment)
                                    controller.popBackStack(R.id.vaultScreen, false)
                                    controller.navigate(
                                        R.id.actionOpenSpaceFromVault,
                                        HomeScreenFragment.args(
                                            space = command.space,
                                            deeplink = null
                                        )
                                    )
                                    controller.navigate(
                                        R.id.chatScreen,
                                        ChatFragment.args(
                                            space = command.space,
                                            ctx = command.chat
                                        )
                                    )
                                }.onFailure {
                                    if (BuildConfig.DEBUG) {
                                        toast("Failed to open chat from push notification")
                                    }
                                }
                            }
                            is Command.Deeplink.DeepLinkToObject -> {
                                when(val effect = command.sideEffect) {
                                    is Command.Deeplink.DeepLinkToObject.SideEffect.SwitchSpace -> {
                                        runCatching {
                                            val controller = findNavController(R.id.fragment)
                                            controller.popBackStack(R.id.vaultScreen, false)
                                            if (effect.chat != null && effect.spaceUxType == SpaceUxType.CHAT) {
                                                controller.navigate(
                                                    R.id.actionOpenChatFromVault,
                                                    ChatFragment.args(
                                                        space = command.space,
                                                        ctx = effect.chat.orEmpty()
                                                    )
                                                )
                                            } else {
                                                controller.navigate(
                                                    R.id.actionOpenSpaceFromVault,
                                                    HomeScreenFragment.args(
                                                        space = command.space,
                                                        deeplink = null
                                                    )
                                                )
                                            }
                                            proceedWithOpenObjectNavigation(command.navigation)
                                        }.onFailure {
                                            Timber.e(it, "Error while switching space when handling deep link to object")
                                        }
                                    }
                                    null -> {
                                        proceedWithOpenObjectNavigation(command.navigation)
                                    }
                                }
                            }
                            is Command.Deeplink.GalleryInstallation -> {
                                runCatching {
                                    findNavController(R.id.fragment).navigate(
                                        R.id.galleryInstallationScreen,
                                        GalleryInstallationFragment.args(
                                            deepLinkType = command.deepLinkType,
                                            deepLinkSource = command.deepLinkSource
                                        )
                                    )
                                }.onFailure {
                                    Timber.e(it, "Error while navigation for deep link gallery installation")
                                }
                            }
                            is Command.Deeplink.Invite -> {
                                runCatching {
                                    findNavController(R.id.fragment).navigate(
                                        R.id.requestJoinSpaceScreen,
                                        RequestJoinSpaceFragment.args(link = command.link)
                                    )
                                }.onFailure {
                                    Timber.e(it, "Error while navigation for deep link invite")
                                }
                            }
                            is Command.Deeplink.MembershipScreen -> {
                                runCatching {
                                    findNavController(R.id.fragment).navigate(
                                        R.id.paymentsScreen,
                                        MembershipFragment.args(tierId = command.tierId),
                                        NavOptions.Builder().setLaunchSingleTop(true).build()
                                    )
                                }.onFailure {
                                    Timber.w(it, "Error while navigation for deep link membership tier")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (savedInstanceState == null) {
            Timber.d("onSaveInstanceStateNull")
            val action = intent.action
            if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
                proceedWithShareIntent(intent)
            }
        } else {
            Timber.d("onSaveInstanceStateNotNull")
        }
    }

    private fun setFragmentLifecycleCallbacks() {
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
                    super.onFragmentResumed(fm, fragment)
                    Timber.d("onFragmentAdded: $fragment is ${fragment.tag}")
                }

                override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
                    super.onFragmentPaused(fm, fragment)
                    Timber.d("onFragmentPaused: $fragment is ${fragment.tag}")
                }
            }, true
        )
    }

    private fun proceedWithOpenObjectNavigation(dest: OpenObjectNavigation) {
        when (dest) {
            is OpenObjectNavigation.OpenDataView -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.dataViewNavigation,
                        args = ObjectSetFragment.args(
                            ctx = dest.target,
                            space = dest.space
                        ),
                        navOptions = Builder()
                            .setPopUpTo(R.id.homeScreen, true)
                            .build()
                    )
                }.onFailure {
                    Timber.e(it, "Error while data view navigation")
                }
            }

            is OpenObjectNavigation.OpenParticipant -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.participantScreen,
                        ParticipantFragment.args(
                            objectId = dest.target,
                            space = dest.space
                        )
                    )
                }.onFailure {
                    Timber.w("Error while opening participant screen")
                }
            }

            is OpenObjectNavigation.OpenEditor -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.objectNavigation,
                        args = EditorFragment.args(
                            ctx = dest.target,
                            space = dest.space
                        )
                    )
                }.onFailure {
                    Timber.e(it, "Error while editor navigation")
                }
            }

            is OpenObjectNavigation.OpenChat -> {
                toast("Cannot open chat from here")
            }

            is OpenObjectNavigation.UnexpectedLayoutError -> {
                toast(getString(R.string.error_unexpected_layout))
            }

            OpenObjectNavigation.NonValidObject -> {
                toast(getString(R.string.error_non_valid_object))
            }

            is OpenObjectNavigation.OpenDateObject -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.dateObjectScreen,
                        args = DateObjectFragment.args(
                            objectId = dest.target,
                            space = dest.space
                        ),
                        navOptions = Builder()
                            .setPopUpTo(R.id.homeScreen, true)
                            .build()
                    )
                }.onFailure {
                    Timber.e(it, "Error while date object navigation")
                }
            }
            is OpenObjectNavigation.OpenType -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        resId = R.id.objectTypeScreen,
                        args = ObjectTypeFragment.args(
                            objectId = dest.target,
                            space = dest.space
                        )
                    )
                }.onFailure {
                    Timber.e(it, "Error while opening object type in main activity")
                }
            }
            is OpenObjectNavigation.OpenBookmarkUrl -> {
                try {
                    ActivityCustomTabsHelper.openUrl(
                        activity = this,
                        url = dest.url
                    )
                } catch (e: Throwable) {
                    Timber.e(e, "Error opening bookmark URL: ${dest.url}")
                    toast("Failed to open URL")
                }
            }
        }
    }

    private fun setupWindowInsets() {
        if (BuildConfig.USE_NEW_WINDOW_INSET_API && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        if (BuildConfig.USE_EDGE_TO_EDGE && Build.VERSION.SDK_INT >= EDGE_TO_EDGE_MIN_SDK) {
            enableEdgeToEdge()
        }
    }

    private fun setupTheme() {
        runBlocking {
            getTheme(BaseUseCase.None).proceed(
                success = {
                    setTheme(it)
                },
                failure = {
                    Timber.e(it, "Error while setting current app theme")
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (BuildConfig.DEBUG) {
            Timber.d("on NewIntent: $intent")
        }
        when(intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    val data = uri.toString()
                    if (DefaultDeepLinkResolver.isDeepLink(data)) {
                        vm.handleNewDeepLink(DefaultDeepLinkResolver.resolve(data))

                        // Optionally clear to prevent repeat
                        intent.action = null
                        intent.data = null
                        intent.replaceExtras(Bundle())
                    }
                } ?: run {
                    intent.extras?.getString(DefaultAppActionManager.ACTION_CREATE_NEW_TYPE_KEY)?.let {
                        vm.onIntentCreateObject(it)
                    }
                }
            }
            Intent.ACTION_SEND -> {
                proceedWithShareIntent(intent, checkDeepLink = true)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                proceedWithShareIntent(intent, checkDeepLink = true)
            }
            AnytypeNotificationService.NOTIFICATION_INTENT_ACTION -> {
                proceedWithNotificationIntent(intent)
            }
            AnytypePushService.ACTION_OPEN_CHAT -> {
                proceedWithOpenChatIntent(intent)
            }
        }
    }

    private fun proceedWithOpenChatIntent(intent: Intent) {
        val chatId = intent.getStringExtra(Relations.CHAT_ID)
        val spaceId = intent.getStringExtra(Relations.SPACE_ID)
        if (!chatId.isNullOrEmpty() && !spaceId.isNullOrEmpty()) {
            if (!isChatFragmentVisible(chatId)) {
                vm.onOpenChatTriggeredByPush(
                    chatId = chatId,
                    spaceId = spaceId
                )
                // Clearing from-notification-to-chat intent.
                intent.replaceExtras(Bundle())
            } else {
                // Do nothing, already there.
            }
        }
    }

    private fun isChatFragmentVisible(chatId: String): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        return if (currentFragment is ChatFragment) {
            currentFragment.ctx == chatId
        } else {
            false
        }
    }

    /**
     * Main activity is responsible only for checking new deep links.
     * Launch deep links are handled by SplashFragment.
     */
    private fun proceedWithShareIntent(intent: Intent, checkDeepLink: Boolean = false) {
        if (BuildConfig.DEBUG) Timber.d("Proceeding with share intent: $intent")
        when {
            intent.type == Mimetype.MIME_TEXT_PLAIN.value -> {
                handleTextShare(
                    intent = intent,
                    checkDeepLink = checkDeepLink
                )
            }
            intent.type?.startsWith(SHARE_IMAGE_INTENT_PATTERN) == true -> {
                proceedWithImageShareIntent(intent)
            }
            intent.type?.startsWith(SHARE_VIDEO_INTENT_PATTERN) == true -> {
                proceedWithVideoShareIntent(intent)
            }
            intent.type?.startsWith(SHARE_FILE_INTENT_PATTERN) == true -> {
                proceedWithFileShareIntent(intent)
            }
            intent.type == Mimetype.MIME_FILE_ALL.value -> {
                proceedWithFileShareIntent(intent)
            }
            else -> Timber.e("Unexpected scenario: ${intent.type}")
        }
    }

    private fun handleTextShare(intent: Intent, checkDeepLink: Boolean) {
        val raw = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.dataString ?: return

        when {
            checkDeepLink && DefaultDeepLinkResolver.isDeepLink(raw) -> {
                vm.handleNewDeepLink(DefaultDeepLinkResolver.resolve(raw))
            }
            raw.isNotEmpty() && !DefaultDeepLinkResolver.isDeepLink(raw) -> {
                vm.onIntentTextShare(raw)
            }
            else -> {
                Timber.d("handleTextShare, skip handle intent :$raw")
            }
        }
    }

    private fun proceedWithFileShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            vm.onIntentMultipleFilesShare(intent.parseActionSendMultipleUris())
        } else {
            val uri = intent.parseActionSendUri()
            if (uri != null) {
                vm.onIntentMultipleFilesShare(listOf(uri))
            } else {
                toast("Could not parse URI")
            }
        }
    }

    private fun proceedWithImageShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            vm.onIntentMultipleImageShare(uris = intent.parseActionSendMultipleUris())
        } else {
            val uri = intent.parseActionSendUri()
            if (uri != null) {
                vm.onIntentMultipleImageShare(listOf(uri))
            } else {
                toast("Could not parse URI")
            }
        }
    }

    private fun proceedWithVideoShareIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            vm.onIntentMultipleVideoShare(uris = intent.parseActionSendMultipleUris())
        } else {
            val uri = intent.parseActionSendUri()
            if (uri != null) {
                vm.onIntentMultipleVideoShare(listOf(uri))
            } else {
                toast("Could not parse URI")
            }
        }
    }

    private fun proceedWithNotificationIntent(intent: Intent) {
        when(val type = intent.getIntExtra(NOTIFICATION_TYPE, -1)) {
            AnytypeNotificationService.REQUEST_TO_JOIN_TYPE -> {
                val space = intent.getStringExtra(Relations.SPACE_ID)
                val identity = intent.getStringExtra(Relations.IDENTITY)
                if (!space.isNullOrEmpty() && !identity.isNullOrEmpty()) {
                    val notification = intent.getStringExtra(AnytypeNotificationService.NOTIFICATION_ID_KEY).orEmpty()
                    vm.onInterceptNotificationAction(
                        action = NotificationAction.Multiplayer.ViewSpaceJoinRequest(
                            notification = notification,
                            space = SpaceId(space),
                            identity = identity
                        )
                    )
                } else {
                    Timber.w("Missing space or identity")
                }
            }
            AnytypeNotificationService.REQUEST_TO_LEAVE_TYPE -> {
                val space = intent.getStringExtra(Relations.SPACE_ID)
                val identity = intent.getStringExtra(Relations.IDENTITY)
                if (!space.isNullOrEmpty() && !identity.isNullOrEmpty()) {
                    val notification = intent.getStringExtra(AnytypeNotificationService.NOTIFICATION_ID_KEY).orEmpty()
                    vm.onInterceptNotificationAction(
                        action = NotificationAction.Multiplayer.ViewSpaceLeaveRequest(
                            notification = notification,
                            space = SpaceId(space)
                        )
                    )
                } else {
                    Timber.w("Missing space or identity")
                }
            }
            AnytypeNotificationService.REQUEST_APPROVED_TYPE -> {
                val space = intent.getStringExtra(Relations.SPACE_ID)
                if (!space.isNullOrEmpty()) {
                    val notification = intent
                        .getStringExtra(AnytypeNotificationService.NOTIFICATION_ID_KEY)
                        .orEmpty()
                    vm.onInterceptNotificationAction(
                        action = NotificationAction.Multiplayer.GoToSpace(
                            notification = notification,
                            space = SpaceId(space)
                        )
                    )
                }
            }
            else -> {
                toast("Unknown type: $type")
            }
        }
    }

    private fun proceedWithNotificationCommand(command: NotificationCommand) {
        when (command) {
            is NotificationCommand.ViewSpaceJoinRequest -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.spaceJoinRequestScreen,
                        SpaceJoinRequestFragment.args(
                            space = command.space,
                            member = command.member,
                            analyticsRoute = EventsDictionary.Routes.notification
                        )
                    )
                }.onFailure {
                    Timber.e(it, "Error while navigation")
                }
            }

            is NotificationCommand.ViewSpaceLeaveRequest -> {
                runCatching {
                    findNavController(R.id.fragment).navigate(
                        R.id.shareSpaceScreen,
                        ShareSpaceFragment.args(space = command.space)
                    )
                }.onFailure {
                    Timber.e(it, "Error while navigation")
                }
            }

            is NotificationCommand.GoToSpace -> {
                runCatching {
                    findNavController(R.id.fragment).popBackStack(R.id.vaultScreen, false)
                    findNavController(R.id.fragment).navigate(
                        R.id.actionOpenSpaceFromVault,
                        HomeScreenFragment.args(
                            space = command.space.id,
                            deeplink = null
                        )
                    )
                }.onFailure {
                    Timber.e(it, "Error while navigation")
                }
            }
        }
    }

    private fun setTheme(themeMode: ThemeMode) {
        themeApplicator.apply(themeMode)
    }

    private fun setWallpaper(wallpaper: Wallpaper) {
        when (wallpaper) {
            is Wallpaper.Gradient -> {
                when (wallpaper.code) {
                    CoverGradient.YELLOW -> container.setBackgroundResource(R.drawable.cover_gradient_yellow)
                    CoverGradient.RED -> container.setBackgroundResource(R.drawable.cover_gradient_red)
                    CoverGradient.BLUE -> container.setBackgroundResource(R.drawable.cover_gradient_blue)
                    CoverGradient.TEAL -> container.setBackgroundResource(R.drawable.cover_gradient_teal)
                    CoverGradient.PINK_ORANGE -> container.setBackgroundResource(R.drawable.wallpaper_gradient_1)
                    CoverGradient.BLUE_PINK -> container.setBackgroundResource(R.drawable.wallpaper_gradient_2)
                    CoverGradient.GREEN_ORANGE -> container.setBackgroundResource(R.drawable.wallpaper_gradient_3)
                    CoverGradient.SKY -> container.setBackgroundResource(R.drawable.wallpaper_gradient_4)
                }
            }
            is Wallpaper.Default -> {
                container.setBackgroundResource(R.drawable.cover_gradient_default)
            }
            is Wallpaper.Color -> {
                val color = WallpaperColor.entries.find { it.code == wallpaper.code }
                if (color != null) {
                    container.setBackgroundColor(Color.parseColor(color.hex))
                }
            }
            is Wallpaper.Image -> {
                container.setBackgroundResource(R.color.default_dashboard_background_color)
            }
        }
        container.background.alpha = WallpaperView.WALLPAPER_DEFAULT_ALPHA
    }

    override fun onResume() {
        super.onResume()
        mdnsProvider.start()
        navigator.bind(findNavController(R.id.fragment))
    }

    override fun onPause() {
        super.onPause()
        mdnsProvider.stop()
        navigator.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        mdnsProvider.stop()
        release()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        runCatching {
            permissions.forEachIndexed { index, permission ->
                when(permission) {
                    Manifest.permission.POST_NOTIFICATIONS -> {
                        val result = grantResults[index]
                        if (result == PackageManager.PERMISSION_GRANTED)
                            vm.onNotificationPermissionGranted()
                        else
                            vm.onNotificationPermissionDenied()
                    }
                }
            }
        }.onFailure {
            Timber.e(it, "Error while handling permission results")
        }
    }

    override fun nav(): AppNavigation = navigator

    fun inject() {
        componentManager().mainEntryComponent.get().inject(this)
    }

    fun release() {
        componentManager().mainEntryComponent.release()
    }

    companion object {
        const val SHARE_DIALOG_LABEL = "anytype.dialog.share.label"
        const val SHARE_IMAGE_INTENT_PATTERN = "image/"
        const val SHARE_VIDEO_INTENT_PATTERN = "video/"
        const val SHARE_FILE_INTENT_PATTERN = "application/"
    }
}
