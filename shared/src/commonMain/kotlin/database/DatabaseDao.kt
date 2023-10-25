package database

import co.touchlab.kermit.Logger
import model.Card
import model.Deck

class DatabaseDao(
    databaseDriverFactory: DatabaseDriverFactory
) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())

    private val dbQuery = database.appDatabaseQueries

    fun addCards(cards: List<Card>) {
        Logger.d { "Adding ${cards.size} cards to database" }
        cards.forEach { this.addCard(it) }
    }

    fun addCard(card: Card) {
        this.dbQuery.addCard(
            card.code,
            card.position.toLong(),
            card.type,
            card.name,
            card.imagePath,
            card.linkedCard?.code
        )
    }

    fun getAllCards(): List<Card> = this.dbQuery.selectAllCards()
        .executeAsList()
        .map {
            val card = it.toCard()
            val linkedCard = it.linkedCardCode?.let {
                dbQuery.selectCardByCode(it).executeAsList().firstOrNull()?.toCard()
            }

            card.copy(
                linkedCard = linkedCard?.copy(
                    linkedCard = card
                )
            )
        }

    fun addDeck(deck: Deck) {
        this.dbQuery.addDeck(deck.id, deck.name, deck.cards.map { it.code }.joinToString { "," })
    }

    fun getDecks(): List<Deck> = this.dbQuery.selectAllDecks().executeAsList().map {
        it.toDeck()
    }
}

private fun database.Card.toCard() = Card(
    code = this.code,
    position = this.position.toInt(),
    type = this.type,
    name = this.name,
    imagePath = this.imagePath,
    linkedCard = null
)

private fun database.Deck.toDeck() = Deck(
    id = this.id,
    name = this.name,
    cards = this.cardCodes.split(",").mapNotNull { null }
)