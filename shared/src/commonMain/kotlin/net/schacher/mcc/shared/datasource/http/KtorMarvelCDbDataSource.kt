package net.schacher.mcc.shared.datasource.http

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.toUpperCasePreservingASCIIRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.LocalDate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.schacher.mcc.shared.datasource.http.dto.CardDto
import net.schacher.mcc.shared.datasource.http.dto.DeckDto
import net.schacher.mcc.shared.datasource.http.dto.DeckUpdateResponseDto
import net.schacher.mcc.shared.datasource.http.dto.PackDto
import net.schacher.mcc.shared.model.Aspect
import net.schacher.mcc.shared.model.Card
import net.schacher.mcc.shared.model.CardType
import net.schacher.mcc.shared.model.CardType.ALLY
import net.schacher.mcc.shared.model.CardType.ATTACHMENT
import net.schacher.mcc.shared.model.CardType.ENVIRONMENT
import net.schacher.mcc.shared.model.CardType.EVENT
import net.schacher.mcc.shared.model.CardType.HERO
import net.schacher.mcc.shared.model.CardType.MAIN_SCHEME
import net.schacher.mcc.shared.model.CardType.MINION
import net.schacher.mcc.shared.model.CardType.OBLIGATION
import net.schacher.mcc.shared.model.CardType.PLAYER_SIDE_SCHEME
import net.schacher.mcc.shared.model.CardType.RESOURCE
import net.schacher.mcc.shared.model.CardType.SIDE_SCHEME
import net.schacher.mcc.shared.model.CardType.SUPPORT
import net.schacher.mcc.shared.model.CardType.TREACHERY
import net.schacher.mcc.shared.model.CardType.UPGRADE
import net.schacher.mcc.shared.model.CardType.VILLAIN
import net.schacher.mcc.shared.model.Deck
import net.schacher.mcc.shared.model.Faction
import net.schacher.mcc.shared.model.Pack
import net.schacher.mcc.shared.repositories.AuthRepository
import pro.schacher.mcc.BuildConfig
import kotlin.coroutines.coroutineContext

class KtorMarvelCDbDataSource(
    private val authRepository: AuthRepository
) : MarvelCDbDataSource {

    private val serviceUrl: String = BuildConfig.PROXY_URL

    private val authHeader: String
        get() = "Bearer ${this.authRepository.accessToken?.token ?: throw IllegalStateException("No access token available")}"

    @OptIn(ExperimentalSerializationApi::class)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    Logger.i { message }
                }
            }
            level = LogLevel.INFO
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    }

    override suspend fun getAllPacks() =
        httpClient.get("$serviceUrl/packs")
            .body<List<PackDto>>()
            .map {
                Pack(
                    code = it.code,
                    name = it.name,
                    cardCodes = this.getCardPack(it.code).map { it.code },
                    url = it.url,
                    id = it.id,
                    position = it.position
                )
            }.also {
                Logger.i { "Packs loaded: ${it.size}" }
            }

    override suspend fun getCardPack(packCode: String): List<Card> =
        runCatching {
            httpClient.get("$serviceUrl/pack/$packCode")
                .body<List<CardDto>>()
                .map { it.toCard() }
        }.getOrNull() ?: emptyList()

    override suspend fun getAllCards(): List<Card> {
        val allPacks = this.getAllPacks()

        return allPacks.map {
            CoroutineScope(coroutineContext).async {
                Logger.i { "Starting download of: ${it.name}" }
                val result = runCatching { getCardPack(it.code) }.getOrNull() ?: emptyList()
                Logger.i { "finished download of: ${it.name}" }
                result
            }
        }.awaitAll().flatten()
    }

    override suspend fun getCard(cardCode: String) =
        httpClient.get("$serviceUrl/card/$cardCode").body<CardDto>().toCard()

    override suspend fun getSpotlightDecksByDate(
        date: LocalDate, cardProvider: suspend (String) -> Card
    ) = runCatching {
        this.httpClient.get("$serviceUrl/spotlight/${date.toDateString()}")
            .body<List<DeckDto>>()
            .map {
                Deck(
                    id = it.id,
                    name = it.name,
                    hero = cardProvider(it.investigator_code!!),
                    aspect = it.meta?.parseAspect(),
                    cards = it.slots.entries
                        .map { entry ->
                            List(entry.value) {
                                runCatching {
                                    cardProvider(entry.key)
                                }.getOrNull()
                            }
                        }
                        .flatten()
                        .filterNotNull()
                )
            }
    }

    override suspend fun getUserDecks(cardProvider: suspend (String) -> Card) = httpClient
        .get("$serviceUrl/api/oauth2/decks") {
            headers { append("Authorization", authHeader) }
        }
        .body<List<DeckDto>>()
        .map { it.toDeck(cardProvider) }
        .sortedBy { it.name }

    override suspend fun getUserDeckById(
        deckId: Int,
        cardProvider: suspend (String) -> Card
    ): Deck = this.getUserDeckDtoById(deckId).toDeck(cardProvider)

    private suspend fun getUserDeckDtoById(deckId: Int) =
        this.httpClient.get("$serviceUrl/api/oauth2/deck/load/$deckId") {
            headers { append("Authorization", authHeader) }
        }.body<DeckDto>()

    override suspend fun updateDeck(deck: Deck): Deck {
        val slots = deck.cards.groupingBy { it.code }.eachCount().let {
            Json.encodeToString(it)
        }

        val response = this.httpClient.put("$serviceUrl/api/oauth2/deck/save/${deck.id}") {
            headers { append("Authorization", authHeader) }
            parameter("slots", slots)
        }.body<DeckUpdateResponseDto>()

        if (response.success) {
            return getUserDeckById(deck.id) { this.getCard(it) }
        } else {
            throw Exception("Failed to update deck: ${response.msg?.toString()}")
        }
    }
}

