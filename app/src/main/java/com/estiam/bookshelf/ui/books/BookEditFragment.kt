package com.estiam.bookshelf.ui.books

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.Book
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.databinding.FragmentBookEditBinding
import com.estiam.bookshelf.domain.BookField
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import com.estiam.bookshelf.ui.common.message
import kotlinx.coroutines.launch

/** Screen 3 - create / edit a book (form with validation). */
class BookEditFragment : Fragment(R.layout.fragment_book_edit) {

    private var _binding: FragmentBookEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookEditViewModel by viewModels { AppViewModelProvider.Factory }
    private var spinnerCategories: List<Category> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBookEditBinding.bind(view)

        if (!viewModel.formPopulated) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.loadBook()?.let { fillForm(it) }
                viewModel.formPopulated = true
            }
        }

        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.selectedCategoryId = if (position == 0) null else spinnerCategories.getOrNull(position - 1)?.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.saveButton.setOnClickListener { viewModel.save(readForm()) }

        collectOnStarted(viewModel.categories) { categories ->
            if (categories != null) updateCategorySpinner(categories)
        }
        collectOnStarted(viewModel.errors) { showErrors(it) }
        collectOnStarted(viewModel.saving) { binding.saveButton.isEnabled = !it }
        collectOnStarted(viewModel.events) { event ->
            when (event) {
                is BookEditEvent.Saved -> {
                    Toast.makeText(requireContext(), R.string.book_saved, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is BookEditEvent.Failed ->
                    Toast.makeText(requireContext(), event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Done in onResume so the label set by the NavController (also after rotation) is replaced.
        (activity as? AppCompatActivity)?.supportActionBar?.title =
            getString(if (viewModel.isNew) R.string.title_add_book else R.string.title_edit_book)
    }

    private fun fillForm(book: Book) {
        binding.titleInput.setText(book.title)
        binding.authorInput.setText(book.author)
        binding.totalPagesInput.setText(book.totalPages.toString())
        binding.currentPageInput.setText(book.currentPage.toString())
        binding.ratingBar.rating = book.rating.toFloat()
        binding.favoriteCheck.isChecked = book.isFavorite
        binding.descriptionInput.setText(book.description)
        viewModel.selectedCategoryId = book.categoryId
        selectCategoryInSpinner()
    }

    private fun updateCategorySpinner(categories: List<Category>) {
        spinnerCategories = categories
        val labels = listOf(getString(R.string.no_category)) + categories.map { it.name }
        binding.categorySpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, labels
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        selectCategoryInSpinner()
    }

    private fun selectCategoryInSpinner() {
        val index = spinnerCategories.indexOfFirst { it.id == viewModel.selectedCategoryId }
        binding.categorySpinner.setSelection(if (index >= 0) index + 1 else 0, false)
    }

    private fun readForm() = BookForm(
        title = binding.titleInput.text?.toString().orEmpty(),
        author = binding.authorInput.text?.toString().orEmpty(),
        totalPages = binding.totalPagesInput.text?.toString().orEmpty(),
        currentPage = binding.currentPageInput.text?.toString().orEmpty(),
        rating = binding.ratingBar.rating.toInt(),
        favorite = binding.favoriteCheck.isChecked,
        description = binding.descriptionInput.text?.toString().orEmpty()
    )

    private fun showErrors(errors: Map<BookField, com.estiam.bookshelf.domain.ValidationError>) {
        val ctx = requireContext()
        binding.titleLayout.error = errors[BookField.TITLE]?.message(ctx)
        binding.authorLayout.error = errors[BookField.AUTHOR]?.message(ctx)
        binding.totalPagesLayout.error = errors[BookField.TOTAL_PAGES]?.message(ctx)
        binding.currentPageLayout.error = errors[BookField.CURRENT_PAGE]?.message(ctx)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
    }
}
