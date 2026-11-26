package com.estiam.bookshelf.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.data.local.CategoryWithCount
import com.estiam.bookshelf.data.repository.CategoryRepository
import com.estiam.bookshelf.domain.CategoryValidator
import com.estiam.bookshelf.domain.SaveResult
import com.estiam.bookshelf.domain.ValidationError
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(private val repository: CategoryRepository) : ViewModel() {

    /** null = still loading. */
    val categories: StateFlow<List<CategoryWithCount>?> = repository.observeCategoriesWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Creates (id == 0) or renames a category. Returns the validation error, or null on success. */
    suspend fun save(id: Long, name: String): ValidationError? {
        val otherNames = repository.getCategories().filter { it.id != id }.map { it.name }
        CategoryValidator.validate(name, otherNames)?.let { return it }
        return when (repository.saveCategory(Category(id = id, name = name))) {
            is SaveResult.Success -> null
            SaveResult.Duplicate -> ValidationError.DUPLICATE
            is SaveResult.Failure -> ValidationError.OUT_OF_RANGE
        }
    }

    fun delete(category: Category) {
        viewModelScope.launch { repository.deleteCategory(category) }
    }
}
