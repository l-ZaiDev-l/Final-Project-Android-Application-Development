package com.estiam.bookshelf.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.estiam.bookshelf.domain.SortOrder
import com.estiam.bookshelf.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** User preferences persisted with Preferences DataStore. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,   // visibly changes the appearance (dark / light)
    val sortOrder: SortOrder = SortOrder.TITLE,    // changes the catalogue order
    val compactList: Boolean = false,              // changes the catalogue row layout
    val dailyGoal: Int = DEFAULT_DAILY_GOAL        // used by the statistics screen
) {
    companion object {
        const val DEFAULT_DAILY_GOAL = 20
    }
}

class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val SORT = intPreferencesKey("sort_order")
        val COMPACT = booleanPreferencesKey("compact_list")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val SEED_IMPORTED = booleanPreferencesKey("seed_imported")
    }

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e(TAG, "Error reading preferences, using defaults", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }
        .map { p ->
            UserPreferences(
                themeMode = p[Keys.THEME]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.SYSTEM,
                sortOrder = SortOrder.fromCode(p[Keys.SORT] ?: SortOrder.TITLE.code),
                compactList = p[Keys.COMPACT] ?: false,
                dailyGoal = p[Keys.DAILY_GOAL] ?: UserPreferences.DEFAULT_DAILY_GOAL
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setSortOrder(order: SortOrder) = dataStore.edit { it[Keys.SORT] = order.code }

    suspend fun setCompactList(compact: Boolean) = dataStore.edit { it[Keys.COMPACT] = compact }

    suspend fun setDailyGoal(goal: Int) = dataStore.edit { it[Keys.DAILY_GOAL] = goal.coerceIn(1, 500) }

    suspend fun isSeedImported(): Boolean = dataStore.data.first()[Keys.SEED_IMPORTED] ?: false

    suspend fun setSeedImported() = dataStore.edit { it[Keys.SEED_IMPORTED] = true }

    private companion object {
        const val TAG = "UserPreferences"
    }
}
