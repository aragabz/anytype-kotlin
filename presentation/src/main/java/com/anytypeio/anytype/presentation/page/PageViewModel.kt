package com.anytypeio.anytype.presentation.page

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.anytypeio.anytype.analytics.base.Analytics
import com.anytypeio.anytype.analytics.base.EventsDictionary
import com.anytypeio.anytype.analytics.base.EventsDictionary.PAGE_CREATE
import com.anytypeio.anytype.analytics.base.EventsDictionary.PAGE_MENTION_CREATE
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_ACTION_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_ADD_BLOCK
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_BOOKMARK
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_DOCUMENT_ICON_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_DOCUMENT_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_MARKUP_LINK
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_MENTION_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_MULTI_SELECT_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_PROFILE_ICON_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_PROFILE_MENU
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_STYLE
import com.anytypeio.anytype.analytics.base.EventsDictionary.POPUP_TURN_INTO
import com.anytypeio.anytype.analytics.base.EventsDictionary.PROP_STYLE
import com.anytypeio.anytype.analytics.base.sendEvent
import com.anytypeio.anytype.analytics.event.EventAnalytics
import com.anytypeio.anytype.analytics.props.Props
import com.anytypeio.anytype.core_ui.common.Alignment
import com.anytypeio.anytype.core_ui.common.Focusable
import com.anytypeio.anytype.core_ui.common.Markup
import com.anytypeio.anytype.core_ui.extensions.updateSelection
import com.anytypeio.anytype.core_ui.features.page.*
import com.anytypeio.anytype.core_ui.features.page.scrollandmove.ScrollAndMoveTargetDescriptor.Companion.END_RANGE
import com.anytypeio.anytype.core_ui.features.page.scrollandmove.ScrollAndMoveTargetDescriptor.Companion.INNER_RANGE
import com.anytypeio.anytype.core_ui.features.page.scrollandmove.ScrollAndMoveTargetDescriptor.Companion.START_RANGE
import com.anytypeio.anytype.core_ui.features.page.styling.StylingEvent
import com.anytypeio.anytype.core_ui.features.page.styling.StylingMode
import com.anytypeio.anytype.core_ui.model.UiBlock
import com.anytypeio.anytype.core_ui.state.ControlPanelState
import com.anytypeio.anytype.core_ui.widgets.ActionItemType
import com.anytypeio.anytype.core_ui.widgets.toolbar.SearchToolbarWidget
import com.anytypeio.anytype.core_ui.widgets.toolbar.adapter.Mention
import com.anytypeio.anytype.core_ui.widgets.toolbar.adapter.MentionAdapter
import com.anytypeio.anytype.core_ui.widgets.toolbar.adapter.getMentionName
import com.anytypeio.anytype.core_utils.common.EventWrapper
import com.anytypeio.anytype.core_utils.ext.*
import com.anytypeio.anytype.core_utils.ui.ViewStateViewModel
import com.anytypeio.anytype.domain.base.Result
import com.anytypeio.anytype.domain.block.interactor.RemoveLinkMark
import com.anytypeio.anytype.domain.block.interactor.UpdateLinkMarks
import com.anytypeio.anytype.domain.block.model.Block
import com.anytypeio.anytype.domain.block.model.Block.Content
import com.anytypeio.anytype.domain.block.model.Block.Prototype
import com.anytypeio.anytype.domain.block.model.Position
import com.anytypeio.anytype.domain.common.Document
import com.anytypeio.anytype.domain.common.Id
import com.anytypeio.anytype.domain.editor.Editor
import com.anytypeio.anytype.domain.error.Error
import com.anytypeio.anytype.domain.event.interactor.InterceptEvents
import com.anytypeio.anytype.domain.event.model.Event
import com.anytypeio.anytype.domain.event.model.Payload
import com.anytypeio.anytype.domain.ext.*
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.domain.page.*
import com.anytypeio.anytype.domain.page.navigation.GetListPages
import com.anytypeio.anytype.presentation.BuildConfig
import com.anytypeio.anytype.presentation.common.StateReducer
import com.anytypeio.anytype.presentation.common.SupportCommand
import com.anytypeio.anytype.presentation.mapper.mark
import com.anytypeio.anytype.presentation.mapper.style
import com.anytypeio.anytype.presentation.mapper.toMentionView
import com.anytypeio.anytype.presentation.navigation.AppNavigation
import com.anytypeio.anytype.presentation.navigation.SupportNavigation
import com.anytypeio.anytype.presentation.page.ControlPanelMachine.Interactor
import com.anytypeio.anytype.presentation.page.TurnIntoConstants.excludeCategoriesForDivider
import com.anytypeio.anytype.presentation.page.TurnIntoConstants.excludeTypesForDotsDivider
import com.anytypeio.anytype.presentation.page.TurnIntoConstants.excludeTypesForLineDivider
import com.anytypeio.anytype.presentation.page.TurnIntoConstants.excludeTypesForText
import com.anytypeio.anytype.presentation.page.editor.*
import com.anytypeio.anytype.presentation.page.model.TextUpdate
import com.anytypeio.anytype.presentation.page.render.BlockViewRenderer
import com.anytypeio.anytype.presentation.page.render.DefaultBlockViewRenderer
import com.anytypeio.anytype.presentation.page.search.search
import com.anytypeio.anytype.presentation.page.selection.SelectionStateHolder
import com.anytypeio.anytype.presentation.page.toggle.ToggleStateHolder
import com.anytypeio.anytype.presentation.util.Bridge
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.regex.Pattern

