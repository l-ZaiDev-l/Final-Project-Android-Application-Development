package com.estiam.bookshelf

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.estiam.bookshelf.data.local.BookShelfDatabase
import com.estiam.bookshelf.data.prefs.UserPreferencesRepository
import com.estiam.bookshelf.data.prefs.userDataStore
import com.estiam.bookshelf.data.repository.BookRepository
import com.estiam.bookshelf.data.repository.CategoryRepository
import com.estiam.bookshelf.data.seed.SeedImporter
import com.estiam.bookshelf.domain.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Manual dependency injection: one instance of each repository for the whole app. */
class AppContainer(context: Context) {
    val database = BookShelfDatabase.getInstance(context)
    val preferencesRepository = UserPreferencesRepository(context.userDataStore)
    val bookRepository = BookRepository(database)
    val categoryRepository = CategoryRepository(database.categoryDao())
    val seedImporter = SeedImporter(context, database, preferencesRepository)
}

class BookShelfApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Apply the saved theme before the first Activity is drawn (avoids a flash / recreation).
        val initialTheme = runBlocking { container.preferencesRepository.preferences.first().themeMode }
        applyTheme(initialTheme)

        // Then react to every later change of the preference.
        appScope.launch {
            container.preferencesRepository.preferences
                .map { it.themeMode }
                .distinctUntilChanged()
                .collect { applyTheme(it) }
        }

        // One-time JSON import.
        appScope.launch {
            try {
                container.seedImporter.importIfNeeded()
            } catch (e: Exception) {
                Log.e(TAG, "Seed import failed", e)
            }
        }
    }

    private fun applyTheme(mode: ThemeMode) {
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private companion object {
        const val TAG = "BookShelfApp"
    }
}
