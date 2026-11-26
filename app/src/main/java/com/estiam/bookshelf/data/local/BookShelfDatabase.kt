package com.estiam.bookshelf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Category::class, Book::class, ReadingSession::class],
    version = 1,
    exportSchema = false
)
abstract class BookShelfDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): ReadingSessionDao

    companion object {
        private const val DB_NAME = "bookshelf.db"

        @Volatile
        private var instance: BookShelfDatabase? = null

        fun getInstance(context: Context): BookShelfDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookShelfDatabase::class.java,
                    DB_NAME
                ).build().also { instance = it }
            }
    }
}