private fun LocalDate.toDateString(): String {
    val dayOfMonth = this.dayOfMonth.let { if (it < 10) "0$it" else it }
    val month = this.monthNumber.let { if (it < 10) "0$it" else it }

    return "${this.year}-${month}-${dayOfMonth}"
}

private const val LEADERSHIP = "leadership"

private const val JUSTICE = "justice"

private const val AGGRESSION = "aggression"

private const val PROTECTION = "protection"

private fun String.parseAspect(): Aspect? = when {
    this.contains(JUSTICE) -> Aspect.JUSTICE
    this.contains(LEADERSHIP) -> Aspect.LEADERSHIP
    this.contains(AGGRESSION) -> Aspect.AGGRESSION
    this.contains(PROTECTION) -> Aspect.PROTECTION
    else -> null
}

private fun CardDto.toCard() = Card(
    code = this.code,
    position = this.position,
    type = this.type_code.toCardType(),
    cost = this.cost,
    name = this.name.trim(),
    setCode = this.card_set_code,
    setName = this.card_set_name,
    packCode = this.pack_code,
    packName = this.pack_name,
    text = this.text?.replace("\n", "\n\n"),
    boostText = this.boost_text.cleanUp(),
    attackText = this.attack_text.cleanUp(),
    quote = this.flavor.cleanUp(),
    aspect = this.faction_code.parseAspect(),
    traits = this.traits?.takeIf { it.isNotEmpty() },
    faction = Faction.valueOf(this.faction_code.toUpperCasePreservingASCIIRules()),
)

private fun String?.cleanUp(): String? =
    this?.replace("\n ", "\n")?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.toCardType(): CardType? = when (this) {
    "hero" -> HERO
    "ally" -> ALLY
    "event" -> EVENT
    "support" -> SUPPORT
    "upgrade" -> UPGRADE
    "resource" -> RESOURCE
    "villain" -> VILLAIN
    "main_scheme" -> MAIN_SCHEME
    "side_scheme" -> SIDE_SCHEME
    "player_side_scheme" -> PLAYER_SIDE_SCHEME
    "attachment" -> ATTACHMENT
    "minion" -> MINION
    "treachery" -> TREACHERY
    "environment" -> ENVIRONMENT
    "obligation" -> OBLIGATION
    else -> null
}

private suspend fun DeckDto.toDeck(cardProvider: suspend (String) -> Card): Deck {
    val heroCard = cardProvider(this.investigator_code!!)

    return Deck(id = this.id,
        name = this.name,
        hero = heroCard,
        aspect = this.meta?.parseAspect(),
        cards = this.slots.entries
            .map { entry -> List(entry.value) { cardProvider(entry.key) } }
            .flatten()
            .toMutableList()
            .also { it.add(0, heroCard) })
}