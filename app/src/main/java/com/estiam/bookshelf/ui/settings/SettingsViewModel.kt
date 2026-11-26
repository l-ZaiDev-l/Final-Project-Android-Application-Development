package com.estiam.bookshelf.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.prefs.UserPreferences
import com.estiam.bookshelf.data.prefs.UserPreferencesRepository
import com.estiam.bookshelf.domain.SortOrder
import com.estiam.bookshelf.domain.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    val preferences: StateFlow<UserPreferences?> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) = launch { repository.setThemeMode(mode) }
    fun setSortOrder(order: SortOrder) = launch { repository.setSortOrder(order) }
    fun setCompactList(compact: Boolean) = launch { repository.setCompactList(compact) }
    fun setDailyGoal(goal: Int) = launch { repository.setDailyGoal(goal) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
