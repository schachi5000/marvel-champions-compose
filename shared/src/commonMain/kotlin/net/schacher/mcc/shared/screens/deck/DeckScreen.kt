package net.schacher.mcc.shared.screens.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import marvelchampionscompanion.shared.generated.resources.Res
import marvelchampionscompanion.shared.generated.resources.remove_card_from_deck
import net.schacher.mcc.shared.design.compose.BackButton
import net.schacher.mcc.shared.design.compose.BackHandler
import net.schacher.mcc.shared.design.compose.BottomSheetContainer
import net.schacher.mcc.shared.design.compose.BottomSpacer
import net.schacher.mcc.shared.design.compose.Card
import net.schacher.mcc.shared.design.compose.CardBackgroundBox
import net.schacher.mcc.shared.design.compose.CardRow
import net.schacher.mcc.shared.design.compose.CardRowEntry
import net.schacher.mcc.shared.design.compose.ConfirmationDialog
import net.schacher.mcc.shared.design.compose.HeaderSmall
import net.schacher.mcc.shared.design.compose.LabeledCard
import net.schacher.mcc.shared.design.compose.ProgressDialog
import net.schacher.mcc.shared.design.compose.SecondaryButton
import net.schacher.mcc.shared.design.compose.Tag
import net.schacher.mcc.shared.design.theme.BottomSheetColors
import net.schacher.mcc.shared.design.theme.BottomSheetShape
import net.schacher.mcc.shared.design.theme.ContentPadding
import net.schacher.mcc.shared.model.Card
import net.schacher.mcc.shared.model.CardType
import net.schacher.mcc.shared.screens.deck.DeckScreenViewModel.UiState
import net.schacher.mcc.shared.screens.deck.DeckScreenViewModel.UiState.CardOption.REMOVE
import net.schacher.mcc.shared.screens.deck.DeckScreenViewModel.UiState.DeckOption.DELETE
import net.schacher.mcc.shared.screens.deck.DeckScreenViewModel.UiState.Loading.DeletingDeck
import net.schacher.mcc.shared.screens.deck.DeckScreenViewModel.UiState.Loading.RemovingCard
import net.schacher.mcc.shared.utils.defaultSort
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private const val COLUMN_COUNT = 3

@Composable
fun DeckScreen(
    deckId: Int,
    navController: NavController = koinInject(),
    viewModel: DeckScreenViewModel = koinInject { parametersOf(deckId) },
) {
    val state = viewModel.state.collectAsState().value

    if (state == null) {
        navController.popBackStack()
        return
    }

    DeckScreen(
        state = state,
        navController = navController,
        onShowCardOptions = { viewModel.onShowCardOptionClicked(it) },
        onCardOptionsDismiss = { viewModel.onCardOptionDismissed() },
        onOptionClick = { viewModel.onOptionClicked(it) },
        onDeleteDeckClicked = { viewModel.onDeleteDeckClicked() },
    )
}

@Composable
fun DeckScreen(
    state: UiState,
    navController: NavController,
    onShowCardOptions: (Card) -> Unit,
    onCardOptionsDismiss: () -> Unit,
    onOptionClick: (UiState.CardOption) -> Unit,
    onDeleteDeckClicked: () -> Unit,
) {

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )

    var deleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(state.selectedCard) {
        if (state.selectedCard != null) {
            scope.launch { sheetState.show() }
        } else {
            scope.launch { sheetState.hide() }
        }
    }

    BackHandler(sheetState.isVisible) {
        onCardOptionsDismiss()
    }

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .collect {
                if (it == ModalBottomSheetValue.Hidden) {
                    onCardOptionsDismiss()
                }
            }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = BottomSheetShape,
        sheetBackgroundColor = BottomSheetColors.Background,
        scrimColor = BottomSheetColors.Scrim,
        sheetContent = { OptionBottomSheet(state.cardOptions, onOptionClick) }
    ) {
        Content(
            state = state,
            onCloseClick = { navController.popBackStack() },
            onCardClick = { navController.navigate("card/${it.code}") },
            onCardOptionsClick = onShowCardOptions,
            onDeleteDeckClicked = { deleteDialog = true }
        )
    }

    if (deleteDialog) {
        ConfirmationDialog(title = state.deck.name,
            message = "Soll das Deck unwiederruflich gelöscht werden?",
            onDismiss = { deleteDialog = false },
            onConfirm = {
                deleteDialog = false
                onDeleteDeckClicked()
            })
    }

    val loading = state.loading
    if (loading != null) {
        ProgressDialog(
            title = when (loading) {
                DeletingDeck -> "Deleting Deck"
                RemovingCard -> "Removing Card"
            },
            dismissible = false
        )
    }
}

