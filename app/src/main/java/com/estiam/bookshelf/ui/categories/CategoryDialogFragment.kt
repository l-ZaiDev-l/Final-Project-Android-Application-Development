package com.estiam.bookshelf.ui.categories

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.Category
import com.estiam.bookshelf.databinding.DialogCategoryBinding
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.message
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Add / rename dialog. A DialogFragment (not a plain AlertDialog) so it survives rotation.
 * It shares the CategoryViewModel of its parent fragment.
 */
class CategoryDialogFragment : DialogFragment() {

    private val viewModel: CategoryViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { AppViewModelProvider.Factory }
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogCategoryBinding.inflate(layoutInflater)
        val categoryId = requireArguments().getLong(ARG_ID)
        if (savedInstanceState == null) {
            binding.nameInput.setText(requireArguments().getString(ARG_NAME).orEmpty())
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (categoryId == 0L) R.string.title_add_category else R.string.title_rename_category)
            .setView(binding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_save, null) // overridden below to keep the dialog open on error
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                lifecycleScope.launch {
                    val error = viewModel.save(categoryId, binding.nameInput.text?.toString().orEmpty())
                    if (error == null) dismiss() else binding.nameLayout.error = error.message(requireContext())
                }
            }
        }
        return dialog
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_NAME = "name"

        fun newInstance(category: Category?) = CategoryDialogFragment().apply {
            arguments = bundleOf(ARG_ID to (category?.id ?: 0L), ARG_NAME to category?.name)
        }
    }
}
