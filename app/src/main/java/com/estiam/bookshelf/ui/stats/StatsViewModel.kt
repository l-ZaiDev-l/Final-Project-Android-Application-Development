package com.estiam.bookshelf.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.prefs.UserPreferencesRepository
import com.estiam.bookshelf.data.repository.BookRepository
import com.estiam.bookshelf.data.repository.CategoryRepository
import com.estiam.bookshelf.domain.ReadingStats
import com.estiam.bookshelf.domain.ReadingStatsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Recomputes the statistics automatically whenever books, sessions, categories or the goal change. */
class StatsViewModel(
    bookRepository: BookRepository,
    categoryRepository: CategoryRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val stats: StateFlow<ReadingStats?> = combine(
        bookRepository.observeAllBooks(),
        bookRepository.observeAllSessions(),
        categoryRepository.observeCategories(),
        preferencesRepository.preferences
    ) { books, sessions, categories, prefs ->
        ReadingStatsCalculator.compute(books, sessions, categories, prefs.dailyGoal, System.currentTimeMillis())
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
