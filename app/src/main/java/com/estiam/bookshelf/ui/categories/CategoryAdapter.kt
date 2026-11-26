package com.estiam.bookshelf.ui.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.CategoryWithCount
import com.estiam.bookshelf.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (CategoryWithCount) -> Unit,
    private val onDelete: (CategoryWithCount) -> Unit
) : ListAdapter<CategoryWithCount, CategoryAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CategoryWithCount) {
            b.nameText.text = item.category.name
            b.countText.text = b.root.context.resources.getQuantityString(
                R.plurals.book_count, item.bookCount, item.bookCount
            )
            b.root.setOnClickListener { onEdit(item) }
            b.deleteButton.setOnClickListener { onDelete(item) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<CategoryWithCount>() {
            override fun areItemsTheSame(old: CategoryWithCount, new: CategoryWithCount) =
                old.category.id == new.category.id

            override fun areContentsTheSame(old: CategoryWithCount, new: CategoryWithCount) = old == new
        }
    }
}
