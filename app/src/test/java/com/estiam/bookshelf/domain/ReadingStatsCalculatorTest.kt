package com.estiam.bookshelf.domain

import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.local.ReadingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class ReadingStatsCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")
    private val day = 24L * 60 * 60 * 1000
    private val now = 20_000L * day + 15 * 60 * 60 * 1000 // day 20000, 15:00 UTC

    private val fiction = Category(id = 1, name = "Fiction")
    private val books = listOf(
        Book(id = 1, title = "A", author = "X", categoryId = 1, totalPages = 100, currentPage = 100, rating = 4, isFavorite = true),
        Book(id = 2, title = "B", author = "Y", categoryId = 1, totalPages = 300, currentPage = 100, rating = 2),
        Book(id = 3, title = "C", author = "Z", categoryId = null, totalPages = 200, currentPage = 0)
    )

    private fun session(daysAgo: Int, pages: Int, minutes: Int = 30) =
        ReadingSession(bookId = 2, date = now - daysAgo * day, pagesRead = pages, durationMinutes = minutes)

    @Test
    fun `counts books by status, favorites and average rating`() {
        val stats = ReadingStatsCalculator.compute(books, emptyList(), listOf(fiction), 20, now, utc)
        assertEquals(3, stats.totalBooks)
        assertEquals(1, stats.completedBooks)
        assertEquals(1, stats.inProgressBooks)
        assertEquals(1, stats.notStartedBooks)
        assertEquals(1, stats.favoriteBooks)
        assertEquals(200, stats.pagesRead)
        assertEquals(3.0, stats.averageRating, 0.001)
    }

    @Test
    fun `daily goal progress uses only today's sessions and is capped at 100 percent`() {
        val sessions = listOf(session(0, 10), session(0, 5), session(1, 40))
        val stats = ReadingStatsCalculator.compute(books, sessions, listOf(fiction), 20, now, utc)
        assertEquals(15, stats.pagesToday)
        assertEquals(75, stats.goalPercent)

        val overGoal = ReadingStatsCalculator.compute(books, listOf(session(0, 50)), listOf(fiction), 20, now, utc)
        assertEquals(100, overGoal.goalPercent)
    }

    @Test
    fun `streak counts consecutive reading days and tolerates nothing read yet today`() {
        assertEquals(3, ReadingStatsCalculator.currentStreak(setOf(10L, 9L, 8L, 6L), today = 10L))
        assertEquals(2, ReadingStatsCalculator.currentStreak(setOf(9L, 8L), today = 10L))
        assertEquals(0, ReadingStatsCalculator.currentStreak(setOf(7L), today = 10L))
    }

    @Test
    fun `reading speed and finish estimate are computed from sessions`() {
        // 2 active days, 60 pages in 120 minutes -> 30 pages/hour and 30 pages/day
        val sessions = listOf(session(0, 20, 40), session(1, 40, 80))
        val stats = ReadingStatsCalculator.compute(books, sessions, listOf(fiction), 20, now, utc)
        assertEquals(30, stats.pagesPerHour)
        assertEquals(200, stats.pagesRemaining)        // book B: 300 - 100
        assertEquals(7, stats.estimatedDaysToFinish)   // ceil(200 / 30)
    }

    @Test
    fun `no sessions means no estimate and empty category stats only for existing books`() {
        val stats = ReadingStatsCalculator.compute(books, emptyList(), listOf(fiction), 20, now, utc)
        assertNull(stats.estimatedDaysToFinish)
        assertEquals(2, stats.categoryStats.size)
        assertEquals("Fiction", stats.categoryStats.first().name)
        assertEquals(200, stats.categoryStats.first().pagesRead)
        assertNull(stats.categoryStats.last().name) // uncategorized
    }
}
