package com.estiam.bookshelf.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A shelf category (e.g. "Fiction"). One category -> many books.
 * The name is unique (case-sensitive at DB level, case-insensitive check in the validator).
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/**
 * A book of the personal library.
 *  - belongs to at most one [Category] (deleting the category sets categoryId to NULL)
 *  - the pair (title, author) is unique, which prevents duplicates (also during the JSON import)
 */
@Entity(
    tableName = "books",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index(value = ["title", "author"], unique = true)
    ]
)
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val categoryId: Long? = null,
    val totalPages: Int,
    val currentPage: Int = 0,
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * A reading session for one book. One book -> many sessions.
 * Sessions are deleted together with their book (CASCADE).
 */
@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val date: Long,
    val pagesRead: Int,
    val durationMinutes: Int,
    val note: String = ""
)
