package net.schacher.mcc.shared.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.schacher.mcc.shared.design.compose.BackHandler
import net.schacher.mcc.shared.design.compose.CardInfo
import net.schacher.mcc.shared.design.compose.FreeBottomSheetContainer
import net.schacher.mcc.shared.design.compose.PagerHeader
import net.schacher.mcc.shared.design.compose.blurByBottomSheet
import net.schacher.mcc.shared.design.theme.ButtonSize
import net.schacher.mcc.shared.design.theme.ContentPadding
import net.schacher.mcc.shared.design.theme.DefaultShape
import net.schacher.mcc.shared.model.Card
import net.schacher.mcc.shared.screens.deck.DeckScreen
import net.schacher.mcc.shared.screens.main.MainViewModel.Event.CardsDatabaseSyncFailed
import net.schacher.mcc.shared.screens.main.MainViewModel.Event.DatabaseSynced
import net.schacher.mcc.shared.screens.main.MainViewModel.Event.DeckCreated
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.FullScreen.CreateDeck
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.FullScreen.DeckScreen
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.FullScreen.PackSelectionScreen
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.MainScreen
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.MainScreen.MyDecks
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.MainScreen.Search
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.MainScreen.Settings
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.MainScreen.Spotlight
import net.schacher.mcc.shared.screens.main.MainViewModel.UiState.SubScreen.CardMenu
import net.schacher.mcc.shared.screens.mydecks.MyDecksScreen
import net.schacher.mcc.shared.screens.mydecks.animateHorizontalAlignmentAsState
import net.schacher.mcc.shared.screens.newdeck.NewDeckScreen
import net.schacher.mcc.shared.screens.packselection.PackSelectionScreen
import net.schacher.mcc.shared.screens.settings.SettingsScreen
import net.schacher.mcc.shared.screens.spotlight.SpotlightScreen
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalResourceApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MainScreen(viewModel: MainViewModel = koinInject()) {
    val state = viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(
        enabled = (state.value.subScreen != null ||
                state.value.fullScreen != null)
    ) {
        viewModel.onBackPressed()
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        sheetShape = RoundedCornerShape(topEnd = 16.dp, topStart = 16.dp),
        sheetBackgroundColor = MaterialTheme.colors.surface,
        sheetContent = {
            state.value.subScreen?.let {
                when (it) {
                    is CardMenu -> CardMenuBottomSheet(viewModel, it.card)
                    else -> {}
                }
            }
        }) {

        Scaffold(
            modifier = Modifier.fillMaxSize().blurByBottomSheet(sheetState),
            backgroundColor = MaterialTheme.colors.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) {
            Box(
                modifier = Modifier.padding(it)
            ) {
                val pageLabels = listOf("Decks", "Spotlight", "Settings")
                val pagerState = rememberPagerState(pageCount = { pageLabels.size })

                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> MyDecksScreen(
                            onDeckClick = { viewModel.onDeckClicked(it) },
                            onAddDeckClick = { viewModel.onNewDeckClicked() }
                        )

                        1 -> SpotlightScreen {
                            viewModel.onDeckClicked(it)
                        }

                        2 -> SettingsScreen(
                            onPackSelectionClick = {
                                viewModel.onPackSelectionClicked()
                            },
                            onLogoutClicked = {
                                viewModel.onLogoutClicked()
                            })
                    }
                }

                PagerHeader(
                    modifier = Modifier.fillMaxWidth()
                        .background(shade)
                        .statusBarsPadding()
                        .padding(
                            top = ContentPadding,
                            bottom = ContentPadding + 16.dp
                        ),
                    pageLabels = pageLabels,
                    pagerState = pagerState,
                ) {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                }

                if (pagerState.currentPage < 2) {
                    ScrollDependingTextButton(
                        text = "Suche",
                        icon = { Icon(Icons.Rounded.Search, contentDescription = "Search cards") },
                        expanded = true,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onClick = {

                        }
                    )
                }
            }
        }
    }

    scope.launch {
        if (state.value.subScreen != null) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect {
            when (it) {
                DatabaseSynced -> snackbarHostState.showSnackbar("Database synced!")
                is CardsDatabaseSyncFailed -> snackbarHostState.showSnackbar("Error fully syncing database")
                is DeckCreated -> snackbarHostState.showSnackbar("Deck created! ${it.deckName}")
            }
        }
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.isVisible }.collect { isVisible ->
            if (!isVisible) {
                viewModel.onContextMenuClosed()
            }
        }
    }

    AnimatedContent(
        targetState = state.value.fullScreen,
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                slideOutVertically { height -> height } + fadeOut())
        }
    ) {
        when (it) {
            is DeckScreen -> DeckScreen(
                it.deck,
                onCloseClick = { viewModel.onBackPressed() },
                onDeleteDeckClick = { viewModel.onRemoveDeckClick(it) }
            )

            is PackSelectionScreen -> PackSelectionScreen {
                viewModel.onBackPressed()
            }

            is CreateDeck -> NewDeckScreen(
                onBackPress = { viewModel.onBackPressed() },
                onNewDeckSelected = { card, aspect ->
                    viewModel.onNewDeckHeroSelected(card, aspect)
                }
            )

            else -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
            }
        }
    }
}

private val shade: Brush
    @Composable
    get() = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to MaterialTheme.colors.background.copy(alpha = 1f),
            0.75f to MaterialTheme.colors.background.copy(alpha = 1f),
            1f to MaterialTheme.colors.background.copy(alpha = 0.0f)
        )
    )

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ScrollDependingTextButton(
    text: String,
    icon: @Composable () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var horizontalBias by remember { mutableStateOf(1f) }
    val alignment by animateHorizontalAlignmentAsState(horizontalBias)

    horizontalBias = if (expanded) 0f else 1f

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = ContentPadding)
                .sizeIn(maxHeight = ButtonSize, minWidth = ButtonSize)
                .padding(horizontal = 8.dp),
            contentColor = MaterialTheme.colors.onPrimary,
            backgroundColor = MaterialTheme.colors.primary,
            shape = DefaultShape
        ) {
            Row(
                modifier = Modifier.fillMaxHeight().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()

                AnimatedVisibility(visible = expanded) {
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = text
                    )
                }
            }
        }
    }
}

@Composable
fun CardMenuBottomSheet(mainViewModel: MainViewModel, card: Card) {
    FreeBottomSheetContainer(modifier = Modifier.fillMaxHeight(0.75f)) {
        CardInfo(card = card)
    }
}

private val MainScreen.tabIndex: Int
    get() = when (this) {
        MyDecks -> 0
        Spotlight -> 1
        Search -> 2
        Settings -> 3
    }