package com.estiam.bookshelf.ui.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.repository.BookRepository
import com.estiam.bookshelf.data.repository.CategoryRepository
import com.estiam.bookshelf.domain.BookField
import com.estiam.bookshelf.domain.BookValidator
import com.estiam.bookshelf.domain.SaveResult
import com.estiam.bookshelf.domain.ValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Raw content of the form (strings, as typed by the user). */
data class BookForm(
    val title: String,
    val author: String,
    val totalPages: String,
    val currentPage: String,
    val rating: Int,
    val favorite: Boolean,
    val description: String
)

sealed interface BookEditEvent {
    data class Saved(val id: Long) : BookEditEvent
    data class Failed(val message: String) : BookEditEvent
}

class BookEditViewModel(
    private val bookRepository: BookRepository,
    categoryRepository: CategoryRepository,
    private val state: SavedStateHandle
) : ViewModel() {

    val bookId: Long = state[BookEditFragment.ARG_BOOK_ID] ?: NEW_BOOK
    val isNew: Boolean get() = bookId == NEW_BOOK

    val categories: StateFlow<List<Category>?> = categoryRepository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errors = MutableStateFlow<Map<BookField, ValidationError>>(emptyMap())
    val errors: StateFlow<Map<BookField, ValidationError>> = _errors.asStateFlow()

    private val _events = Channel<BookEditEvent>(Channel.BUFFERED)
    val events: Flow<BookEditEvent> = _events.receiveAsFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Selected category, kept in the SavedStateHandle (survives rotation; the Spinner loads async). */
    var selectedCategoryId: Long?
        get() = state.get<Long>(KEY_CATEGORY)?.takeIf { it != NO_CATEGORY }
        set(value) {
            state[KEY_CATEGORY] = value ?: NO_CATEGORY
        }

    /** True once the fields were filled from the database: never overwrite what the user typed. */
    var formPopulated: Boolean
        get() = state[KEY_POPULATED] ?: false
        set(value) {
            state[KEY_POPULATED] = value
        }

    private var original: Book? = null

    suspend fun loadBook(): Book? {
        if (isNew) return null
        return bookRepository.getBook(bookId).also { original = it }
    }

    fun save(form: BookForm) {
        if (_saving.value) return
        val errors = BookValidator.validate(form.title, form.author, form.totalPages, form.currentPage, form.rating)
        _errors.value = errors
        if (errors.isNotEmpty()) return

        viewModelScope.launch {
            _saving.value = true
            try {
                val base = when {
                    isNew -> Book(title = "", author = "", totalPages = 1)
                    else -> original ?: bookRepository.getBook(bookId)
                }
                if (base == null) {
                    _events.send(BookEditEvent.Failed("Book no longer exists"))
                    return@launch
                }
                val book = base.copy(
                    title = form.title.trim(),
                    author = form.author.trim(),
                    categoryId = selectedCategoryId,
                    totalPages = form.totalPages.trim().toInt(),
                    currentPage = form.currentPage.trim().toIntOrNull() ?: 0,
                    rating = form.rating,
                    isFavorite = form.favorite,
                    description = form.description.trim()
                )
                when (val result = bookRepository.saveBook(book)) {
                    is SaveResult.Success -> _events.send(BookEditEvent.Saved(result.id))
                    SaveResult.Duplicate -> _errors.value = mapOf(BookField.TITLE to ValidationError.DUPLICATE)
                    is SaveResult.Failure -> _events.send(BookEditEvent.Failed(result.message))
                }
            } finally {
                _saving.value = false
            }
        }
    }

    companion object {
        const val NEW_BOOK = -1L
        private const val NO_CATEGORY = -1L
        private const val KEY_CATEGORY = "selectedCategory"
        private const val KEY_POPULATED = "formPopulated"
    }
}
