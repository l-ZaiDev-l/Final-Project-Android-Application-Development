package com.estiam.bookshelf.ui.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.local.BookWithDetails
import com.estiam.bookshelf.data.local.ReadingSession
import com.estiam.bookshelf.data.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BookDetailState {
    data object Loading : BookDetailState
    data object NotFound : BookDetailState
    data class Loaded(val details: BookWithDetails) : BookDetailState
}

class BookDetailViewModel(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bookId: Long = savedStateHandle[BookDetailFragment.ARG_BOOK_ID] ?: -1L

    val state: StateFlow<BookDetailState> = repository.observeBook(bookId)
        .map { if (it == null) BookDetailState.NotFound else BookDetailState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookDetailState.Loading)

    private val currentBook get() = (state.value as? BookDetailState.Loaded)?.details?.book

    fun toggleFavorite() {
        val book = currentBook ?: return
        viewModelScope.launch { repository.setFavorite(book.id, !book.isFavorite) }
    }

    /** The detail Flow then emits null -> NotFound -> the screen closes itself. */
    fun deleteBook() {
        val book = currentBook ?: return
        viewModelScope.launch { repository.deleteBook(book) }
    }

    fun deleteSession(session: ReadingSession) {
        viewModelScope.launch { repository.deleteSession(session) }
    }
}
