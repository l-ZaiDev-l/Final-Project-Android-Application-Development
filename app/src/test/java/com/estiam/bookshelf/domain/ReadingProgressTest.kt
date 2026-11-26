package com.estiam.bookshelf.domain

import com.estiam.bookshelf.data.local.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingProgressTest {

    private val now = 1_700_000_000_000L
    private val book = Book(id = 1, title = "Dune", author = "Frank Herbert", totalPages = 200, currentPage = 50)

    @Test
    fun `applying a session advances the current page`() {
        val updated = ReadingProgress.applySession(book, pagesRead = 30, now = now)
        assertEquals(80, updated.currentPage)
        assertEquals(ReadingStatus.READING, ReadingProgress.status(updated))
        assertNull(updated.completedAt)
    }

    @Test
    fun `progress never goes beyond total pages and the book becomes completed`() {
        val updated = ReadingProgress.applySession(book, pagesRead = 500, now = now)
        assertEquals(200, updated.currentPage)
        assertEquals(ReadingStatus.COMPLETED, ReadingProgress.status(updated))
        assertEquals(now, updated.completedAt)
        assertEquals(100, ReadingProgress.percent(updated))
    }

    @Test
    fun `reverting a session on a completed book clears completedAt and never goes below 0`() {
        val completed = book.copy(currentPage = 200, completedAt = now)
        val reverted = ReadingProgress.revertSession(completed, pagesRead = 20, now = now)
        assertEquals(180, reverted.currentPage)
        assertNull(reverted.completedAt)

        val belowZero = ReadingProgress.revertSession(book, pagesRead = 999, now = now)
        assertEquals(0, belowZero.currentPage)
        assertEquals(ReadingStatus.NOT_STARTED, ReadingProgress.status(belowZero))
    }

    @Test
    fun `percent and remaining pages are computed from the book`() {
        assertEquals(25, ReadingProgress.percent(book))
        assertEquals(150, ReadingProgress.remainingPages(book))
        assertEquals(0, ReadingProgress.percent(0, 0))
    }

    @Test
    fun `normalize keeps an existing completion date`() {
        val earlier = now - 86_400_000L
        val completed = book.copy(currentPage = 200, completedAt = earlier)
        assertEquals(earlier, ReadingProgress.normalize(completed, now).completedAt)
    }
}
