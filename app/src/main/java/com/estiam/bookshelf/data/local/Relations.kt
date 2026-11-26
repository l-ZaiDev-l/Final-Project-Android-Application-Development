package com.estiam.bookshelf.data.local

import androidx.room.Embedded
import androidx.room.Relation

/** Row of the paged catalogue: a book + the name of its category (LEFT JOIN). */
data class BookListItem(
    @Embedded val book: Book,
    val categoryName: String?
)

/** Book detail screen: a book with its category (N-1) and its reading sessions (1-N). */
data class BookWithDetails(
    @Embedded val book: Book,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category?,
    @Relation(parentColumn = "id", entityColumn = "bookId")
    val sessions: List<ReadingSession>
)

/** Category with the number of books it contains. */
data class CategoryWithCount(
    @Embedded val category: Category,
    val bookCount: Int
)
