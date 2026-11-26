package com.estiam.bookshelf.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.BookShelfDatabase
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.prefs.UserPreferencesRepository
import com.estiam.bookshelf.domain.ReadingProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports assets/seed_books.json into Room the very first time the app starts.
 *
 * Guarantees:
 *  - runs only once: a "seed_imported" flag is stored in DataStore after success
 *  - no duplicates: unique index (title, author) + INSERT OR IGNORE
 *  - never overwrites user changes: IGNORE keeps the existing row untouched
 */
class SeedImporter(
    private val context: Context,
    private val db: BookShelfDatabase,
    private val preferences: UserPreferencesRepository,
    private val assetName: String = ASSET_NAME
) {

    data class Report(val inserted: Int, val skippedExisting: Int, val rejected: Int)

    suspend fun importIfNeeded(): Report? = withContext(Dispatchers.IO) {
        if (preferences.isSeedImported()) {
            Log.d(TAG, "Seed already imported, skipping")
            return@withContext null
        }

        val json = context.assets.open(assetName).bufferedReader().use { it.readText() }
        val parsed = SeedParser.parse(json)
        parsed.rejected.forEach { Log.w(TAG, "Rejected record #${it.index}: ${it.reason}") }

        val now = System.currentTimeMillis()
        val ids = db.withTransaction {
            val categoryDao = db.categoryDao()
            parsed.categories.forEach { categoryDao.insertIgnore(Category(name = it)) }
            val categoryIds = categoryDao.getAll().associate { it.name.lowercase() to it.id }

            val books = parsed.books.mapIndexed { index, seed ->
                ReadingProgress.normalize(
                    Book(
                        title = seed.title!!,
                        author = seed.author!!,
                        categoryId = seed.category?.let { categoryIds[it.lowercase()] },
                        totalPages = seed.totalPages!!,
                        currentPage = seed.currentPage ?: 0,
                        rating = seed.rating ?: 0,
                        isFavorite = seed.favorite ?: false,
                        description = seed.description.orEmpty(),
                        // spread creation dates so that "Recently added" sorting is meaningful
                        createdAt = now - index * HOUR_MS
                    ),
                    now
                )
            }
            db.bookDao().insertAllIgnore(books)
        }

        preferences.setSeedImported()
        val inserted = ids.count { it != -1L }
        Report(inserted, ids.size - inserted, parsed.rejected.size).also {
            Log.i(TAG, "Seed import done: $it")
        }
    }

    companion object {
        const val ASSET_NAME = "seed_books.json"
        private const val TAG = "SeedImporter"
        private const val HOUR_MS = 60L * 60 * 1000
    }
}
