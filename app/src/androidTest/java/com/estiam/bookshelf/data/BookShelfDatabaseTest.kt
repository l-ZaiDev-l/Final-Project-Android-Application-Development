package com.estiam.bookshelf.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.BookShelfDatabase
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.local.ReadingSession
import com.estiam.bookshelf.data.repository.BookRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room relationships + repository transactions, on an in-memory database. */
@RunWith(AndroidJUnit4::class)
class BookShelfDatabaseTest {

    private lateinit var db: BookShelfDatabase
    private lateinit var repository: BookRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, BookShelfDatabase::class.java).build()
        repository = BookRepository(db) { 1_000L }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addingAndDeletingSessionsKeepsBookProgressConsistent() = runTest {
        val bookId = db.bookDao().insert(Book(title = "Dune", author = "Herbert", totalPages = 100))

        repository.addSession(ReadingSession(bookId = bookId, date = 0, pagesRead = 60, durationMinutes = 30))
        repository.addSession(ReadingSession(bookId = bookId, date = 0, pagesRead = 40, durationMinutes = 20))
        val completed = db.bookDao().getById(bookId)!!
        assertEquals(100, completed.currentPage)
        assertNotNull(completed.completedAt)

        val session = db.sessionDao().getForBook(bookId).first { it.pagesRead == 40 }
        repository.deleteSession(session)
        val reverted = db.bookDao().getById(bookId)!!
        assertEquals(60, reverted.currentPage)
        assertNull(reverted.completedAt)
    }

    @Test
    fun deletingBookCascadesToSessions() = runTest {
        val bookId = db.bookDao().insert(Book(title = "Emma", author = "Austen", totalPages = 300))
        repository.addSession(ReadingSession(bookId = bookId, date = 0, pagesRead = 10, durationMinutes = 5))

        repository.deleteBook(db.bookDao().getById(bookId)!!)

        assertTrue(db.sessionDao().getForBook(bookId).isEmpty())
        assertTrue(db.sessionDao().observeAll().first().isEmpty())
    }

    @Test
    fun deletingCategoryKeepsItsBooksWithoutCategory() = runTest {
        val categoryId = db.categoryDao().insert(Category(name = "Classics"))
        val bookId = db.bookDao().insert(Book(title = "Ulysses", author = "Joyce", totalPages = 700, categoryId = categoryId))

        db.categoryDao().delete(Category(id = categoryId, name = "Classics"))

        val details = db.bookDao().observeDetails(bookId).first()!!
        assertNull(details.book.categoryId)
        assertNull(details.category)
    }

    @Test
    fun insertIgnoreNeverDuplicatesNorOverwritesUserChanges() = runTest {
        val id = db.bookDao().insert(Book(title = "Dune", author = "Herbert", totalPages = 100, currentPage = 42))

        val result = db.bookDao().insertAllIgnore(
            listOf(Book(title = "Dune", author = "Herbert", totalPages = 999, currentPage = 0))
        )

        assertEquals(listOf(-1L), result)
        assertEquals(1, db.bookDao().count())
        assertEquals(42, db.bookDao().getById(id)!!.currentPage)
    }
}
