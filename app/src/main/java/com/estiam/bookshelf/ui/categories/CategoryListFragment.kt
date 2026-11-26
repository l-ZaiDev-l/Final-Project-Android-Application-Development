package com.estiam.bookshelf.ui.categories

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.CategoryWithCount
import com.estiam.bookshelf.databinding.FragmentCategoryListBinding
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Screen 5 - category management (create / rename / delete). */
class CategoryListFragment : Fragment(R.layout.fragment_category_list) {

    private var _binding: FragmentCategoryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentCategoryListBinding.bind(view)

        val adapter = CategoryAdapter(
            onEdit = { CategoryDialogFragment.newInstance(it.category).show(childFragmentManager, DIALOG_TAG) },
            onDelete = ::confirmDelete
        )
        binding.categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.categoryRecycler.adapter = adapter

        binding.addCategoryFab.setOnClickListener {
            CategoryDialogFragment.newInstance(null).show(childFragmentManager, DIALOG_TAG)
        }

        collectOnStarted(viewModel.categories) { list ->
            if (list == null) return@collectOnStarted
            adapter.submitList(list)
            binding.emptyView.isVisible = list.isEmpty()
        }
    }

    private fun confirmDelete(item: CategoryWithCount) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_category_title, item.category.name))
            .setMessage(
                resources.getQuantityString(R.plurals.delete_category_message, item.bookCount, item.bookCount)
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ -> viewModel.delete(item.category) }
            .show()
    }

    override fun onDestroyView() {
        binding.categoryRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DIALOG_TAG = "category_dialog"
    }
}