@Composable
private fun Content(
    state: UiState,
    onCloseClick: () -> Unit,
    onCardClick: (Card) -> Unit,
    onCardOptionsClick: (Card) -> Unit,
    onDeleteDeckClicked: () -> Unit,
) {
    CardBackgroundBox(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        cardCode = state.deck.hero.code,
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(Modifier.statusBarsPadding().height(ContentPadding))
            }
            item {
                Row(
                    modifier = Modifier.padding(
                        vertical = 16.dp,
                        horizontal = ContentPadding
                    )
                ) {
                    Card(state.deck.hero) {
                        onCardClick(state.deck.hero)
                    }

                    state.deck.hero.linkedCard?.let {
                        Card(it) {
                            onCardClick(it)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.padding(ContentPadding)) {
                    state.deck.version?.let {
                        Tag(text = "v$it")
                    }
                    state.deck.problem?.let {
                        Tag(
                            modifier = Modifier.padding(start = 4.dp),
                            text = it,
                            color = MaterialTheme.colors.error
                        )
                    }
                }
            }

            val heroCards = CardRowEntry("Hero cards", state.deck.cards
                .filter { it.type != CardType.HERO && it.setCode == state.deck.hero.setCode }
                .distinctBy { it.name }
                .sortedBy { it.cost ?: 0 })

            item {
                CardRow(
                    modifier = Modifier.padding(
                        horizontal = ContentPadding,
                        vertical = 16.dp,
                    ),
                    cardRowEntry = heroCards
                ) {
                    onCardClick(it)
                }
            }

            val otherCards = state.deck.cards
                .filter { it.setCode != state.deck.hero.setCode }
                .distinctBy { it.name }
                .defaultSort()

            if (otherCards.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(ContentPadding),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeaderSmall(
                            title = "Other Cards",
                            subTitle = otherCards.size.toString()
                        )
                    }
                }

                val rows = otherCards.chunked(COLUMN_COUNT)
                items(rows.size) { index ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = ContentPadding,
                                end = ContentPadding,
                                top = 8.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rows[index].forEach { card ->
                            Box(Modifier.weight(1f)) {
                                val cardCount = state.deck.cards.count { it.code == card.code }
                                LabeledCard(
                                    modifier = Modifier.wrapContentHeight(),
                                    label = listOfNotNull(card.name,
                                        cardCount.takeIf { it > 1 }?.let { "($it)" })
                                        .joinToString(" "),
                                    card = card
                                ) {
                                    onCardClick(card)
                                }

                                if (state.cardOptions.isNotEmpty()) {
                                    Icon(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colors.surface.copy(alpha = 0.8f),
                                                CircleShape
                                            )
                                            .padding(4.dp)
                                            .align(Alignment.TopEnd)
                                            .clickable { onCardOptionsClick(card) },
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Card Options",
                                        tint = MaterialTheme.colors.onSurface
                                    )
                                }
                            }
                        }

                        if (rows[index].size < COLUMN_COUNT) {
                            repeat(COLUMN_COUNT - rows[index].size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            state.deckOptions.find { it == DELETE }?.let {
                item {
                    SecondaryButton(
                        modifier = Modifier
                            .padding(horizontal = ContentPadding)
                            .fillMaxWidth(),
                        onClick = onDeleteDeckClicked,
                        label = "Deck löschen"
                    )
                }
            }

            item {
                BottomSpacer()
            }
        }

        BackButton(onCloseClick)
    }
}

@Composable
private fun OptionBottomSheet(
    cardOptions: Set<UiState.CardOption>,
    onOptionClick: (UiState.CardOption) -> Unit
) {
    BottomSheetContainer {
        Column {
            cardOptions.forEach {
                Row(modifier = Modifier.fillMaxWidth()
                    .height(64.dp)
                    .clickable { onOptionClick(it) }
                    .padding(ContentPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colors.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.padding(start = ContentPadding)
                    )
                }
            }
        }
    }
}

private val UiState.CardOption.label
    @Composable
    get() = when (this) {
        REMOVE -> stringResource(Res.string.remove_card_from_deck)
    }

private data class ConfirmationState(val title: String, val message: String)