class PageViewModel(
    private val openPage: OpenPage,
    private val closePage: ClosePage,
    private val createPage: CreatePage,
    private val createDocument: CreateDocument,
    private val createNewDocument: CreateNewDocument,
    private val archiveDocument: ArchiveDocument,
    private val interceptEvents: InterceptEvents,
    private val updateLinkMarks: UpdateLinkMarks,
    private val removeLinkMark: RemoveLinkMark,
    private val reducer: StateReducer<List<Block>, Event>,
    private val urlBuilder: UrlBuilder,
    private val renderer: DefaultBlockViewRenderer,
    private val orchestrator: Orchestrator,
    private val getListPages: GetListPages,
    private val analytics: Analytics,
    private val bridge: Bridge<Payload>
) : ViewStateViewModel<ViewState>(),
    SupportNavigation<EventWrapper<AppNavigation.Command>>,
    SupportCommand<Command>,
    BlockViewRenderer by renderer,
    ToggleStateHolder by renderer,
    SelectionStateHolder by orchestrator.memory.selections,
    StateReducer<List<Block>, Event> by reducer {

    val searchResultScrollPosition = MutableStateFlow(NO_SEARCH_RESULT_POSITION)

    private val session = MutableStateFlow(Session.IDLE)

    private val views: List<BlockView> get() = orchestrator.stores.views.current()

    private val jobs = mutableListOf<Job>()

    private var mode = EditorMode.EDITING

    private val controlPanelInteractor = Interactor(viewModelScope)
    val controlPanelViewState = MutableLiveData<ControlPanelState>()

    /**
     * Sends renderized document to UI
     */
    private val renderCommand = Proxy.Subject<Unit>()

    /**
     * Renderizes document, create views from it, dispatches them to [renderCommand]
     */
    private val renderizePipeline = Proxy.Subject<Document>()

    private val markupActionPipeline = Proxy.Subject<MarkupAction>()

    private val titleChannel = Channel<String>()
    private val titleChanges = titleChannel.consumeAsFlow()

    /**
     * Currently opened document id.
     */
    var context: String = EMPTY_CONTEXT

    /**
     * Current document
     */
    var blocks: Document = emptyList()

    private val _focus: MutableLiveData<Id> = MutableLiveData()
    val focus: LiveData<Id> = _focus

    private val _toasts: Channel<String> = Channel()
    val toasts: Flow<String> get() = _toasts.consumeAsFlow()

    /**
     * Open gallery and search media files for block with that id
     */
    private var mediaBlockId = ""

    /**
     * Current position of last mentionFilter or -1 if none
     */
    private var mentionFrom = -1

    override val navigation = MutableLiveData<EventWrapper<AppNavigation.Command>>()
    override val commands = MutableLiveData<EventWrapper<Command>>()

    init {
        startHandlingTextChanges()
        startProcessingFocusChanges()
        startProcessingTitleChanges()
        startProcessingControlPanelViewState()
        startObservingPayload()
        startObservingErrors()
        processRendering()
        processMarkupChanges()
        viewModelScope.launch { orchestrator.start() }
    }

    private fun startProcessingFocusChanges() {
        viewModelScope.launch {
            orchestrator.stores.focus.stream().collect {
                if (it.isEmpty) {
                    orchestrator.stores.textSelection.update(Editor.TextSelection.empty())
                }
                _focus.postValue(it.id)
            }
        }
    }

    private fun startProcessingTitleChanges() {
        titleChanges
            .debounce(TEXT_CHANGES_DEBOUNCE_DURATION)
            .onEach { update -> proceedWithUpdatingDocumentTitle(update) }
            .launchIn(viewModelScope)
    }

    private fun proceedWithUpdatingDocumentTitle(update: String) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Document.UpdateTitle(
                    context = context,
                    title = update
                )
            )
        }
    }

    private fun startObservingPayload() {
        viewModelScope.launch {
            orchestrator
                .proxies
                .payloads
                .stream()
                .map { payload -> processEvents(payload.events) }
                .collect { viewModelScope.launch { refresh() } }
        }
    }

    private fun startObservingErrors() {
        viewModelScope.launch {
            orchestrator.proxies.errors
                .stream()
                .collect { _toasts.offer(it.message ?: "Unknown error") }
        }
        viewModelScope.launch {
            orchestrator.proxies.toasts
                .stream()
                .collect { _toasts.send(it) }
        }
    }

    private suspend fun processEvents(events: List<Event>) {
        Timber.d("Blocks before handling events: $blocks")
        Timber.d("Events: $events")
        events.forEach { event ->
            if (event is Event.Command.ShowBlock) {
                orchestrator.stores.details.update(event.details)
            }
            if (event is Event.Command.UpdateDetails) {
                orchestrator.stores.details.add(event.target, event.details)
            }
            blocks = reduce(blocks, event)
        }
        Timber.d("Blocks after handling events: $blocks")
    }

    private fun startProcessingControlPanelViewState() {
        viewModelScope.launch {
            controlPanelInteractor
                .state()
                .distinctUntilChanged()
                .collect { controlPanelViewState.postValue(it) }
        }
    }

    private fun processMarkupChanges() {
        markupActionPipeline
            .stream()
            .withLatestFrom(
                orchestrator.stores.textSelection
                    .stream()
                    .distinctUntilChanged()
            )
            { a, b -> Pair(a, b) }
            .onEach { (action, textSelection) ->
                val range = textSelection.selection
                if (textSelection.isNotEmpty && range != null && range.first != range.last) {
                    if (action.type == Markup.Type.LINK) {
                        val block = blocks.first { it.id == textSelection.id }
                        stateData.value = ViewState.OpenLinkScreen(
                            pageId = context,
                            block = block,
                            range = IntRange(
                                start = range.first,
                                endInclusive = range.last.dec()
                            )
                        )
                    } else {
                        applyMarkup(
                            selection = Pair(textSelection.id, range),
                            action = action
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyLinkMarkup(
        blockId: String, link: String, range: IntRange
    ) {
        val targetBlock = blocks.first { it.id == blockId }
        val targetContent = targetBlock.content as Content.Text
        val linkMark = Content.Text.Mark(
            type = Content.Text.Mark.Type.LINK,
            range = IntRange(start = range.first, endInclusive = range.last.inc()),
            param = link
        )
        val marks = targetContent.marks

        updateLinkMarks(
            scope = viewModelScope,
            params = UpdateLinkMarks.Params(
                marks = marks,
                newMark = linkMark
            ),
            onResult = { result ->
                result.either(
                    fnL = { throwable ->
                        Timber.e("Error update marks:${throwable.message}")
                    },
                    fnR = { marks ->
                        val sortedMarks = marks.sortByType()
                        val newContent = targetContent.copy(marks = sortedMarks)
                        val newBlock = targetBlock.copy(content = newContent)
                        rerenderingBlocks(newBlock)
                        proceedWithUpdatingText(
                            intent = Intent.Text.UpdateText(
                                context = context,
                                text = newBlock.content.asText().text,
                                target = targetBlock.id,
                                marks = sortedMarks
                            )
                        )
                    }
                )
            }
        )
    }

    private suspend fun applyMarkup(
        selection: Pair<String, IntRange>,
        action: MarkupAction
    ) {
        val target = blocks.first { block -> block.id == selection.first }

        val new = target.markup(
            type = action.type,
            param = action.param,
            range = selection.second
        )

        blocks = blocks.map { block ->
            if (block.id != target.id)
                block
            else
                new
        }

        refresh()

        proceedWithUpdatingText(
            intent = Intent.Text.UpdateText(
                context = context,
                target = new.id,
                text = new.content<Content.Text>().text,
                marks = new.content<Content.Text>().marks
            )
        )
    }

    private fun rerenderingBlocks(block: Block) =
        viewModelScope.launch {
            blocks = blocks.map {
                if (it.id != block.id)
                    it
                else
                    block
            }
            refresh()
        }

    private fun processRendering() {

        // stream to UI

        renderCommand
            .stream()
            .switchToLatestFrom(orchestrator.stores.views.stream())
            .onEach { dispatchToUI(it) }
            .launchIn(viewModelScope)

        // renderize, in order to send to UI

        renderizePipeline
            .stream()
            .filter { it.isNotEmpty() }
            .onEach { document -> refreshStyleToolbar(document) }
            .withLatestFrom(
                orchestrator.stores.focus.stream(),
                orchestrator.stores.details.stream()
            ) { models, focus, details ->
                models.asMap().render(
                    mode = mode,
                    indent = INITIAL_INDENT,
                    anchor = context,
                    focus = focus,
                    root = models.first { it.id == context },
                    details = details
                )
            }
            .catch { emit(emptyList()) }
            .onEach { views ->
                orchestrator.stores.views.update(views)
                renderCommand.send(Unit)
            }
            .launchIn(viewModelScope)
    }

    private fun refreshStyleToolbar(document: Document) {
        controlPanelViewState.value?.let { state ->
            if (state.stylingToolbar.isVisible) {
                state.stylingToolbar.target?.id?.let { targetId ->
                    controlPanelInteractor.onEvent(
                        event = ControlPanelMachine.Event.OnRefresh.StyleToolbar(
                            target = document.find { it.id == targetId },
                            selection = orchestrator.stores.textSelection.current().selection
                        )
                    )
                }
            }
        }
    }

    private fun dispatchToUI(views: List<BlockView>) {
        stateData.postValue(
            ViewState.Success(
                blocks = views
            )
        )
    }

    private fun startHandlingTextChanges() {
        orchestrator
            .proxies
            .changes
            .stream()
            .filterNotNull()
            .onEach { update ->
                orchestrator.textInteractor.consume(update, context)
            }
            .launchIn(viewModelScope)

        orchestrator
            .proxies
            .saves
            .stream()
            .debounce(TEXT_CHANGES_DEBOUNCE_DURATION)
            .filterNotNull()
            .onEach { update ->
                blocks = blocks.map { block ->
                    if (block.id == update.target) {
                        block.updateText(update)
                    } else
                        block
                }
            }
            .map { update ->
                Intent.Text.UpdateText(
                    context = context,
                    target = update.target,
                    text = update.text,
                    marks = update.markup.filter { it.range.first != it.range.last }
                )
            }
            .onEach { params -> proceedWithUpdatingText(params) }
            .launchIn(viewModelScope)
    }

    private fun proceedWithUpdatingText(intent: Intent.Text.UpdateText) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(intent)
        }
    }

    fun onStart(id: Id) {
        Timber.d("onStart")

        context = id

        stateData.postValue(ViewState.Loading)

        jobs += viewModelScope.launch {
            interceptEvents
                .build(InterceptEvents.Params(context))
                .map { events -> processEvents(events) }
                .collect { refresh() }
        }

        jobs += viewModelScope.launch {
            bridge
                .flow()
                .filter { it.context == context }
                .collect { orchestrator.proxies.payloads.send(it) }
        }

        viewModelScope.launch {
            openPage(OpenPage.Params(id)).proceed(
                success = { result ->
                    when (result) {
                        is Result.Success -> {
                            session.value = Session.OPEN
                            onStartFocusing(result.data)
                            orchestrator.proxies.payloads.send(result.data)
                        }
                        is Result.Failure -> {
                            session.value = Session.ERROR
                            if (result.error is Error.BackwardCompatibility)
                                dispatch(Command.AlertDialog)
                        }
                    }
                },
                failure = {
                    session.value = Session.ERROR
                    Timber.e(it, "Error while opening page with id: $id")
                }
            )
        }
    }

    private fun onStartFocusing(payload: Payload) {
        val event = payload.events.find { it is Event.Command.ShowBlock }
        if (event is Event.Command.ShowBlock) {
            val root = event.blocks.find { it.id == context }
            when {
                root == null -> Timber.e("Could not find the root block on initial focusing")
                root.children.size == 1 -> {
                    val first = event.blocks.first { it.id == root.children.first() }
                    val content = first.content
                    if (content is Content.Layout && content.type == Content.Layout.Type.HEADER) {
                        val title = event.blocks.title()
                        val focus = Editor.Focus(id = title.id, cursor = Editor.Cursor.End)
                        viewModelScope.launch { orchestrator.stores.focus.update(focus) }
                        controlPanelInteractor.onEvent(
                            ControlPanelMachine.Event.OnFocusChanged(
                                id = title.id, style = Content.Text.Style.TITLE
                            )
                        )
                    }
                }
                else -> Timber.d("Skipping initial focusing, document is not empty.")
            }
        }
    }

    fun onAddLinkPressed(blockId: String, link: String, range: IntRange) {
        applyLinkMarkup(blockId, link, range)
    }

    fun onUnlinkPressed(blockId: String, range: IntRange) {

        val target = blocks.first { it.id == blockId }
        val content = target.content<Content.Text>()
        val marks = content.marks

        viewModelScope.launch {
            removeLinkMark(
                params = RemoveLinkMark.Params(
                    range = range,
                    marks = marks
                )
            ).proceed(
                failure = { Timber.e("Error update marks:${it.message}") },
                success = {
                    val newContent = content.copy(marks = it)
                    val newBlock = target.copy(content = newContent)
                    rerenderingBlocks(newBlock)
                    proceedWithUpdatingText(
                        intent = Intent.Text.UpdateText(
                            context = context,
                            text = newBlock.content.asText().text,
                            target = target.id,
                            marks = it
                        )
                    )
                }
            )
        }
    }

    fun onSystemBackPressed(editorHasChildrenScreens: Boolean) {
        if (editorHasChildrenScreens) {
            dispatch(Command.PopBackStack)
        } else {
            val state = controlPanelViewState.value
            checkNotNull(state) { "Control panel state is null" }
            if (state.stylingToolbar.isVisible) {
                onCloseBlockStyleToolbarClicked()
            } else {
                proceedWithExiting()
            }
        }
    }

    fun onDismissBlockActionMenu(editorHasChildrenScreens: Boolean) {
        onExitActionMode()
        onSystemBackPressed(editorHasChildrenScreens)
    }

    fun onBackButtonPressed() {
        proceedWithExiting()
    }

    fun onBottomSheetHidden() {
        proceedWithExitingToDesktop()
    }

    private fun proceedWithExiting() {
        when (session.value) {
            Session.ERROR -> navigate(EventWrapper(AppNavigation.Command.Exit))
            Session.IDLE -> navigate(EventWrapper(AppNavigation.Command.Exit))
            Session.OPEN -> {
                viewModelScope.launch {
                    closePage(
                        ClosePage.Params(context)
                    ).proceed(
                        success = { navigation.postValue(EventWrapper(AppNavigation.Command.Exit)) },
                        failure = { Timber.e(it, "Error while closing document: $context") }
                    )
                }
            }
        }
    }

    private fun proceedWithExitingToDesktop() {
        closePage(viewModelScope, ClosePage.Params(context)) { result ->
            result.either(
                fnR = { navigateToDesktop() },
                fnL = { Timber.e(it, "Error while closing the test page") }
            )
        }
    }

    fun navigateToDesktop() {
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.SCREEN_DASHBOARD
        )
        navigation.postValue(EventWrapper(AppNavigation.Command.ExitToDesktop))
    }

    @Deprecated("replace by onTextBlockTextChanged")
    fun onTextChanged(
        id: String,
        text: String,
        marks: List<Content.Text.Mark>
    ) {
        val update = TextUpdate.Default(target = id, text = text, markup = marks)
        viewModelScope.launch { orchestrator.proxies.changes.send(update) }
    }

    fun onTitleBlockTextChanged(view: BlockView.Title) {
        val new = views.map { if (it.id == view.id) view else it }
        val update = TextUpdate.Default(
            target = view.id,
            text = view.text ?: EMPTY_TEXT,
            markup = emptyList()
        )
        viewModelScope.launch { orchestrator.stores.views.update(new) }
        viewModelScope.launch { orchestrator.proxies.changes.send(update) }
    }

    fun onTextBlockTextChanged(
        view: BlockView.Text
    ) {

        Timber.d("Text block's text changed: $view")

        val update = if (view is BlockView.Text.Paragraph) TextUpdate.Pattern(
            target = view.id,
            text = view.text,
            markup = view.marks.map { it.mark() }
        ) else TextUpdate.Default(
            target = view.id,
            text = view.text,
            markup = view.marks.map { it.mark() }
        )

        val store = orchestrator.stores.views
        val old = store.current()
        val new = old.map { if (it.id == view.id) view else it }

        viewModelScope.launch { store.update(new) }
        viewModelScope.launch { orchestrator.proxies.changes.send(update) }
    }

    fun onSelectionChanged(id: String, selection: IntRange) {
        viewModelScope.launch {
            orchestrator.stores.textSelection.update(Editor.TextSelection(id, selection))
        }
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnSelectionChanged(id, selection))
    }

    fun onBlockFocusChanged(id: String, hasFocus: Boolean) {
        Timber.d("Focus changed ($hasFocus): $id")
        if (hasFocus) {
            updateFocus(id)
            controlPanelInteractor.onEvent(
                ControlPanelMachine.Event.OnFocusChanged(
                    id = id,
                    style = if (id == context)
                        Content.Text.Style.TITLE
                    else
                        blocks.first { it.id == id }.textStyle()
                )
            )
        }
    }

    private fun proceedWithMergingBlocks(
        target: String,
        previous: String
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.Merge(
                    context = context,
                    previous = previous,
                    pair = Pair(previous, target),
                    previousLength = blocks.find { it.id == previous }?.let { block ->
                        if (block.content is Content.Text) {
                            block.content.asText().text.length
                        } else {
                            null
                        }
                    }
                )
            )
        }
    }

    fun onEnterKeyClicked(
        target: String,
        text: String,
        marks: List<Content.Text.Mark>,
        range: IntRange
    ) {
        val focus = orchestrator.stores.focus.current()
        if (!focus.isEmpty && focus.id == target) {
            proceedWithEnterEvent(focus.id, range, text, marks)
        } else {
            Timber.e("No blocks in focus, emit SplitLineEnter event")
        }
    }

    private fun proceedWithEnterEvent(
        target: Id,
        range: IntRange,
        text: String,
        marks: List<Content.Text.Mark>
    ) {
        if (context == target) {
            onEndLineEnterTitleClicked()
        } else {
            if (text.isEndLineClick(range)) {
                onEndLineEnterClicked(target, text, marks)
            } else {
                proceedWithSplitEvent(target, range, text, marks)
            }
        }
    }

    private fun proceedWithSplitEvent(
        target: Id,
        range: IntRange,
        text: String,
        marks: List<Content.Text.Mark>
    ) {

        val block = blocks.first { it.id == target }
        val content = block.content<Content.Text>()

        blocks = blocks.updateTextContent(target, text, marks)

        viewModelScope.launch {
            orchestrator.proxies.saves.send(null)
            orchestrator.proxies.changes.send(null)
        }

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateText(
                    context = context,
                    target = target,
                    marks = marks,
                    text = text
                )
            )
        }

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.Split(
                    context = context,
                    block = block,
                    range = range,
                    isToggled = if (content.isToggle()) renderer.isToggled(target) else null
                )
            )
        }
    }

    private fun onEndLineEnterTitleClicked() {
        val page = blocks.first { it.id == context }
        val next = page.children.getOrElse(0) { "" }
        proceedWithCreatingNewTextBlock(
            id = next,
            style = Content.Text.Style.P,
            position = Position.TOP
        )
    }

    fun onEndLineEnterClicked(
        id: String,
        text: String,
        marks: List<Content.Text.Mark>
    ) {
        Timber.d("On endline enter clicked")

        val target = blocks.first { it.id == id }

        val content = target.content<Content.Text>().copy(
            text = text,
            marks = marks
        )

        blocks = blocks.replace(
            replacement = { old -> old.copy(content = content) }
        ) { block -> block.id == id }

        if (content.isList() || content.isToggle()) {
            handleEndlineEnterPressedEventForListItem(content, id)
        } else {
            proceedWithCreatingNewTextBlock(
                id = id,
                style = Content.Text.Style.P
            )
        }
    }

    fun onDocumentMenuClicked() {
        blocks.find { it.id == context }?.let { root ->
            val content = root.content
            check(content is Content.Smart)
            when (content.type) {
                Content.Smart.Type.PROFILE -> {
                    dispatch(command = Command.OpenProfileMenu)
                    viewModelScope.sendEvent(
                        analytics = analytics,
                        eventName = POPUP_PROFILE_MENU
                    )
                }
                Content.Smart.Type.PAGE -> {
                    dispatch(command = Command.OpenDocumentMenu)
                    viewModelScope.sendEvent(
                        analytics = analytics,
                        eventName = POPUP_DOCUMENT_MENU
                    )
                }
                else -> {
                    Timber.e("Trying to open menu for unexpected smart content: ${content.type}")
                }
            }
        }
    }

    fun onEmptyBlockBackspaceClicked(id: String) {
        Timber.d("onEmptyBlockBackspaceClicked: $id")
        proceedWithUnlinking(target = id)
    }

    fun onNonEmptyBlockBackspaceClicked(
        id: String,
        text: String,
        marks: List<Content.Text.Mark>
    ) {
        blocks = blocks.map { block ->
            if (block.id == id) {
                block.copy(
                    content = block.content<Content.Text>().copy(
                        text = text,
                        marks = marks
                    )
                )
            } else {
                block
            }
        }

        viewModelScope.launch {
            orchestrator.proxies.saves.send(null)
            orchestrator.proxies.changes.send(null)
        }

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateText(
                    context = context,
                    target = id,
                    marks = marks,
                    text = text
                )
            )
        }

        val index = views.indexOfFirst { it.id == id }

        if (index > 0) {
            val previous = views[index.dec()]
            if (previous is BlockView.Text) {
                proceedWithMergingBlocks(
                    previous = previous.id,
                    target = id
                )
            } else {
                if (previous is BlockView.Title) _toasts.offer("Merging with title currently not supported")
                Timber.d("Skipping merge because previous block is not a text block")
            }
        } else {
            Timber.d("Skipping merge on non-empty-block-backspace-pressed event")
        }
    }

    private fun handleEndlineEnterPressedEventForListItem(
        content: Content.Text,
        id: String
    ) {
        if (content.text.isNotEmpty()) {
            proceedWithCreatingNewTextBlock(id, content.style)
        } else {
            proceedUpdateTextStyle(
                style = Content.Text.Style.P,
                targets = listOf(id)
            )
        }
    }

    private fun proceedWithCreatingNewTextBlock(
        id: String,
        style: Content.Text.Style,
        position: Position = Position.BOTTOM
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Create(
                    context = context,
                    target = id,
                    position = position,
                    prototype = Prototype.Text(style = style)
                )
            )
        }
    }

    private fun updateFocus(id: Id) {
        Timber.d("Updating focus: $id")
        viewModelScope.launch { orchestrator.stores.focus.update(Editor.Focus.id(id)) }
    }

    private fun onBlockLongPressedClicked(target: String, dimensions: BlockDimensions) {
        val views = orchestrator.stores.views.current()
        val view = views.find { it.id == target }
        if (view != null) {
            onEnterActionMode()
            dispatch(
                Command.OpenActionBar(
                    block = view,
                    dimensions = dimensions
                )
            )
            viewModelScope.sendEvent(
                analytics = analytics,
                eventName = POPUP_ACTION_MENU
            )
        } else {
            Timber.e("Could not open action menu on long click. Target view was missing.")
        }
    }

    fun onEditorContextMenuStyleClicked(selection: IntRange) {
        val target = blocks.first { it.id == orchestrator.stores.focus.current().id }
        controlPanelInteractor.onEvent(
            ControlPanelMachine.Event.OnEditorContextMenuStyleClicked(
                target = target,
                selection = selection
            )
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_STYLE
        )
    }

    fun onStylingToolbarEvent(event: StylingEvent) {
        val state = controlPanelViewState.value!!
        when (event) {
            is StylingEvent.Coloring.Text -> {
                proceedWithStylingEvent(state, Markup.Type.TEXT_COLOR, event.color.title)
            }
            is StylingEvent.Coloring.Background -> {
                proceedWithStylingEvent(state, Markup.Type.BACKGROUND_COLOR, event.color.title)
            }
            is StylingEvent.Markup.Bold -> {
                proceedWithStylingEvent(state, Markup.Type.BOLD, null)
            }
            is StylingEvent.Markup.Italic -> {
                proceedWithStylingEvent(state, Markup.Type.ITALIC, null)
            }
            is StylingEvent.Markup.StrikeThrough -> {
                proceedWithStylingEvent(state, Markup.Type.STRIKETHROUGH, null)
            }
            is StylingEvent.Markup.Code -> {
                proceedWithStylingEvent(state, Markup.Type.KEYBOARD, null)
            }
            is StylingEvent.Markup.Link -> {
                proceedWithStylingEvent(state, Markup.Type.LINK, null)
            }
            is StylingEvent.Alignment.Left -> {
                onBlockAlignmentActionClicked(Alignment.START)
            }
            is StylingEvent.Alignment.Center -> {
                onBlockAlignmentActionClicked(Alignment.CENTER)
            }
            is StylingEvent.Alignment.Right -> {
                onBlockAlignmentActionClicked(Alignment.END)
            }
        }
    }

    private fun proceedWithStylingEvent(
        state: ControlPanelState,
        type: Markup.Type,
        param: String?
    ) {
        if (state.stylingToolbar.mode == StylingMode.MARKUP) {
            onStyleToolbarMarkupAction(type, param)
        } else {
            state.stylingToolbar.target?.id?.let { id ->
                when (type) {
                    Markup.Type.ITALIC -> onBlockStyleMarkupActionClicked(id, type)
                    Markup.Type.BOLD -> onBlockStyleMarkupActionClicked(id, type)
                    Markup.Type.STRIKETHROUGH -> onBlockStyleMarkupActionClicked(id, type)
                    Markup.Type.TEXT_COLOR -> onToolbarTextColorAction(id, param)
                    Markup.Type.BACKGROUND_COLOR -> onBlockBackgroundColorAction(id, param)
                    Markup.Type.LINK -> onBlockStyleLinkClicked(id)
                    Markup.Type.KEYBOARD -> onBlockStyleMarkupActionClicked(id, type)
                    Markup.Type.MENTION -> Unit
                }
            } ?: run { Timber.e("Target id was missing for markup styling event: $type") }
        }
    }

    private fun onStyleToolbarMarkupAction(type: Markup.Type, param: String? = null) {
        viewModelScope.launch {
            markupActionPipeline.send(
                MarkupAction(
                    type = type,
                    param = param
                )
            )
        }
    }

    private fun onBlockAlignmentActionClicked(alignment: Alignment) {
        controlPanelViewState.value?.stylingToolbar?.target?.id?.let { id ->
            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.Text.Align(
                        context = context,
                        target = id,
                        alignment = when (alignment) {
                            Alignment.START -> Block.Align.AlignLeft
                            Alignment.CENTER -> Block.Align.AlignCenter
                            Alignment.END -> Block.Align.AlignRight
                        }
                    )
                )
            }
        }
    }

    fun onCloseBlockStyleToolbarClicked() {
        val focused = !orchestrator.stores.focus.current().isEmpty
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.StylingToolbar.OnClose(focused))
    }

    fun onToolbarTextColorAction(id: String, color: String?) {
        check(color != null)
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnBlockTextColorSelected)
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateColor(
                    context = context,
                    target = id,
                    color = color
                )
            )
        }
    }

    private fun onBlockBackgroundColorAction(id: String, color: String?) {
        check(color != null)
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnBlockBackgroundColorSelected)
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateBackgroundColor(
                    context = context,
                    targets = listOf(id),
                    color = color
                )
            )
        }
    }

    private fun onBlockStyleLinkClicked(id: String) {
        val target = blocks.first { it.id == id }
        val range = IntRange(
            start = 0,
            endInclusive = target.content<Content.Text>().text.length.dec()
        )
        stateData.value = ViewState.OpenLinkScreen(context, target, range)
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_MARKUP_LINK
        )
    }

    private fun onBlockStyleMarkupActionClicked(id: String, action: Markup.Type) {

        controlPanelInteractor.onEvent(
            ControlPanelMachine.Event.OnBlockStyleSelected
        )

        val target = blocks.first { it.id == id }
        val content = target.content as Content.Text

        if (content.text.isNotEmpty()) {

            val new = target.markup(
                type = action,
                range = 0..content.text.length,
                param = null
            )

            blocks = blocks.map { block ->
                if (block.id != target.id)
                    block
                else
                    new
            }

            viewModelScope.launch { refresh() }

            viewModelScope.launch {
                proceedWithUpdatingText(
                    intent = Intent.Text.UpdateText(
                        context = context,
                        target = new.id,
                        text = new.content<Content.Text>().text,
                        marks = new.content<Content.Text>().marks
                    )
                )
            }
        }
    }

    fun onActionMenuItemClicked(id: String, action: ActionItemType) {
        when (action) {
            ActionItemType.AddBelow -> {
                onExitActionMode()
                dispatch(Command.PopBackStack)
                proceedWithCreatingNewTextBlock(
                    id = id,
                    style = Content.Text.Style.P
                )
            }
            ActionItemType.TurnInto -> {
                val excludedTypes = mutableListOf<String>()
                val excludedCategories = mutableListOf<String>()
                val target = blocks.first { it.id == id }
                when (val content = target.content) {
                    is Content.Text -> excludedTypes.addAll(excludeTypesForText())
                    is Content.Divider -> {
                        excludedCategories.addAll(excludeCategoriesForDivider())
                        when (content.style) {
                            Content.Divider.Style.LINE -> excludedTypes.addAll(
                                excludeTypesForLineDivider()
                            )
                            Content.Divider.Style.DOTS -> excludedTypes.addAll(
                                excludeTypesForDotsDivider()
                            )
                        }
                    }
                }
                onExitActionMode()
                dispatch(
                    Command.OpenTurnIntoPanel(
                        target = id,
                        excludedCategories = excludedCategories,
                        excludedTypes = excludedTypes
                    )
                )
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = POPUP_TURN_INTO
                )
            }
            ActionItemType.Delete -> {
                proceedWithUnlinking(target = id)
                onExitActionMode()
                dispatch(Command.PopBackStack)
            }
            ActionItemType.Duplicate -> {
                duplicateBlock(target = id)
                onExitActionMode()
                dispatch(Command.PopBackStack)
            }
            ActionItemType.Rename -> {
                _toasts.offer("Rename not implemented")
            }
            ActionItemType.MoveTo -> {
                onExitActionMode()
                dispatch(Command.PopBackStack)
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = EventsDictionary.SCREEN_MOVE_TO
                )
                navigate(
                    EventWrapper(
                        AppNavigation.Command.OpenMoveToScreen(
                            context = context,
                            targets = listOf(id)
                        )
                    )
                )
            }
            ActionItemType.Style -> {
                val textSelection = orchestrator.stores.textSelection.current()
                controlPanelInteractor.onEvent(
                    ControlPanelMachine.Event.OnBlockActionToolbarStyleClicked(
                        target = blocks.first { it.id == id },
                        focused = textSelection.isNotEmpty,
                        selection = textSelection.selection
                    )
                )
                onExitActionMode()
                dispatch(Command.PopBackStack)
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = POPUP_STYLE
                )
            }
            ActionItemType.Download -> {
                viewModelScope.launch {
                    onExitActionMode()
                    dispatch(Command.PopBackStack)
                    delay(300)
                    dispatch(Command.RequestDownloadPermission(id))
                }
            }
            ActionItemType.Replace -> {
                _toasts.offer("Replace not implemented")
            }
            ActionItemType.AddCaption -> {
                _toasts.offer("Add caption not implemented")
            }
            ActionItemType.Divider -> {
                _toasts.offer("not implemented")
            }
        }
    }

    private fun proceedWithUnlinking(target: String) {

        val position = views.indexOfFirst { it.id == target }

        var previous: Id? = null
        var cursor: Int? = null

        if (position <= 0) return

        for (i in position.dec() downTo 0) {
            when (val view = views[i]) {
                is BlockView.Text -> {
                    previous = view.id
                    cursor = view.text.length
                    break
                }
                is BlockView.Code -> {
                    previous = view.id
                    cursor = view.text.length
                    break
                }
                is BlockView.Title -> {
                    previous = view.id
                    cursor = view.text?.length ?: 0
                    break
                }
            }
        }

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Unlink(
                    context = context,
                    targets = listOf(target),
                    previous = previous,
                    next = null,
                    cursor = cursor
                )
            )
        }
    }

    private fun duplicateBlock(target: String) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Duplicate(
                    context = context,
                    target = target
                )
            )
        }
    }

    fun onActionUndoClicked() {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Document.Undo(
                    context = context
                )
            )
        }
    }

    fun onActionRedoClicked() {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Document.Redo(
                    context = context
                )
            )
        }
    }

    fun onEnterSearchModeClicked() {
        mode = EditorMode.SEARCH
        viewModelScope.launch { orchestrator.stores.views.update(views.toReadMode()) }
        viewModelScope.launch { renderCommand.send(Unit) }
        viewModelScope.launch { controlPanelInteractor.onEvent(ControlPanelMachine.Event.SearchToolbar.OnEnterSearchMode) }
    }

    fun onSearchToolbarEvent(event: SearchToolbarWidget.Event) {
        if (mode != EditorMode.SEARCH) return
        when (event) {
            is SearchToolbarWidget.Event.Query -> {
                val query = event.query.trim()
                val update = if (query.isEmpty()) {
                    views.clearSearchHighlights()
                } else {
                    val flags = Pattern.MULTILINE or Pattern.CASE_INSENSITIVE
                    val escaped = Pattern.quote(query)
                    val pattern = Pattern.compile(escaped, flags)
                    views.highlight { pairs ->
                        pairs.map { (key, txt) ->
                            BlockView.Searchable.Field(
                                key = key,
                                highlights = txt.search(pattern)
                            )
                        }
                    }
                }
                viewModelScope.launch { orchestrator.stores.views.update(update) }
                viewModelScope.launch { renderCommand.send(Unit) }
            }
            is SearchToolbarWidget.Event.Next -> {
                val update = views.nextSearchTarget()
                viewModelScope.launch { orchestrator.stores.views.update(update) }
                viewModelScope.launch { renderCommand.send(Unit) }
                val target = update.find { view ->
                    view is BlockView.Searchable && view.searchFields.any { it.isTargeted }
                }
                val pos = update.indexOf(target)
                searchResultScrollPosition.value = pos
            }
            is SearchToolbarWidget.Event.Previous -> {
                val update = views.previousSearchTarget()
                viewModelScope.launch { orchestrator.stores.views.update(update) }
                viewModelScope.launch { renderCommand.send(Unit) }
                val target = update.find { view ->
                    view is BlockView.Searchable && view.searchFields.any { it.isTargeted }
                }
                val pos = update.indexOf(target)
                searchResultScrollPosition.value = pos
            }
            is SearchToolbarWidget.Event.Cancel -> {
                mode = EditorMode.EDITING
                val update = views.clearSearchHighlights().toEditMode()
                viewModelScope.launch { orchestrator.stores.views.update(update) }
                viewModelScope.launch { renderCommand.send(Unit) }
                controlPanelInteractor.onEvent(ControlPanelMachine.Event.SearchToolbar.OnExitSearchMode)
                dispatch(Command.ClearSearchInput)
            }
            is SearchToolbarWidget.Event.Search -> {
                val update = views.nextSearchTarget()
                viewModelScope.launch { orchestrator.stores.views.update(update) }
                viewModelScope.launch { renderCommand.send(Unit) }
                val target = update.find { view ->
                    view is BlockView.Searchable && view.searchFields.any { it.isTargeted }
                }
                val pos = update.indexOf(target)
                searchResultScrollPosition.value = pos
            }
        }
    }

    fun onAddTextBlockClicked(style: Content.Text.Style) {

        val target = blocks.first { it.id == orchestrator.stores.focus.current().id }

        val content = target.content

        if (content is Content.Text && content.text.isEmpty()) {
            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.CRUD.Replace(
                        context = context,
                        target = target.id,
                        prototype = Prototype.Text(style = style)
                    )
                )
            }
        } else {
            proceedWithCreatingNewTextBlock(
                id = target.id,
                style = style,
                position = Position.BOTTOM
            )
        }

        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnAddBlockToolbarOptionSelected)
    }

    private fun onAddLocalVideoClicked(blockId: String) {
        mediaBlockId = blockId
        dispatch(Command.OpenGallery(mediaType = MIME_VIDEO_ALL))
    }

    private fun onAddLocalPictureClicked(blockId: String) {
        mediaBlockId = blockId
        dispatch(Command.OpenGallery(mediaType = MIME_IMAGE_ALL))
    }

    fun onAddLinkToObjectClicked() {

        val focused = blocks.first { it.id == orchestrator.stores.focus.current().id }

        val content = focused.content

        val replace = content is Content.Text && content.text.isEmpty()

        var position: Position = Position.BOTTOM

        var target: Id = focused.id

        if (!replace && focused.id == context) {
            if (focused.children.isEmpty()) {
                position = Position.INNER
            } else {
                position = Position.TOP
                target = focused.children.first()
            }
        }

        proceedWithClearingFocus()

        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.SCREEN_LINK_TO
        )

        navigate(
            EventWrapper(
                AppNavigation.Command.OpenLinkToScreen(
                    target = target,
                    context = context,
                    replace = replace,
                    position = position
                )
            )
        )
    }

    fun onTogglePlaceholderClicked(target: Id) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Create(
                    context = context,
                    target = target,
                    prototype = Prototype.Text(
                        style = Content.Text.Style.P
                    ),
                    position = Position.INNER
                )
            )
        }
    }

    fun onToggleClicked(target: Id) {
        onToggleChanged(target)
        viewModelScope.launch { refresh() }
    }

    private fun onAddLocalFileClicked(blockId: String) {
        mediaBlockId = blockId
        dispatch(Command.OpenGallery(mediaType = MIME_FILE_ALL))
    }

    fun onAddFileBlockClicked(type: Content.File.Type) {
        val focused = blocks.first { it.id == orchestrator.stores.focus.current().id }
        val content = focused.content
        if (content is Content.Text && content.text.isEmpty()) {
            proceedWithReplacingByEmptyFileBlock(
                id = focused.id,
                type = type
            )
        } else {
            proceedWithCreatingEmptyFileBlock(
                id = focused.id,
                type = type,
                position = Position.BOTTOM
            )
        }
    }

    private fun proceedWithCreatingEmptyFileBlock(
        id: String,
        type: Content.File.Type,
        state: Content.File.State = Content.File.State.EMPTY,
        position: Position = Position.BOTTOM
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Create(
                    context = context,
                    target = id,
                    position = position,
                    prototype = Prototype.File(type = type, state = state)
                )
            )
        }
    }

    private fun proceedWithReplacingByEmptyFileBlock(
        id: String,
        type: Content.File.Type,
        state: Content.File.State = Content.File.State.EMPTY
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Replace(
                    context = context,
                    target = id,
                    prototype = Prototype.File(type = type, state = state)
                )
            )
        }
    }

    fun onCheckboxClicked(view: BlockView.Text.Checkbox) {

        blocks = blocks.map { block ->
            if (block.id == view.id) {
                block.copy(
                    content = block.content<Content.Text>().copy(
                        isChecked = view.isChecked
                    )
                )
            } else {
                block
            }
        }

        val store = orchestrator.stores.views

        viewModelScope.launch {
            store.update(
                views.map { v ->
                    if (v.id == view.id)
                        view.copy()
                    else
                        v
                }
            )
        }

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateCheckbox(
                    context = context,
                    target = view.id,
                    isChecked = view.isChecked
                )
            )
        }
    }

    fun onBlockToolbarStyleClicked() {
        val target = orchestrator.stores.focus.current().id
        val view = views.first { it.id == target }
        when (view) {
            is BlockView.Title -> _toasts.offer(CANNOT_OPEN_STYLE_PANEL_FOR_TITLE_ERROR)
            is BlockView.Code -> _toasts.offer(CANNOT_OPEN_STYLE_PANEL_FOR_CODE_BLOCK_ERROR)
            else -> {
                val textSelection = orchestrator.stores.textSelection.current()
                controlPanelInteractor.onEvent(
                    ControlPanelMachine.Event.OnBlockActionToolbarStyleClicked(
                        target = blocks.first { it.id == orchestrator.stores.focus.current().id },
                        focused = textSelection.isNotEmpty,
                        selection = textSelection.selection
                    )
                )
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = EventsDictionary.BTN_STYLE_MENU
                )
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = POPUP_STYLE
                )
            }
        }
    }

    fun onBlockToolbarBlockActionsClicked() {
        val target = orchestrator.stores.focus.current().id
        val view = views.first { it.id == target }
        if (view is BlockView.Title) {
            _toasts.offer(CANNOT_OPEN_ACTION_MENU_FOR_TITLE_ERROR)
        } else {
            dispatch(Command.Measure(target = target))
            viewModelScope.sendEvent(
                analytics = analytics,
                eventName = EventsDictionary.BTN_BLOCK_ACTIONS
            )
        }
    }

    fun onMeasure(target: Id, dimensions: BlockDimensions) {
        proceedWithClearingFocus()
        onBlockLongPressedClicked(target, dimensions)
    }

    fun onAddBlockToolbarClicked() {
        dispatch(Command.OpenAddBlockPanel)
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_ADD_BLOCK_MENU
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_ADD_BLOCK
        )
    }

    fun onEnterMultiSelectModeClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.MultiSelect.OnEnter)
        mode = EditorMode.MULTI_SELECT
        viewModelScope.launch { orchestrator.stores.focus.update(Editor.Focus.empty()) }
        viewModelScope.launch {
            delay(DELAY_REFRESH_DOCUMENT_TO_ENTER_MULTI_SELECT_MODE)
            refresh()
        }
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_MULTI_SELECT_MENU
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_ENTER_MS
        )
    }

    fun onExitMultiSelectModeClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.MultiSelect.OnExit)
        mode = EditorMode.EDITING
        clearSelections()
        viewModelScope.launch {
            delay(300)
            orchestrator.stores.focus.update(Editor.Focus.empty())
            refresh()
        }
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_MS_DONE
        )
    }

    fun onEnterScrollAndMoveClicked() {
        mode = EditorMode.SCROLL_AND_MOVE
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.SAM.OnEnter)
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_SCROLL_MOVE
        )
    }

    fun onExitScrollAndMoveClicked() {
        mode = EditorMode.MULTI_SELECT
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.SAM.OnExit)
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_SCROLL_MOVE_CANCEL
        )
    }

    fun onApplyScrollAndMoveClicked() {
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_SCROLL_MOVE_MOVE
        )
    }

    private fun onEnterActionMode() {
        mode = EditorMode.ACTION_MODE
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.ReadMode.OnEnter)
        viewModelScope.launch {
            refresh()
        }
    }

    private fun onExitActionMode() {
        mode = EditorMode.EDITING
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.ReadMode.OnExit)
        viewModelScope.launch { refresh() }
    }

    fun onMultiSelectModeDeleteClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.MultiSelect.OnDelete)

        val exclude = mutableSetOf<String>()

        val selected = currentSelection().toList()

        blocks.filter { selected.contains(it.id) }.forEach { block ->
            block.children.forEach { if (selected.contains(it)) exclude.add(it) }
        }

        clearSelections()

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.CRUD.Unlink(
                    context = context,
                    targets = selected - exclude,
                    next = null,
                    previous = null,
                    effects = listOf(SideEffect.ClearMultiSelectSelection)
                )
            )
        }

        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_MS_DELETE
        )
    }

    fun onMultiSelectCopyClicked() {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Clipboard.Copy(
                    context = context,
                    blocks = blocks.filter { block ->
                        currentSelection().contains(block.id)
                    },
                    range = null
                )
            )
        }

        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_MS_COPY
        )
    }

    fun onMultiSelectModeSelectAllClicked() {
        (stateData.value as ViewState.Success).let { state ->
            if (currentSelection().isEmpty()) {
                onSelectAllClicked(state)
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = EventsDictionary.BTN_MS_SELECT_ALL
                )
            } else {
                onUnselectAllClicked(state)
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = EventsDictionary.BTN_MS_UNSELECT_ALL
                )
            }
        }
    }

    private fun onSelectAllClicked(state: ViewState.Success) =
        state.blocks.map { block ->
            if (block.id != blocks.titleId()) select(block.id)
            block.updateSelection(newSelection = true)
        }.let {
            onMultiSelectModeBlockClicked()
            stateData.postValue(ViewState.Success(it))
        }

    private fun onUnselectAllClicked(state: ViewState.Success) =
        state.blocks.map { block ->
            unselect(block.id)
            block.updateSelection(newSelection = false)
        }.let {
            onMultiSelectModeBlockClicked()
            stateData.postValue(ViewState.Success(it))
        }

    fun onMultiSelectTurnIntoButtonClicked() {

        val excludedCategories = mutableListOf<String>()
        val excludedTypes = mutableListOf<String>()

        val targets = currentSelection()

        val blocks = blocks.filter { targets.contains(it.id) }

        val hasTextBlocks = blocks.any { it.content is Content.Text }
        val hasDividerBlocks = blocks.any { it.content is Content.Divider }

        when {
            hasTextBlocks -> {
                excludedTypes.apply {
                    add(UiBlock.FILE.name)
                    add(UiBlock.IMAGE.name)
                    add(UiBlock.VIDEO.name)
                    add(UiBlock.BOOKMARK.name)
                    add(UiBlock.LINE_DIVIDER.name)
                    add(UiBlock.THREE_DOTS.name)
                    add(UiBlock.LINK_TO_OBJECT.name)
                }
                dispatch(Command.OpenMultiSelectTurnIntoPanel(excludedCategories, excludedTypes))
            }
            hasDividerBlocks -> {
                excludedCategories.apply {
                    add(UiBlock.Category.TEXT.name)
                    add(UiBlock.Category.LIST.name)
                    add(UiBlock.Category.OBJECT.name)
                }
                excludedTypes.add(UiBlock.CODE.name)
                dispatch(Command.OpenMultiSelectTurnIntoPanel(excludedCategories, excludedTypes))
            }
            else -> {
                _toasts.offer("Cannot turn selected blocks into other blocks")
            }
        }

        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_MS_TURN_INTO
        )
    }

    fun onOpenPageNavigationButtonClicked() {
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.SCREEN_NAVIGATION
        )
        navigation.postValue(
            EventWrapper(
                AppNavigation.Command.OpenPageNavigationScreen(
                    target = context
                )
            )
        )
    }

    // ----------------- Turn Into -----------------------------------------

    fun onTurnIntoMultiSelectBlockClicked(uiBlock: UiBlock) {
        proceedUpdateBlockStyle(
            targets = currentSelection().toList(),
            uiBlock = uiBlock,
            action = {
                clearSelections()
                controlPanelInteractor.onEvent(ControlPanelMachine.Event.MultiSelect.OnTurnInto)
            },
            errorAction = { _toasts.offer("Cannot convert selected blocks to $uiBlock") }
        )
    }

    fun onTurnIntoBlockClicked(target: String, uiBlock: UiBlock) {
        proceedUpdateBlockStyle(
            targets = listOf(target),
            uiBlock = uiBlock,
            errorAction = { _toasts.offer("Cannot convert block to $uiBlock") }
        )
        dispatch(Command.PopBackStack)
    }

    private fun proceedUpdateBlockStyle(
        targets: List<String>,
        uiBlock: UiBlock,
        action: (() -> Unit)? = null,
        errorAction: (() -> Unit)? = null
    ) {
        when (uiBlock) {
            UiBlock.TEXT, UiBlock.HEADER_ONE,
            UiBlock.HEADER_TWO, UiBlock.HEADER_THREE,
            UiBlock.HIGHLIGHTED, UiBlock.CHECKBOX,
            UiBlock.BULLETED, UiBlock.NUMBERED,
            UiBlock.TOGGLE, UiBlock.CODE -> {
                action?.invoke()
                proceedUpdateTextStyle(targets, uiBlock.style())
            }
            UiBlock.PAGE -> {
                action?.invoke()
                proceedWithTurningIntoDocument(targets)
            }
            UiBlock.LINE_DIVIDER -> {
                action?.invoke()
                proceedUpdateDividerStyle(targets, Content.Divider.Style.LINE)
            }
            UiBlock.THREE_DOTS -> {
                action?.invoke()
                proceedUpdateDividerStyle(targets, Content.Divider.Style.DOTS)
            }
            UiBlock.LINK_TO_OBJECT -> errorAction?.invoke()
            UiBlock.FILE -> errorAction?.invoke()
            UiBlock.IMAGE -> errorAction?.invoke()
            UiBlock.VIDEO -> errorAction?.invoke()
            UiBlock.BOOKMARK -> errorAction?.invoke()
        }
    }

    private fun proceedUpdateTextStyle(
        targets: List<String>,
        style: Content.Text.Style
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Text.UpdateStyle(
                    context = context,
                    targets = targets,
                    style = style
                )
            )
        }
    }

    private fun proceedUpdateDividerStyle(targets: List<String>, style: Content.Divider.Style) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Divider.UpdateStyle(
                    context = context,
                    targets = targets,
                    style = style
                )
            )
        }
    }

    private fun proceedWithTurningIntoDocument(targets: List<String>) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Document.TurnIntoDocument(
                    context = context,
                    targets = targets
                )
            )
        }
    }

    fun onAddDividerBlockClicked(style: Content.Divider.Style) {

        val focused = blocks.first { it.id == orchestrator.stores.focus.current().id }
        val content = focused.content
        val prototype = when (style) {
            Content.Divider.Style.LINE -> Prototype.DividerLine
            Content.Divider.Style.DOTS -> Prototype.DividerDots
        }

        if (content is Content.Text && content.text.isEmpty()) {
            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.CRUD.Replace(
                        context = context,
                        target = focused.id,
                        prototype = prototype
                    )
                )
            }
        } else {

            val position: Position

            var target: Id = focused.id

            if (focused.id == context) {
                if (focused.children.isEmpty()) {
                    position = Position.INNER
                } else {
                    position = Position.TOP
                    target = focused.children.first()
                }
            } else {
                position = Position.BOTTOM
            }

            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.CRUD.Create(
                        context = context,
                        target = target,
                        position = position,
                        prototype = prototype
                    )
                )
            }
        }

        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnAddBlockToolbarOptionSelected)
    }

    fun onOutsideClicked() {

        val root = blocks.find { it.id == context } ?: return

        if (root.children.isEmpty()) {
            addNewBlockAtTheEnd()
        } else {
            val last = blocks.first { it.id == root.children.last() }
            when (val content = last.content) {
                is Content.Text -> {
                    when {
                        content.style == Content.Text.Style.TITLE -> addNewBlockAtTheEnd()
                        content.text.isNotEmpty() -> addNewBlockAtTheEnd()
                        content.text.isEmpty() -> {
                            val stores = orchestrator.stores
                            if (stores.focus.current().isEmpty) {
                                val focus = Editor.Focus(id = last.id, cursor = null)
                                viewModelScope.launch { orchestrator.stores.focus.update(focus) }
                                viewModelScope.launch { refresh() }
                            } else {
                                Timber.d("Outside click is ignored because focus is not empty")
                            }
                        }
                        else -> Timber.d("Outside-click has been ignored.")
                    }
                }
                is Content.Link -> {
                    addNewBlockAtTheEnd()
                }
                is Content.Bookmark -> {
                    addNewBlockAtTheEnd()
                }
                is Content.File -> {
                    addNewBlockAtTheEnd()
                }
                is Content.Divider -> {
                    addNewBlockAtTheEnd()
                }
                is Content.Layout -> {
                    addNewBlockAtTheEnd()
                }
                else -> {
                    Timber.d("Outside-click has been ignored.")
                }
            }
        }
    }

    fun onHideKeyboardClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnClearFocusClicked)
        viewModelScope.launch { orchestrator.stores.focus.update(Editor.Focus.empty()) }
        views.onEach { if (it is Focusable) it.isFocused = false }
        viewModelScope.launch { renderCommand.send(Unit) }
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.BTN_HIDE_KEYBOARD
        )
    }

    private fun proceedWithClearingFocus() {
        viewModelScope.launch {
            orchestrator.stores.focus.update(Editor.Focus.empty())
            refresh()
        }
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnClearFocusClicked)
    }

    private suspend fun refresh() {
        if (BuildConfig.DEBUG) {
            Timber.d("----------Refreshing Blocks---------------------\n$blocks")
            Timber.d("----------Finished Refreshing Blocks------------")
        }
        renderizePipeline.send(blocks)
    }

    private fun onPageClicked(target: String) =
        proceedWithOpeningPage(
            target = blocks.first { it.id == target }.content<Content.Link>().target
        )

    private fun onMentionClicked(target: String) {
        proceedWithClearingFocus()
        proceedWithOpeningPage(target = target)
    }

    fun onAddNewPageClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnAddBlockToolbarOptionSelected)

        val position: Position

        val focused = blocks.first { it.id == orchestrator.stores.focus.current().id }

        var target = focused.id

        if (focused.id == context) {
            if (focused.children.isEmpty())
                position = Position.INNER
            else {
                position = Position.TOP
                target = focused.children.first()
            }
        } else {
            position = Position.BOTTOM
        }

        val params = CreateDocument.Params(
            context = context,
            position = position,
            target = target
        )

        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            createDocument(
                params = params
            ).proceed(
                failure = { Timber.e(it, "Error while creating new page with params: $params") },
                success = { result ->
                    val middleTime = System.currentTimeMillis()
                    analytics.registerEvent(
                        EventAnalytics.Anytype(
                            name = PAGE_CREATE,
                            props = Props(mapOf(PROP_STYLE to Content.Page.Style.EMPTY)),
                            duration = EventAnalytics.Duration(
                                start = startTime,
                                middleware = middleTime,
                                render = middleTime
                            )
                        )
                    )
                    orchestrator.proxies.payloads.send(result.payload)
                    proceedWithOpeningPage(result.target)
                }
            )
        }
    }

    fun onArchiveThisPageClicked() {
        dispatch(command = Command.CloseKeyboard)
        viewModelScope.launch {
            archiveDocument(
                ArchiveDocument.Params(
                    context = context,
                    targets = listOf<String>(context),
                    isArchived = true
                )
            ).proceed(
                failure = { Timber.e(it, "Error while archiving page") },
                success = { proceedWithExiting() }
            )
        }
    }

    fun onAddBookmarkBlockClicked() {

        val focused = blocks.first { it.id == orchestrator.stores.focus.current().id }

        val content = focused.content

        if (content is Content.Text && content.text.isEmpty()) {
            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.CRUD.Replace(
                        context = context,
                        target = focused.id,
                        prototype = Prototype.Bookmark
                    )
                )
            }
        } else {

            val position: Position

            var target: Id = focused.id

            if (focused.id == context) {
                if (focused.children.isEmpty()) {
                    position = Position.INNER
                } else {
                    position = Position.TOP
                    target = focused.children.first()
                }
            } else {
                position = Position.BOTTOM
            }

            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.CRUD.Create(
                        context = context,
                        position = position,
                        target = target,
                        prototype = Prototype.Bookmark
                    )
                )
            }
        }
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnAddBlockToolbarOptionSelected)
    }

    fun onAddBookmarkUrl(target: String, url: String) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Bookmark.SetupBookmark(
                    context = context,
                    target = target,
                    url = url
                )
            )
        }
    }

    private fun onBookmarkPlaceholderClicked(target: String) {
        dispatch(
            command = Command.OpenBookmarkSetter(
                context = context,
                target = target
            )
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_BOOKMARK
        )
    }

    private fun onBookmarkClicked(view: BlockView.Media.Bookmark) =
        dispatch(command = Command.Browse(view.url))

    private fun onFailedBookmarkClicked(view: BlockView.Error.Bookmark) =
        dispatch(command = Command.Browse(view.url))

    fun onTitleTextInputClicked() {
        controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnTextInputClicked)
    }

    fun onTextInputClicked(target: Id) {
        Timber.d("onTextInputClicked: $target")
        if (mode == EditorMode.MULTI_SELECT) {
            onBlockMultiSelectClicked(target)
        } else {
            controlPanelInteractor.onEvent(ControlPanelMachine.Event.OnTextInputClicked)
        }
    }

    private fun onBlockMultiSelectClicked(target: Id) {
        (stateData.value as? ViewState.Success)?.let { state ->

            var allow = true

            val parent = blocks.find { it.children.contains(target) }

            if (parent != null && parent.id != context) {
                if (isSelected(parent.id)) allow = false
            }

            if (!allow) return

            toggleSelection(target)

            val descendants = blocks.asMap().descendants(parent = target)

            if (isSelected(target)) {
                descendants.forEach { child -> select(child) }
            } else {
                descendants.forEach { child -> unselect(child) }
            }

            onMultiSelectModeBlockClicked()

            val update = state.blocks.map { view ->
                if (view.id == target || descendants.contains(view.id))
                    view.updateSelection(newSelection = isSelected(target))
                else
                    view
            }

            stateData.postValue(ViewState.Success(update))
        }
    }

    fun onPaste(
        range: IntRange
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Clipboard.Paste(
                    context = context,
                    focus = orchestrator.stores.focus.current().id,
                    range = range,
                    selected = emptyList()
                )
            )
        }
    }

    fun onApplyScrollAndMove(
        target: Id,
        ratio: Float
    ) {

        val ordering = views.mapIndexed { index, view -> view.id to index }.toMap()

        val exclude = mutableSetOf<String>()

        var moveTarget = target

        var position = when (ratio) {
            in START_RANGE -> Position.TOP
            in END_RANGE -> Position.BOTTOM
            in INNER_RANGE -> Position.INNER
            else -> {
                if (ratio > 1) Position.BOTTOM
                else throw IllegalStateException("Unexpected ratio: $ratio")
            }
        }

        val targetBlock = blocks.first { it.id == target }

        val parent = blocks.find { it.children.contains(target) }?.id

        val selected = currentSelection().toList()

        if (selected.contains(target)) {
            if (position == Position.INNER) {
                _toasts.offer(CANNOT_BE_DROPPED_INSIDE_ITSELF_ERROR)
            } else if (selected.size == 1) {
                _toasts.offer(CANNOT_MOVE_BLOCK_ON_SAME_POSITION)
            }
            return
        }

        if (selected.contains(parent)) {
            _toasts.offer(CANNOT_MOVE_PARENT_INTO_CHILD)
            return
        }

        if (position == Position.INNER) {

            if (!targetBlock.supportNesting()) {
                _toasts.offer(CANNOT_BE_PARENT_ERROR)
                return
            }

            val targetContext = if (targetBlock.content is Content.Link) {
                targetBlock.content<Content.Link>().target
            } else {
                context
            }

            blocks.filter { selected.contains(it.id) }.forEach { block ->
                block.children.forEach { if (selected.contains(it)) exclude.add(it) }
            }

            clearSelections()

            controlPanelInteractor.onEvent(ControlPanelMachine.Event.SAM.OnApply)

            mode = EditorMode.MULTI_SELECT

            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.Document.Move(
                        context = context,
                        target = moveTarget,
                        targetContext = targetContext,
                        blocks = (selected - exclude).sortedBy { id -> ordering[id] },
                        position = position
                    )
                )
            }
        } else {

            val targetContext = context

            if (target == context) {
                position = Position.TOP
                moveTarget = targetBlock.children.first()
            }

            blocks.filter { selected.contains(it.id) }.forEach { block ->
                block.children.forEach { if (selected.contains(it)) exclude.add(it) }
            }

            clearSelections()

            controlPanelInteractor.onEvent(ControlPanelMachine.Event.SAM.OnApply)

            mode = EditorMode.MULTI_SELECT

            viewModelScope.launch {
                orchestrator.proxies.intents.send(
                    Intent.Document.Move(
                        context = context,
                        target = moveTarget,
                        targetContext = targetContext,
                        blocks = (selected - exclude).sortedBy { id -> ordering[id] },
                        position = position
                    )
                )
            }
        }
    }

    fun onCopy(
        range: IntRange
    ) {
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Clipboard.Copy(
                    context = context,
                    range = range,
                    blocks = listOf(blocks.first { it.id == focus.value })
                )
            )
        }
    }

    fun onClickListener(clicked: ListenerType) = when (clicked) {
        is ListenerType.Bookmark.View -> {
            when (mode) {
                EditorMode.EDITING -> onBookmarkClicked(clicked.item)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.item.id)
                else -> Unit
            }
        }
        is ListenerType.Bookmark.Placeholder -> {
            when (mode) {
                EditorMode.EDITING -> onBookmarkPlaceholderClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Bookmark.Error -> {
            when (mode) {
                EditorMode.EDITING -> onFailedBookmarkClicked(clicked.item)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.item.id)
                else -> Unit
            }
        }
        is ListenerType.File.View -> {
            when (mode) {
                EditorMode.EDITING -> onFileClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.File.Placeholder -> {
            when (mode) {
                EditorMode.EDITING -> onAddLocalFileClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.File.Error -> {
            when (mode) {
                EditorMode.EDITING -> onAddLocalFileClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.File.Upload -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Picture.View -> {
            when (mode) {
                EditorMode.EDITING -> {
                    val target = blocks.find { it.id == clicked.target }
                    if (target != null) {
                        val content = target.content
                        check(content is Content.File)
                        dispatch(
                            Command.OpenFullScreenImage(
                                target = clicked.target,
                                url = urlBuilder.original(content.hash)
                            )
                        )
                    } else {
                        Timber.e("Could not find target for picture")
                    }
                }
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Picture.Placeholder -> {
            when (mode) {
                EditorMode.EDITING -> onAddLocalPictureClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Picture.Error -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Picture.Upload -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Video.View -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Video.Placeholder -> {
            when (mode) {
                EditorMode.EDITING -> onAddLocalVideoClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Video.Error -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Video.Upload -> {
            when (mode) {
                EditorMode.EDITING -> Unit
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.LongClick -> {
            when (mode) {
                EditorMode.EDITING -> onBlockLongPressedClicked(clicked.target, clicked.dimensions)
                EditorMode.MULTI_SELECT -> Unit
                else -> Unit
            }
        }
        is ListenerType.Page -> {
            when (mode) {
                EditorMode.EDITING -> onPageClicked(clicked.target)
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.Mention -> {
            when (mode) {
                EditorMode.EDITING -> onMentionClicked(clicked.target)
                else -> Unit
            }
        }
        is ListenerType.EditableBlock -> {
            //Todo block view refactoring
        }
        ListenerType.TitleBlock -> {
            //Todo block view refactoring
        }
        is ListenerType.DividerClick -> {
            when (mode) {
                EditorMode.MULTI_SELECT -> onBlockMultiSelectClicked(clicked.target)
                else -> Unit
            }
        }
    }

    fun onPlusButtonPressed() {
        val startTime = System.currentTimeMillis()
        createPage(
            scope = viewModelScope,
            params = CreatePage.Params.insideDashboard()
        ) { result ->
            result.either(
                fnL = { Timber.e(it, "Error while creating a new page on home dashboard") },
                fnR = { id ->
                    val middle = System.currentTimeMillis()
                    viewModelScope.sendEvent(
                        analytics = analytics,
                        startTime = startTime,
                        middleTime = middle,
                        renderTime = middle,
                        eventName = PAGE_CREATE,
                        props = Props(mapOf(PROP_STYLE to Content.Page.Style.EMPTY))
                    )
                    proceedWithOpeningPage(id)
                }
            )
        }
    }

    fun onProceedWithFilePath(filePath: String?) {
        if (filePath == null) {
            Timber.d("Error while getting filePath")
            return
        }
        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Media.Upload(
                    context = context,
                    target = mediaBlockId,
                    filePath = filePath,
                    url = ""
                )
            )
        }
    }

    fun onPageIconClicked() {
        val details = orchestrator.stores.details.current()
        dispatch(
            Command.OpenDocumentIconActionMenu(
                target = context,
                emoji = details.details[context]?.iconEmoji?.let { name ->
                    if (name.isNotEmpty())
                        name
                    else
                        null
                },
                image = details.details[context]?.iconImage?.let { name ->
                    if (name.isNotEmpty())
                        urlBuilder.image(name)
                    else
                        null
                }
            )
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_DOCUMENT_ICON_MENU
        )
    }

    fun onProfileIconClicked() {
        val details = orchestrator.stores.details.current()
        dispatch(
            Command.OpenProfileIconActionMenu(
                target = context,
                image = details.details[context]?.iconImage?.let { name ->
                    if (name.isNotEmpty() && name.isNotBlank())
                        urlBuilder.image(name)
                    else
                        null
                },
                name = details.details[context]?.name
            )
        )
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = POPUP_PROFILE_ICON_MENU
        )
    }

    private fun onFileClicked(id: String) {
        dispatch(Command.RequestDownloadPermission(id))
    }

    fun startDownloadingFile(id: String) {

        _toasts.offer("Downloading file in background...")

        val block = blocks.first { it.id == id }
        val file = block.content<Content.File>()

        viewModelScope.launch {
            orchestrator.proxies.intents.send(
                Intent.Media.DownloadFile(
                    url = when (file.type) {
                        Content.File.Type.IMAGE -> urlBuilder.image(file.hash)
                        else -> urlBuilder.file(file.hash)
                    },
                    name = file.name.orEmpty()
                )
            )
        }
    }

    fun onPageSearchClicked() {
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.SCREEN_SEARCH
        )
        navigation.postValue(EventWrapper(AppNavigation.Command.OpenPageSearch))
    }

    fun onMentionEvent(mentionEvent: MentionEvent) {
        when (mentionEvent) {
            is MentionEvent.MentionSuggestText -> {
                controlPanelInteractor.onEvent(
                    ControlPanelMachine.Event.Mentions.OnQuery(
                        text = mentionEvent.text.toString()
                    )
                )
            }
            is MentionEvent.MentionSuggestStart -> {
                mentionFrom = mentionEvent.mentionStart
                controlPanelInteractor.onEvent(
                    ControlPanelMachine.Event.Mentions.OnStart(
                        cursorCoordinate = mentionEvent.cursorCoordinate,
                        mentionFrom = mentionEvent.mentionStart
                    )
                )
                viewModelScope.launch {
                    getListPages.invoke(Unit).proceed(
                        failure = { it.timber() },
                        success = { response ->
                            controlPanelInteractor.onEvent(
                                ControlPanelMachine.Event.Mentions.OnResult(
                                    mentions = response.listPages.map { it.toMentionView(urlBuilder) }
                                )
                            )
                        }
                    )
                }
                viewModelScope.sendEvent(
                    analytics = analytics,
                    eventName = POPUP_MENTION_MENU
                )
            }
            MentionEvent.MentionSuggestStop -> {
                mentionFrom = -1
                controlPanelInteractor.onEvent(
                    ControlPanelMachine.Event.Mentions.OnStop
                )
            }
        }
    }

    fun onAddMentionNewPageClicked(name: String) {

        val params = CreateNewDocument.Params(
            name = name.removePrefix(MentionAdapter.MENTION_PREFIX)
        )

        val startTime = System.currentTimeMillis()

        viewModelScope.launch {
            createNewDocument(
                params = params
            ).proceed(
                failure = {
                    Timber.e(it, "Error while creating new page with params: $params")
                },
                success = { result ->
                    val middleTime = System.currentTimeMillis()
                    onCreateMentionInText(
                        mention = Mention(
                            id = result.id,
                            title = result.name.getMentionName(MENTION_TITLE_EMPTY),
                            emoji = result.emoji,
                            image = null
                        ),
                        mentionTrigger = name
                    )
                    analytics.registerEvent(
                        EventAnalytics.Anytype(
                            name = PAGE_MENTION_CREATE,
                            props = Props(mapOf(PROP_STYLE to Content.Page.Style.EMPTY)),
                            duration = EventAnalytics.Duration(
                                start = startTime,
                                middleware = middleTime,
                                render = middleTime
                            )
                        )
                    )
                }
            )
        }
    }

    fun onMentionSuggestClick(mention: Mention, mentionTrigger: String) {
        onCreateMentionInText(mention, mentionTrigger)
    }

    fun onCreateMentionInText(mention: Mention, mentionTrigger: String) {
        Timber.d("onAddMentionClicked, suggest:$mention, from:$mentionFrom")

        controlPanelInteractor.onEvent(ControlPanelMachine.Event.Mentions.OnMentionClicked)

        val target = blocks.first { it.id == focus.value }

        val new = target.addMention(
            mentionText = mention.title,
            mentionId = mention.id,
            from = mentionFrom,
            mentionTrigger = mentionTrigger
        )

        blocks = blocks.map { block ->
            if (block.id != target.id)
                block
            else
                new
        }

        viewModelScope.launch {
            val position = mentionFrom + mention.title.length + 1
            orchestrator.stores.focus.update(
                t = Editor.Focus(
                    id = new.id,
                    cursor = Editor.Cursor.Range(IntRange(position, position))
                )
            )
            refresh()
        }

        viewModelScope.launch {
            proceedWithUpdatingText(
                intent = Intent.Text.UpdateText(
                    context = context,
                    target = new.id,
                    text = new.content<Content.Text>().text,
                    marks = new.content<Content.Text>().marks
                )
            )
        }
    }

    private fun onMultiSelectModeBlockClicked() {
        controlPanelInteractor.onEvent(
            ControlPanelMachine.Event.MultiSelect.OnBlockClick(
                count = currentSelection().size
            )
        )
    }

    private fun addNewBlockAtTheEnd() {
        proceedWithCreatingNewTextBlock(
            id = "",
            position = Position.INNER,
            style = Content.Text.Style.P
        )
    }

    private fun proceedWithOpeningPage(target: Id) {
        viewModelScope.sendEvent(
            analytics = analytics,
            eventName = EventsDictionary.SCREEN_DOCUMENT
        )
        navigate(EventWrapper(AppNavigation.Command.OpenPage(target)))
    }

    /**
     * Return true, when mention menu is closed, and we need absorb back button click
     */
    fun onBackPressedCallback(): Boolean {
        return controlPanelViewState.value?.let { state ->
            val isVisible = state.mentionToolbar.isVisible
            if (isVisible) {
                onMentionEvent(MentionEvent.MentionSuggestStop)
                true
            } else {
                false
            }
        } ?: run { false }
    }

    companion object {
        const val NO_SEARCH_RESULT_POSITION = -1
        const val EMPTY_TEXT = ""
        const val EMPTY_CONTEXT = ""
        const val EMPTY_FOCUS_ID = ""
        const val TEXT_CHANGES_DEBOUNCE_DURATION = 500L
        const val DELAY_REFRESH_DOCUMENT_TO_ENTER_MULTI_SELECT_MODE = 150L
        const val INITIAL_INDENT = 0
        const val CANNOT_MOVE_BLOCK_ON_SAME_POSITION = "Selected block is already on the position"
        const val CANNOT_BE_DROPPED_INSIDE_ITSELF_ERROR = "A block cannot be moved inside itself."
        const val CANNOT_BE_PARENT_ERROR = "This block does not support nesting."
        const val CANNOT_MOVE_PARENT_INTO_CHILD =
            "Cannot move parent into child. Please, check selected blocks."
        const val MENTION_TITLE_EMPTY = "Untitled"

        const val CANNOT_OPEN_ACTION_MENU_FOR_TITLE_ERROR =
            "Opening action menu for title currently not supported"
        const val CANNOT_OPEN_STYLE_PANEL_FOR_TITLE_ERROR =
            "Opening style panel for title currently not supported"
        const val CANNOT_OPEN_STYLE_PANEL_FOR_CODE_BLOCK_ERROR =
            "Opening style panel for code block currently not supported"
    }

    data class MarkupAction(
        val type: Markup.Type,
        val param: String? = null
    )

    override fun onCleared() {
        super.onCleared()

        orchestrator.stores.focus.cancel()
        orchestrator.stores.details.cancel()
        orchestrator.stores.textSelection.cancel()
        orchestrator.proxies.changes.cancel()
        orchestrator.proxies.saves.cancel()

        markupActionPipeline.cancel()
        renderizePipeline.cancel()

        controlPanelInteractor.channel.cancel()
        titleChannel.cancel()

        Timber.d("onCleared")
    }

    fun onStop() {
        Timber.d("onStop")
        jobs.apply {
            forEach { it.cancel() }
            clear()
        }
    }

    enum class Session { IDLE, OPEN, ERROR }
}