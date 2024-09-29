package net.schacher.mcc.shared.screens.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import net.schacher.mcc.shared.design.compose.BackButton
import net.schacher.mcc.shared.design.compose.BottomSpacer
import net.schacher.mcc.shared.design.compose.Card
import net.schacher.mcc.shared.design.compose.CardBackgroundBox
import net.schacher.mcc.shared.design.compose.Tag
import net.schacher.mcc.shared.design.theme.ButtonSize
import net.schacher.mcc.shared.design.theme.ContentPadding
import net.schacher.mcc.shared.design.theme.DefaultShape
import net.schacher.mcc.shared.design.theme.FABPadding
import net.schacher.mcc.shared.design.theme.color
import net.schacher.mcc.shared.localization.label
import net.schacher.mcc.shared.model.Card
import net.schacher.mcc.shared.platform.isAndroid
import net.schacher.mcc.shared.repositories.CardRepository
import net.schacher.mcc.shared.repositories.DeckRepository
import net.schacher.mcc.shared.screens.AppRoute
import net.schacher.mcc.shared.screens.navigate
import net.schacher.mcc.shared.screens.resultState
import org.koin.compose.koinInject

@Composable
fun CardScreen(
    cardCode: String,
    modifier: Modifier = Modifier,
    cardRepository: CardRepository = koinInject(),
    deckRepository: DeckRepository = koinInject(),
    navController: NavController = koinInject()
) {
    var card by remember { mutableStateOf<Card?>(null) }
    LaunchedEffect(cardCode) {
        card = cardRepository.getCard(cardCode)
    }

    val selectedDeckId = navController.resultState<Int?>()?.value
    selectedDeckId?.let {
        LaunchedEffect(it) {
            deckRepository.addCardToDeck(it, cardCode)
        }
    }

    card?.let {
        CardScreen(
            card = it,
            modifier = modifier,
            onCloseClick = { navController.popBackStack() },
            onAddToDeckClick = {
                navController.navigate(AppRoute.SelectDeck)
            }
        )
    }
}

@Composable
fun CardScreen(
    card: Card,
    modifier: Modifier = Modifier,
    onAddToDeckClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    CardBackgroundBox(
        cardCode = card.code,
        modifier = modifier
    ) {
        Content(
            card = card,
            onAddToDeckClick = onAddToDeckClick,
            onCloseClick = onCloseClick,
        )
    }
}

@Composable
private fun Content(
    card: Card,
    onAddToDeckClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val state = rememberLazyListState()

        LazyColumn(state = state) {
            item {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .statusBarsPadding()
                        .padding(ContentPadding),
                    horizontalAlignment = Alignment.Start
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            card = card,
                            modifier = Modifier.sizeIn(maxWidth = 260.dp)
                                .padding(vertical = ContentPadding)
                        )
                    }

                    Text(
                        modifier = Modifier.padding(top = ContentPadding),
                        text = card.name,
                        maxLines = 2,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colors.onSurface
                    )

                    Text(
                        text = card.packName,
                        maxLines = 1,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.75f)
                    )


                    LazyRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item { Tag(text = card.code) }
                        item { card.type?.let { Tag(text = it.label) } }
                        item { card.aspect?.let { Tag(text = it.label, color = it.color) } }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        card.traits?.let {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = it.toAnnotatedString(),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        card.text?.let {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = it.toAnnotatedString(),
                                fontSize = 18.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        card.attackText?.let {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = it.toAnnotatedString(),
                                fontSize = 18.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        card.boostText?.let {
                            Text(
                                modifier = Modifier.padding(top = 16.dp),
                                text = buildAnnotatedString {
                                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                                    append("Boost: ")
                                    pop()
                                    append(it.toAnnotatedString())
                                },
                                fontSize = 18.sp,
                                color = MaterialTheme.colors.onSurface
                            )
                        }

                        card.quote?.let {
                            Text(
                                modifier = Modifier.padding(top = 32.dp),
                                text = it,
                                fontSize = 18.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colors.onSurface
                            )
                        }
                    }

                    BottomSpacer()
                }

            }
        }

        BackButton(onCloseClick)


        FloatingActionButton(
            onClick = onAddToDeckClick,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                .padding(
                    end = FABPadding,
                    bottom = if (isAndroid()) ContentPadding else 0.dp
                )
                .size(ButtonSize),
            contentColor = MaterialTheme.colors.onPrimary,
            backgroundColor = MaterialTheme.colors.primary,
            shape = DefaultShape
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add to deck"
            )
        }
    }
}


private val boldRegex = Regex("<b>(.*?)</b>|\\[\\[(.*?)]]|\\[(.*?)]")

private val italicRegex = Regex("<i>(.*?)</i>")

private const val EMOJI_HERO = "\uD83D\uDE42"

private fun String.toAnnotatedString(): AnnotatedString {
    val value = this

    // Could be handled more elegantly, but this works for now
    val boldStrings = boldRegex.findAll(value).map {
        it.value.replace("<b>", "")
            .replace("</b>", "")
            .replace("[", "")
            .replace("]", "")
            .replace("per_hero", EMOJI_HERO)
            .replace("per_player", EMOJI_HERO)
    }.toList()

    return buildAnnotatedString {
        value.split(boldRegex).forEachIndexed { index, s ->
            append(s)

            if (index < boldStrings.count()) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(boldStrings[index])
                pop()
            }
        }
    }
}