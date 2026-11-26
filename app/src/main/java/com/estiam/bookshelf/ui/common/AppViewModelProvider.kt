package com.estiam.bookshelf.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.estiam.bookshelf.AppContainer
import com.estiam.bookshelf.BookShelfApp
import com.estiam.bookshelf.ui.books.BookDetailViewModel
import com.estiam.bookshelf.ui.books.BookEditViewModel
import com.estiam.bookshelf.ui.books.BookListViewModel
import com.estiam.bookshelf.ui.categories.CategoryViewModel
import com.estiam.bookshelf.ui.sessions.SessionEditViewModel
import com.estiam.bookshelf.ui.settings.SettingsViewModel
import com.estiam.bookshelf.ui.stats.StatsViewModel

/** Creates every ViewModel of the app with its dependencies (manual DI). */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            BookListViewModel(
                container().bookRepository,
                container().categoryRepository,
                container().preferencesRepository,
                createSavedStateHandle()
            )
        }
        initializer { BookDetailViewModel(container().bookRepository, createSavedStateHandle()) }
        initializer {
            BookEditViewModel(container().bookRepository, container().categoryRepository, createSavedStateHandle())
        }
        initializer { SessionEditViewModel(container().bookRepository, createSavedStateHandle()) }
        initializer { CategoryViewModel(container().categoryRepository) }
        initializer {
            StatsViewModel(container().bookRepository, container().categoryRepository, container().preferencesRepository)
        }
        initializer { SettingsViewModel(container().preferencesRepository) }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as BookShelfApp).container
}
