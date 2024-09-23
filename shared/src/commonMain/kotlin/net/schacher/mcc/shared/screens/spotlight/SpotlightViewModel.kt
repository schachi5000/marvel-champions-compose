package net.schacher.mcc.shared.screens.spotlight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import net.schacher.mcc.shared.datasource.http.MarvelCDbDataSource
import net.schacher.mcc.shared.model.Deck
import net.schacher.mcc.shared.repositories.CardRepository

class SpotlightViewModel(
    private val cardRepository: CardRepository,
    private val marvelCDbDataSource: MarvelCDbDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())

    val state = _state.asStateFlow()

    private val dates: List<LocalDate>
        get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).let {
            listOf(
                it.date,
                it.date.minus(1, kotlinx.datetime.DateTimeUnit.DAY),
                it.date.minus(2, kotlinx.datetime.DateTimeUnit.DAY),
            )
        }

    init {
        this.onRefresh()
    }

    fun onRefresh() {
        if (this.state.value.loading) {
            return
        }

        _state.update {
            it.copy(
                decks = emptyMap(),
                loading = true
            )
        }

        this.viewModelScope.launch {
            val updatedDecks = mutableMapOf<LocalDate, List<Deck>>()
            dates.forEach { date ->
                val result = marvelCDbDataSource.getSpotlightDecksByDate(date) {
                    cardRepository.getCard(it)
                }

                result.exceptionOrNull()?.let {
                    Logger.e(it) { "Error loading decks for $date" }
                }

                result.getOrNull()?.let {
                    updatedDecks[date] = it
                    Logger.d { "${it.size} decks loaded to $date" }
                }

                _state.update {
                    it.copy(
                        decks = updatedDecks,
                        loading = updatedDecks.isNotEmpty()
                    )
                }
            }

            _state.update {
                it.copy(loading = false)
            }
        }
    }

    data class UiState(
        val decks: Map<LocalDate, List<Deck>> = emptyMap(),
        val loading: Boolean = false
    )
}

