package com.estiam.bookshelf.domain

/** Filter chips of the catalogue. [code] is the value understood by BookDao.pagingSource. */
enum class BookFilter(val code: Int) {
    ALL(0), FAVORITES(1), READING(2), COMPLETED(3), NOT_STARTED(4);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: ALL
    }
}

/** Sort orders of the catalogue (persisted in DataStore). */
enum class SortOrder(val code: Int) {
    TITLE(0), AUTHOR(1), RECENT(2), RATING(3), PROGRESS(4);

    companion object {
        fun fromCode(code: Int) = entries.firstOrNull { it.code == code } ?: TITLE
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ReadingStatus { NOT_STARTED, READING, COMPLETED }

/** Everything needed to build one catalogue query. */
data class BookQuery(
    val search: String = "",
    val categoryId: Long? = null,
    val filter: BookFilter = BookFilter.ALL,
    val sort: SortOrder = SortOrder.TITLE
)

/** Result of a write operation in a repository. */
sealed interface SaveResult {
    data class Success(val id: Long) : SaveResult
    data object Duplicate : SaveResult
    data class Failure(val message: String) : SaveResult
}
