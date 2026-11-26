package com.estiam.bookshelf.domain

import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.local.ReadingSession
import java.util.TimeZone
import kotlin.math.ceil

/** Per-category line of the statistics screen. [name] is null for books without category. */
data class CategoryStat(val name: String?, val bookCount: Int, val pagesRead: Int)

data class ReadingStats(
    val totalBooks: Int = 0,
    val completedBooks: Int = 0,
    val inProgressBooks: Int = 0,
    val notStartedBooks: Int = 0,
    val favoriteBooks: Int = 0,
    val pagesRead: Int = 0,
    val sessionCount: Int = 0,
    val totalMinutes: Int = 0,
    val pagesPerHour: Int = 0,
    val currentStreakDays: Int = 0,
    val pagesToday: Int = 0,
    val dailyGoal: Int = 0,
    val goalPercent: Int = 0,
    val pagesRemaining: Int = 0,
    val estimatedDaysToFinish: Int? = null,
    val averageRating: Double = 0.0,
    val categoryStats: List<CategoryStat> = emptyList()
)

/**
 * Business rule #2: reading statistics, daily goal and reading streak.
 * Pure function of the data (+ clock and time zone) so it is fully unit-testable.
 */
object ReadingStatsCalculator {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun epochDay(millis: Long, timeZone: TimeZone): Long =
        Math.floorDiv(millis + timeZone.getOffset(millis), DAY_MS)

    /** Number of consecutive days with reading, ending today (or yesterday if nothing read yet today). */
    fun currentStreak(readingDays: Set<Long>, today: Long): Int {
        var day = when {
            today in readingDays -> today
            (today - 1) in readingDays -> today - 1
            else -> return 0
        }
        var streak = 0
        while (day in readingDays) {
            streak++
            day--
        }
        return streak
    }

    fun compute(
        books: List<Book>,
        sessions: List<ReadingSession>,
        categories: List<Category>,
        dailyGoal: Int,
        now: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): ReadingStats {
        val statuses = books.map { ReadingProgress.status(it) }
        val completed = statuses.count { it == ReadingStatus.COMPLETED }
        val reading = statuses.count { it == ReadingStatus.READING }

        val sessionPages = sessions.sumOf { it.pagesRead }
        val minutes = sessions.sumOf { it.durationMinutes }
        val pagesPerHour = if (minutes > 0) (sessionPages * 60.0 / minutes).toInt() else 0

        val today = epochDay(now, timeZone)
        val pagesByDay = sessions.groupBy { epochDay(it.date, timeZone) }
            .mapValues { (_, list) -> list.sumOf { it.pagesRead } }
        val pagesToday = pagesByDay[today] ?: 0
        val streak = currentStreak(pagesByDay.filterValues { it > 0 }.keys, today)

        val goalPercent = if (dailyGoal > 0) (pagesToday * 100 / dailyGoal).coerceAtMost(100) else 0

        val remaining = books
            .filter { ReadingProgress.status(it) == ReadingStatus.READING }
            .sumOf { ReadingProgress.remainingPages(it) }
        val avgPagesPerActiveDay = if (pagesByDay.isNotEmpty()) sessionPages.toDouble() / pagesByDay.size else 0.0
        val estimatedDays = if (remaining > 0 && avgPagesPerActiveDay > 0) {
            ceil(remaining / avgPagesPerActiveDay).toInt()
        } else null

        val rated = books.filter { it.rating > 0 }
        val avgRating = if (rated.isEmpty()) 0.0 else rated.map { it.rating }.average()

        val namesById = categories.associate { it.id to it.name }
        val categoryStats = books.groupBy { it.categoryId }
            .map { (categoryId, list) ->
                CategoryStat(
                    name = categoryId?.let { namesById[it] },
                    bookCount = list.size,
                    pagesRead = list.sumOf { it.currentPage }
                )
            }
            .sortedWith(compareByDescending<CategoryStat> { it.pagesRead }.thenBy { it.name ?: "" })

        return ReadingStats(
            totalBooks = books.size,
            completedBooks = completed,
            inProgressBooks = reading,
            notStartedBooks = books.size - completed - reading,
            favoriteBooks = books.count { it.isFavorite },
            pagesRead = books.sumOf { it.currentPage },
            sessionCount = sessions.size,
            totalMinutes = minutes,
            pagesPerHour = pagesPerHour,
            currentStreakDays = streak,
            pagesToday = pagesToday,
            dailyGoal = dailyGoal,
            goalPercent = goalPercent,
            pagesRemaining = remaining,
            estimatedDaysToFinish = estimatedDays,
            averageRating = avgRating,
            categoryStats = categoryStats
        )
    }
}
