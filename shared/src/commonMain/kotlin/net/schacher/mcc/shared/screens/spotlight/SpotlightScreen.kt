package net.schacher.mcc.shared.screens.spotlight

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import marvelchampionscompanion.shared.generated.resources.Res
import marvelchampionscompanion.shared.generated.resources.no_decks_found
import net.schacher.mcc.shared.design.compose.DeckRow
import net.schacher.mcc.shared.design.compose.LoadingDeck
import net.schacher.mcc.shared.design.compose.ShimmerBox
import net.schacher.mcc.shared.design.theme.HorizontalScreenPadding
import net.schacher.mcc.shared.model.Deck
import net.schacher.mcc.shared.screens.spotlight.ListItem.DeckItem
import net.schacher.mcc.shared.screens.spotlight.ListItem.HeaderItem
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SpotlightScreen(
    viewModel: SpotlightViewModel = koinInject(),
    onDeckClick: (Deck) -> Unit
) {
    val state by viewModel.state.collectAsState()

    SpotlightScreen(state, onDeckClick)
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SpotlightScreen(
    state: SpotlightViewModel.UiState,
    onDeckClick: (Deck) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = HorizontalScreenPadding)
    ) {

        AnimatedVisibility(
            visible = !state.loading,
            exit = fadeOut(),
            enter = fadeIn()
        ) {
            val entries = mutableListOf<ListItem>()
            state.decks.forEach { (date, decks) ->
                entries.add(HeaderItem("${date.dayOfMonth}. ${date.month}"))
                decks.forEach { deck ->
                    entries.add(DeckItem(deck))
                }
            }

            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally) {
                items(entries.size) { index ->
                    if (index == 0) {
                        Spacer(Modifier.statusBarsPadding().height(16.dp))
                    }

                    when (val entry = entries[index]) {
                        is HeaderItem -> Header(entry.header)
                        is DeckItem -> DeckRow(entry.deck) {
                            onDeckClick(entry.deck)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (!state.loading && state.decks.isEmpty()) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(Res.string.no_decks_found),
                style = MaterialTheme.typography.h6
            )
        }

        AnimatedVisibility(
            visible = state.loading,
            exit = fadeOut(),
            enter = fadeIn()
        ) {
            LoadingContent()
        }
    }
}

@Composable
private fun Header(label: String) {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .background(MaterialTheme.colors.primary, RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp, horizontal = 16.dp),
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            ShimmerBox(Modifier.size(112.dp, height = 24.dp).clip(RoundedCornerShape(16.dp)))
        }


        for (i in 0..2) {
            LoadingDeck()
            Spacer(Modifier.height(16.dp))
        }
    }
}


private sealed interface ListItem {
    data class DeckItem(val deck: Deck) : ListItem
    data class HeaderItem(val header: String) : ListItem
}