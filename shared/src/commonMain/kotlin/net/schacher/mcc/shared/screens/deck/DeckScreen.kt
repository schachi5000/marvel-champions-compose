package net.schacher.mcc.shared.screens.deck

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.schacher.mcc.shared.design.compose.Deck
import net.schacher.mcc.shared.model.Deck

@Composable
fun DeckScreen(
    deckViewModel: DeckViewModel,
    onDeckClick: (Deck) -> Unit,
    onAddDeckClick: () -> Unit
) {
    val state by deckViewModel.state.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn {
            items(state.decks.size) { index ->
                if (index == 0) {
                    Spacer(Modifier.statusBarsPadding().height(16.dp))
                }

                Deck(state.decks[index]) {
                    onDeckClick(state.decks[index])
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}