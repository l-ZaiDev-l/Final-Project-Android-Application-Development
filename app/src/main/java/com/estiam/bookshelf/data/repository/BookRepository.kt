package com.estiam.bookshelf.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.BookListItem
import com.estiam.bookshelf.data.local.BookShelfDatabase
import com.estiam.bookshelf.data.local.BookWithDetails
import com.estiam.bookshelf.data.local.ReadingSession
import com.estiam.bookshelf.domain.BookQuery
import com.estiam.bookshelf.domain.ReadingProgress
import com.estiam.bookshelf.domain.SaveResult
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for books and reading sessions.
 * ViewModels never talk to the DAOs directly.
 */
class BookRepository(
    private val db: BookShelfDatabase,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val bookDao = db.bookDao()
    private val sessionDao = db.sessionDao()

    /** Paging 3 stream of the main catalogue. */
    fun pagedBooks(query: BookQuery): Flow<PagingData<BookListItem>> {
        Log.d(TAG, "pagedBooks query=$query")
        return Pager(
            config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)
        ) {
            bookDao.pagingSource(
                search = query.search.trim(),
                categoryId = query.categoryId,
                filter = query.filter.code,
                sort = query.sort.code
            )
        }.flow
    }

    fun observeBook(id: Long): Flow<BookWithDetails?> = bookDao.observeDetails(id)

    fun observeAllBooks(): Flow<List<Book>> = bookDao.observeAll()

    fun observeAllSessions(): Flow<List<ReadingSession>> = sessionDao.observeAll()

    suspend fun getBook(id: Long): Book? = bookDao.getById(id)

    /** Inserts (id == 0) or updates a book. Returns [SaveResult.Duplicate] if (title, author) already exists. */
    suspend fun saveBook(book: Book): SaveResult {
        val normalized = ReadingProgress.normalize(book, clock())
        return try {
            if (normalized.id == 0L) {
                SaveResult.Success(bookDao.insert(normalized))
            } else {
                bookDao.update(normalized)
                SaveResult.Success(normalized.id)
            }
        } catch (e: SQLiteConstraintException) {
            Log.w(TAG, "Duplicate book '${book.title}' by '${book.author}'", e)
            SaveResult.Duplicate
        }
    }

    suspend fun deleteBook(book: Book) {
        Log.d(TAG, "deleteBook id=${book.id} (sessions are removed by CASCADE)")
        bookDao.delete(book)
    }

    suspend fun setFavorite(bookId: Long, favorite: Boolean) = bookDao.setFavorite(bookId, favorite)

    /** Adds a session AND advances the book progress in the same transaction. */
    suspend fun addSession(session: ReadingSession): Boolean = db.withTransaction {
        val book = bookDao.getById(session.bookId) ?: return@withTransaction false
        sessionDao.insert(session)
        val updated = ReadingProgress.applySession(book, session.pagesRead, clock())
        bookDao.update(updated)
        Log.d(TAG, "addSession book=${book.id} page ${book.currentPage} -> ${updated.currentPage}")
        true
    }

    /** Deletes a session AND moves the book progress back in the same transaction. */
    suspend fun deleteSession(session: ReadingSession) = db.withTransaction {
        sessionDao.delete(session)
        bookDao.getById(session.bookId)?.let { book ->
            val updated = ReadingProgress.revertSession(book, session.pagesRead, clock())
            bookDao.update(updated)
            Log.d(TAG, "deleteSession book=${book.id} page ${book.currentPage} -> ${updated.currentPage}")
        }
    }

    private companion object {
        const val TAG = "BookRepository"
        const val PAGE_SIZE = 20
    }
}
