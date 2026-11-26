package com.estiam.bookshelf.ui.books

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.databinding.FragmentBookListBinding
import com.estiam.bookshelf.domain.BookFilter
import com.estiam.bookshelf.domain.SortOrder
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted

/** Screen 1 - main catalogue (Paging 3 + search + filters + category + sort). */
class BookListFragment : Fragment(R.layout.fragment_book_list) {

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookListViewModel by viewModels { AppViewModelProvider.Factory }

    private lateinit var adapter: BookPagingAdapter
    private var spinnerCategories: List<Category> = emptyList()

    private val chipToFilter by lazy {
        mapOf(
            R.id.chipAll to BookFilter.ALL,
            R.id.chipFavorites to BookFilter.FAVORITES,
            R.id.chipReading to BookFilter.READING,
            R.id.chipCompleted to BookFilter.COMPLETED,
            R.id.chipNotStarted to BookFilter.NOT_STARTED
        )
    }

    private val sortItems = mapOf(
        R.id.sort_title to SortOrder.TITLE,
        R.id.sort_author to SortOrder.AUTHOR,
        R.id.sort_recent to SortOrder.RECENT,
        R.id.sort_rating to SortOrder.RATING,
        R.id.sort_progress to SortOrder.PROGRESS
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBookListBinding.bind(view)

        adapter = BookPagingAdapter(
            onClick = { item ->
                findNavController().navigate(
                    R.id.action_bookList_to_bookDetail,
                    bundleOf(BookDetailFragment.ARG_BOOK_ID to item.book.id)
                )
            },
            onToggleFavorite = viewModel::toggleFavorite
        )
        binding.bookRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.bookRecycler.adapter = adapter

        setupSearch()
        setupFilterChips()
        setupCategorySpinner()
        setupMenu()

        binding.addBookFab.setOnClickListener {
            findNavController().navigate(R.id.action_bookList_to_bookEdit)
        }
        binding.retryButton.setOnClickListener { adapter.retry() }

        collectOnStarted(viewModel.books) { adapter.submitData(it) }
        collectOnStarted(adapter.loadStateFlow) { states -> renderLoadState(states.refresh) }
        collectOnStarted(viewModel.preferences) { prefs ->
            adapter.compact = prefs.compactList
            requireActivity().invalidateOptionsMenu()
        }
        collectOnStarted(viewModel.categories) { categories ->
            if (categories != null) updateCategorySpinner(categories)
        }
    }

    private fun setupSearch() {
        if (binding.searchInput.text.isNullOrEmpty() && viewModel.search.value.isNotEmpty()) {
            binding.searchInput.setText(viewModel.search.value)
        }
        binding.searchInput.doAfterTextChanged { viewModel.setSearch(it?.toString().orEmpty()) }
    }

    private fun setupFilterChips() {
        val current = BookFilter.fromCode(viewModel.filter.value)
        chipToFilter.entries.firstOrNull { it.value == current }?.let { binding.filterChips.check(it.key) }
        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = checkedIds.firstOrNull()?.let { chipToFilter[it] } ?: BookFilter.ALL
            viewModel.setFilter(filter)
        }
    }

    private fun setupCategorySpinner() {
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val categoryId = if (position == 0) BookListViewModel.ALL_CATEGORIES
                else spinnerCategories.getOrNull(position - 1)?.id ?: BookListViewModel.ALL_CATEGORIES
                viewModel.setCategory(categoryId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun updateCategorySpinner(categories: List<Category>) {
        spinnerCategories = categories
        val labels = listOf(getString(R.string.all_categories)) + categories.map { it.name }
        binding.categorySpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, labels
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val selectedIndex = categories.indexOfFirst { it.id == viewModel.categoryId.value }
        if (selectedIndex >= 0) {
            binding.categorySpinner.setSelection(selectedIndex + 1, false)
        } else {
            // the selected category was deleted (or "all" was selected)
            binding.categorySpinner.setSelection(0, false)
            viewModel.setCategory(BookListViewModel.ALL_CATEGORIES)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_book_list, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                val current = viewModel.preferences.value.sortOrder
                sortItems.forEach { (id, order) -> menu.findItem(id)?.isChecked = order == current }
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                val order = sortItems[item.itemId] ?: return false
                viewModel.setSortOrder(order)
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun renderLoadState(refresh: LoadState) {
        val isEmpty = refresh is LoadState.NotLoading && adapter.itemCount == 0
        binding.loadingIndicator.isVisible = refresh is LoadState.Loading && adapter.itemCount == 0
        binding.bookRecycler.isVisible = !isEmpty && refresh !is LoadState.Error
        binding.retryButton.isVisible = refresh is LoadState.Error
        binding.emptyView.isVisible = isEmpty || refresh is LoadState.Error
        binding.emptyView.text = when {
            refresh is LoadState.Error -> getString(R.string.error_loading_books)
            viewModel.hasActiveCriteria -> getString(R.string.empty_books_filtered)
            else -> getString(R.string.empty_books)
        }
    }

    override fun onDestroyView() {
        binding.bookRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
