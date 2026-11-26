package com.estiam.bookshelf.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.local.CategoryDao
import com.estiam.bookshelf.data.local.CategoryWithCount
import com.estiam.bookshelf.domain.SaveResult
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val categoryDao: CategoryDao) {

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeCategoriesWithCount(): Flow<List<CategoryWithCount>> = categoryDao.observeWithCount()

    suspend fun getCategories(): List<Category> = categoryDao.getAll()

    suspend fun saveCategory(category: Category): SaveResult {
        val clean = category.copy(name = category.name.trim())
        return try {
            if (clean.id == 0L) {
                SaveResult.Success(categoryDao.insert(clean))
            } else {
                categoryDao.update(clean)
                SaveResult.Success(clean.id)
            }
        } catch (e: SQLiteConstraintException) {
            Log.w(TAG, "Duplicate category '${clean.name}'", e)
            SaveResult.Duplicate
        }
    }

    /** Books of this category are kept and become "uncategorized" (FK ON DELETE SET NULL). */
    suspend fun deleteCategory(category: Category) {
        Log.d(TAG, "deleteCategory id=${category.id}")
        categoryDao.delete(category)
    }

    private companion object {
        const val TAG = "CategoryRepository"
    }
}
