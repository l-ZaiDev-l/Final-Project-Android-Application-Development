package com.estiam.bookshelf.ui.books

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.BookWithDetails
import com.estiam.bookshelf.databinding.FragmentBookDetailBinding
import com.estiam.bookshelf.domain.ReadingProgress
import com.estiam.bookshelf.domain.ReadingStatus
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import com.estiam.bookshelf.ui.common.formatDate
import com.estiam.bookshelf.ui.common.label
import com.estiam.bookshelf.ui.sessions.SessionAdapter
import com.estiam.bookshelf.ui.sessions.SessionEditFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Screen 2 - details of one book with its reading sessions. */
class BookDetailFragment : Fragment(R.layout.fragment_book_detail) {

    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookDetailViewModel by viewModels { AppViewModelProvider.Factory }
    private lateinit var sessionAdapter: SessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentBookDetailBinding.bind(view)

        sessionAdapter = SessionAdapter { session ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_session_title)
                .setMessage(getString(R.string.delete_session_message, session.pagesRead))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteSession(session) }
                .show()
        }
        binding.sessionRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.sessionRecycler.adapter = sessionAdapter

        binding.favoriteButton.setOnClickListener { viewModel.toggleFavorite() }
        binding.logSessionFab.setOnClickListener {
            findNavController().navigate(
                R.id.action_bookDetail_to_sessionEdit,
                bundleOf(SessionEditFragment.ARG_BOOK_ID to viewModel.bookId)
            )
        }

        setupMenu()

        collectOnStarted(viewModel.state) { state ->
            when (state) {
                BookDetailState.Loading -> binding.content.isVisible = false
                BookDetailState.NotFound -> closeScreen()
                is BookDetailState.Loaded -> render(state.details)
            }
        }
    }

    private fun render(details: BookWithDetails) {
        val book = details.book
        val ctx = requireContext()
        val percent = ReadingProgress.percent(book)
        val status = ReadingProgress.status(book)

        binding.content.isVisible = true
        binding.titleText.text = book.title
        binding.authorText.text = book.author
        binding.categoryText.text = details.category?.name ?: getString(R.string.uncategorized)
        binding.statusChip.text = status.label(ctx)
        binding.ratingBar.rating = book.rating.toFloat()
        binding.ratingBar.isVisible = book.rating > 0
        binding.progressIndicator.progress = percent
        binding.progressText.text = getString(R.string.progress_format, book.currentPage, book.totalPages, percent)
        binding.remainingText.text = when (status) {
            ReadingStatus.COMPLETED -> getString(
                R.string.completed_on, formatDate(book.completedAt ?: System.currentTimeMillis())
            )
            else -> getString(R.string.pages_remaining, ReadingProgress.remainingPages(book))
        }
        binding.descriptionText.isVisible = book.description.isNotBlank()
        binding.descriptionText.text = book.description

        binding.favoriteButton.text = getString(
            if (book.isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite
        )
        binding.favoriteButton.setIconResource(if (book.isFavorite) R.drawable.ic_star else R.drawable.ic_star_border)

        binding.logSessionFab.isVisible = status != ReadingStatus.COMPLETED
        sessionAdapter.submitList(details.sessions)
        binding.sessionEmptyText.isVisible = details.sessions.isEmpty()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_book_detail, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(
                        R.id.action_bookDetail_to_bookEdit,
                        bundleOf(BookEditFragment.ARG_BOOK_ID to viewModel.bookId)
                    )
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_book_title)
            .setMessage(R.string.delete_book_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.deleteBook() }
            .show()
    }

    private fun closeScreen() {
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.bookDetailFragment) {
            Toast.makeText(requireContext(), R.string.book_not_found, Toast.LENGTH_SHORT).show()
            nav.navigateUp()
        }
    }

    override fun onDestroyView() {
        binding.sessionRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
    }
}
