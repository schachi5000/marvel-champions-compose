package net.schacher.mcc.shared.design.compose

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import marvelchampionscompanion.shared.generated.resources.Res
import marvelchampionscompanion.shared.generated.resources.ic_cards
import marvelchampionscompanion.shared.generated.resources.ic_deck
import net.schacher.mcc.shared.design.previewablePainterResource
import net.schacher.mcc.shared.design.theme.DeckShape
import net.schacher.mcc.shared.design.theme.color
import net.schacher.mcc.shared.model.Deck
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

private val deckHeight = 120.dp
private val contentPadding = 8.dp

@Composable
fun DeckRow(deck: Deck, onClick: () -> Unit = {}) {
    Box(modifier = Modifier.wrapContentHeight()) {
        Column(
            modifier = Modifier.fillMaxWidth().height(deckHeight)
                .padding(horizontal = 24.dp)
                .clip(DeckShape)
                .background(deck.aspect.color.copy(alpha = 0.4f), DeckShape)
        ) {}

        Column(
            modifier = Modifier.fillMaxWidth().height(deckHeight)
                .padding(start = 12.dp, top = 8.dp, end = 12.dp)
                .clip(DeckShape)
                .background(deck.aspect.color, DeckShape)
        ) {}

        Box(Modifier.padding(top = 16.dp)) {
            DeckContent(deck, onClick)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun DeckContent(deck: Deck, onClick: () -> Unit = {}) {
    BackgroundImage(
        modifier = Modifier.fillMaxSize().height(deckHeight),
        deck = deck,
    )

    Row(
        modifier = Modifier.fillMaxWidth()
            .height(deckHeight)
            .clip(DeckShape)
            .clickable { onClick() }
            .padding(contentPadding)
    ) {
        Thumbnail(
            card = deck.hero,
            modifier = Modifier.fillMaxHeight(),
        )
        Spacer(Modifier.width(contentPadding))
        Column(
            modifier = Modifier.fillMaxHeight()
                .background(MaterialTheme.colors.surface.copy(alpha = 0.8f), DeckShape)
                .padding(12.dp)
        ) {
            Text(
                text = deck.name,
                maxLines = 1,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {

                InfoRow("${deck.cards.size} Karten", Res.drawable.ic_cards)
                Spacer(Modifier.height(4.dp))
                InfoRow("${deck.requiredDecks.size} Packs benötigt", Res.drawable.ic_deck)
            }
        }
    }
}

@Composable
private fun BackgroundImage(modifier: Modifier = Modifier, deck: Deck) {
    Box(
        modifier = modifier.fillMaxSize().clip(DeckShape)
    ) {
        CardImage(
            modifier = Modifier.fillMaxSize()
                .blur(6.dp)
                .background(MaterialTheme.colors.surface)
                .graphicsLayer {
                    scaleX = 1.6f
                    scaleY = 1.6f
                },
            cardCode = deck.hero.code,
            filterQuality = FilterQuality.Low,
            contentDescription = deck.name,
            contentScale = ContentScale.Crop,
            animationSpec = tween(
                durationMillis = 500
            ),
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        )
        Column(
            modifier = modifier.fillMaxSize()
                .clip(DeckShape)
                .background(deck.aspect.color.copy(alpha = 0.4f), DeckShape)
        ) {}
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun InfoRow(label: String, drawableResource: DrawableResource) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = previewablePainterResource(drawableResource),
            contentDescription = null,
            Modifier.size(16.dp)
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = label,
            maxLines = 1,
        )
    }
}

@Composable
fun LoadingDeck() {
    ShimmerBox(
        modifier = Modifier.fillMaxWidth().height(deckHeight + 24.dp).clip(DeckShape)
    )
}
