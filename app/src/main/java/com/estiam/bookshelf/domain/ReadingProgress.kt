package com.estiam.bookshelf.domain

import com.estiam.bookshelf.data.local.Book

/**
 * Business rule #1: keeps a book's progress consistent with its reading sessions.
 *
 *  - logging a session moves currentPage forward (never beyond totalPages)
 *  - deleting a session moves it back (never below 0)
 *  - a book is "completed" when currentPage == totalPages; completedAt is set / cleared accordingly
 */
object ReadingProgress {

    fun percent(currentPage: Int, totalPages: Int): Int =
        if (totalPages <= 0) 0 else currentPage.coerceIn(0, totalPages) * 100 / totalPages

    fun percent(book: Book): Int = percent(book.currentPage, book.totalPages)

    fun remainingPages(book: Book): Int = (book.totalPages - book.currentPage).coerceAtLeast(0)

    fun status(book: Book): ReadingStatus = when {
        book.totalPages > 0 && book.currentPage >= book.totalPages -> ReadingStatus.COMPLETED
        book.currentPage > 0 -> ReadingStatus.READING
        else -> ReadingStatus.NOT_STARTED
    }

    fun applySession(book: Book, pagesRead: Int, now: Long): Book =
        normalize(book.copy(currentPage = book.currentPage + pagesRead.coerceAtLeast(0)), now)

    fun revertSession(book: Book, pagesRead: Int, now: Long): Book =
        normalize(book.copy(currentPage = book.currentPage - pagesRead.coerceAtLeast(0)), now)

    /** Clamps currentPage into [0, totalPages] and keeps completedAt in sync with the status. */
    fun normalize(book: Book, now: Long): Book {
        val current = book.currentPage.coerceIn(0, book.totalPages.coerceAtLeast(0))
        val completed = book.totalPages > 0 && current >= book.totalPages
        val completedAt = when {
            !completed -> null
            book.completedAt != null -> book.completedAt
            else -> now
        }
        return book.copy(currentPage = current, completedAt = completedAt)
    }
}
