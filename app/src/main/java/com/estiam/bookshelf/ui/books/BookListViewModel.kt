package com.estiam.bookshelf.ui.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.estiam.bookshelf.data.local.BookListItem
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.prefs.UserPreferences
import com.estiam.bookshelf.data.prefs.UserPreferencesRepository
import com.estiam.bookshelf.data.repository.BookRepository
import com.estiam.bookshelf.data.repository.CategoryRepository
import com.estiam.bookshelf.domain.BookFilter
import com.estiam.bookshelf.domain.BookQuery
import com.estiam.bookshelf.domain.SortOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State of the catalogue. Search / filter / category live in the SavedStateHandle so they
 * survive rotation AND process death; the sort order comes from DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModel(
    private val bookRepository: BookRepository,
    categoryRepository: CategoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val search: StateFlow<String> = state.getStateFlow(KEY_SEARCH, "")
    val filter: StateFlow<Int> = state.getStateFlow(KEY_FILTER, BookFilter.ALL.code)
    val categoryId: StateFlow<Long> = state.getStateFlow(KEY_CATEGORY, ALL_CATEGORIES)

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    /** null while loading, so a restored category selection is not reset by an empty initial list. */
    val categories: StateFlow<List<Category>?> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Paged catalogue, re-queried automatically whenever one of the inputs changes. */
    val books: Flow<PagingData<BookListItem>> = combine(
        search,
        filter,
        categoryId,
        preferencesRepository.preferences.map { it.sortOrder }.distinctUntilChanged()
    ) { search, filter, category, sort ->
        BookQuery(
            search = search,
            categoryId = category.takeIf { it != ALL_CATEGORIES },
            filter = BookFilter.fromCode(filter),
            sort = sort
        )
    }
        .distinctUntilChanged()
        .flatMapLatest { bookRepository.pagedBooks(it) }
        .cachedIn(viewModelScope)

    val hasActiveCriteria: Boolean
        get() = search.value.isNotBlank() || filter.value != BookFilter.ALL.code || categoryId.value != ALL_CATEGORIES

    fun setSearch(text: String) {
        if (text != search.value) state[KEY_SEARCH] = text
    }

    fun setFilter(filter: BookFilter) {
        state[KEY_FILTER] = filter.code
    }

    fun setCategory(id: Long) {
        if (id != categoryId.value) state[KEY_CATEGORY] = id
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { preferencesRepository.setSortOrder(order) }
    }

    fun toggleFavorite(item: BookListItem) {
        viewModelScope.launch { bookRepository.setFavorite(item.book.id, !item.book.isFavorite) }
    }

    companion object {
        const val ALL_CATEGORIES = -1L
        private const val KEY_SEARCH = "search"
        private const val KEY_FILTER = "filter"
        private const val KEY_CATEGORY = "category"
    }
}
