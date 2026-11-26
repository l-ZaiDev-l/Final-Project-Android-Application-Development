package com.estiam.bookshelf.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query(
        """
        SELECT c.*, (SELECT COUNT(*) FROM books b WHERE b.categoryId = c.id) AS bookCount
        FROM categories c
        ORDER BY c.name COLLATE NOCASE
        """
    )
    fun observeWithCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Category>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    /** Returns -1 when a category with the same name already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface BookDao {

    /**
     * Main catalogue, consumed through Paging 3.
     * filter: 0 = all, 1 = favorites, 2 = reading, 3 = completed, 4 = not started
     * sort:   0 = title, 1 = author, 2 = recently added, 3 = rating, 4 = progress
     */
    @Query(
        """
        SELECT b.*, c.name AS categoryName
        FROM books b
        LEFT JOIN categories c ON b.categoryId = c.id
        WHERE (:search = '' OR b.title LIKE '%' || :search || '%' OR b.author LIKE '%' || :search || '%')
          AND (:categoryId IS NULL OR b.categoryId = :categoryId)
          AND (:filter = 0
               OR (:filter = 1 AND b.isFavorite = 1)
               OR (:filter = 2 AND b.currentPage > 0 AND b.currentPage < b.totalPages)
               OR (:filter = 3 AND b.currentPage >= b.totalPages)
               OR (:filter = 4 AND b.currentPage = 0))
        ORDER BY
          CASE WHEN :sort = 0 THEN b.title END COLLATE NOCASE ASC,
          CASE WHEN :sort = 1 THEN b.author END COLLATE NOCASE ASC,
          CASE WHEN :sort = 2 THEN b.createdAt END DESC,
          CASE WHEN :sort = 3 THEN b.rating END DESC,
          CASE WHEN :sort = 4 THEN CAST(b.currentPage AS REAL) / b.totalPages END DESC,
          b.title COLLATE NOCASE ASC
        """
    )
    fun pagingSource(
        search: String,
        categoryId: Long?,
        filter: Int,
        sort: Int
    ): PagingSource<Int, BookListItem>

    @Transaction
    @Query("SELECT * FROM books WHERE id = :id")
    fun observeDetails(id: Long): Flow<BookWithDetails?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: Book): Long

    /** Used by the JSON import: an existing (title, author) is never overwritten. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(books: List<Book>): List<Long>

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}

@Dao
interface ReadingSessionDao {

    @Query("SELECT * FROM reading_sessions ORDER BY date DESC")
    fun observeAll(): Flow<List<ReadingSession>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY date DESC")
    suspend fun getForBook(bookId: Long): List<ReadingSession>

    @Insert
    suspend fun insert(session: ReadingSession): Long

    @Delete
    suspend fun delete(session: ReadingSession)
}
