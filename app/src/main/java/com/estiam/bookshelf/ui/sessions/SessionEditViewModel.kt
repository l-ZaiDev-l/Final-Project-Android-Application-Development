package com.estiam.bookshelf.ui.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.ReadingSession
import com.estiam.bookshelf.data.repository.BookRepository
import com.estiam.bookshelf.domain.ReadingProgress
import com.estiam.bookshelf.domain.SessionField
import com.estiam.bookshelf.domain.SessionValidator
import com.estiam.bookshelf.domain.ValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionEditEvent {
    data object Saved : SessionEditEvent
    data object BookMissing : SessionEditEvent
}

class SessionEditViewModel(
    private val repository: BookRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    private val bookId: Long = state[SessionEditFragment.ARG_BOOK_ID] ?: -1L

    val book: StateFlow<Book?> = repository.observeBook(bookId)
        .map { it?.book }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Session date, kept in the SavedStateHandle so it survives rotation. */
    val date: StateFlow<Long> = state.getStateFlow(KEY_DATE, System.currentTimeMillis())

    private val _errors = MutableStateFlow<Map<SessionField, ValidationError>>(emptyMap())
    val errors: StateFlow<Map<SessionField, ValidationError>> = _errors.asStateFlow()

    private val _events = Channel<SessionEditEvent>(Channel.BUFFERED)
    val events: Flow<SessionEditEvent> = _events.receiveAsFlow()

    fun setDate(millis: Long) {
        state[KEY_DATE] = millis
    }

    fun save(pages: String, minutes: String, note: String) {
        val current = book.value
        if (current == null) {
            viewModelScope.launch { _events.send(SessionEditEvent.BookMissing) }
            return
        }
        val errors = SessionValidator.validate(
            pages = pages,
            minutes = minutes,
            dateMillis = date.value,
            remainingPages = ReadingProgress.remainingPages(current),
            now = System.currentTimeMillis()
        )
        _errors.value = errors
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            val ok = repository.addSession(
                ReadingSession(
                    bookId = current.id,
                    date = date.value,
                    pagesRead = pages.trim().toInt(),
                    durationMinutes = minutes.trim().toInt(),
                    note = note.trim()
                )
            )
            _events.send(if (ok) SessionEditEvent.Saved else SessionEditEvent.BookMissing)
        }
    }

    private companion object {
        const val KEY_DATE = "date"
    }
}
