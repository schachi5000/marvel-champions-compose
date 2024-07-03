package net.schacher.mcc.shared.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.schacher.mcc.shared.datasource.database.SettingsDao
import net.schacher.mcc.shared.platform.PlatformInfo
import net.schacher.mcc.shared.repositories.AuthRepository
import net.schacher.mcc.shared.repositories.CardRepository
import net.schacher.mcc.shared.repositories.DeckRepository
import net.schacher.mcc.shared.repositories.PackRepository

class SettingsViewModel(
    private val cardRepository: CardRepository,
    private val deckRepository: DeckRepository,
    private val packRepository: PackRepository,
    private val authRepository: AuthRepository,
    settingsDao: SettingsDao,
    platformInfo: PlatformInfo
) : ViewModel() {

    private val _state = MutableStateFlow(
        UiState(
            cardCount = cardRepository.cards.value.size,
            deckCount = deckRepository.decks.value.size,
            packCount = packRepository.packs.value.size,
            packsInCollectionCount = packRepository.packsInCollection.value.size,
            settingsValues = settingsDao.getAllEntries(),
            versionName = platformInfo.version,
            guestLogin = authRepository.isGuest()
        )
    )

    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.decks.collect { value ->
                _state.update { it.copy(deckCount = value.size) }
            }
        }

        viewModelScope.launch {
            authRepository.loginState.collect {
                _state.update { it.copy(guestLogin = authRepository.isGuest()) }
            }
        }

        viewModelScope.launch {
            packRepository.packsInCollection.collect { value ->
                _state.update { it.copy(packsInCollectionCount = value.size) }
            }
        }

        viewModelScope.launch {
            packRepository.packs.collect { value ->
                _state.update {
                    it.copy(
                        packCount = value.size,
                        packsInCollectionCount = packRepository.packsInCollection.value.size
                    )
                }
            }
        }
    }

    fun onWipeDatabaseClick() {
        if (this.state.value.syncInProgress) {
            return
        }

        viewModelScope.launch {
            Logger.i { "Wiping database..." }
            cardRepository.deleteAllCardData()
            deckRepository.deleteAllDeckData()
            packRepository.deleteAllPackData()
            Logger.i { "Wiping complete" }

            _state.update { it.copy(cardCount = 0, deckCount = 0) }
        }
    }

    fun onSyncClick() {
        _state.update { it.copy(syncInProgress = true) }

        viewModelScope.launch {
            try {
                cardRepository.refreshAllCards()
            } catch (e: Exception) {
                Logger.e(e) { "Error refreshing cards" }
            }

            try {
                packRepository.refreshAllPacks()
            } catch (e: Exception) {
                Logger.e(e) { "Error refreshing packs" }
            }

            _state.update {
                it.copy(
                    cardCount = cardRepository.cards.value.size,
                    deckCount = deckRepository.decks.value.size,
                    packCount = packRepository.packs.value.size,
                    packsInCollectionCount = packRepository.packsInCollection.value.size,
                    syncInProgress = false
                )
            }
        }
    }

    data class UiState(
        val cardCount: Int,
        val deckCount: Int,
        val packCount: Int,
        val packsInCollectionCount: Int,
        val syncInProgress: Boolean = false,
        val settingsValues: List<Pair<String, Any>> = emptyList(),
        val versionName: String,
        val guestLogin: Boolean
    )
}

