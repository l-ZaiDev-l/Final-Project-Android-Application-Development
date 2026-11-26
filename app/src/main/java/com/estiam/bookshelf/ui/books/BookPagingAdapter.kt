package com.estiam.bookshelf.ui.books

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.BookListItem
import com.estiam.bookshelf.databinding.ItemBookBinding
import com.estiam.bookshelf.databinding.ItemBookCompactBinding
import com.estiam.bookshelf.domain.ReadingProgress
import com.estiam.bookshelf.ui.common.label

/**
 * RecyclerView adapter fed by Paging 3. Two layouts: normal and compact
 * (the "compact list" DataStore preference switches between them).
 */
class BookPagingAdapter(
    private val onClick: (BookListItem) -> Unit,
    private val onToggleFavorite: (BookListItem) -> Unit
) : PagingDataAdapter<BookListItem, RecyclerView.ViewHolder>(DIFF) {

    var compact: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun getItemViewType(position: Int) = if (compact) TYPE_COMPACT else TYPE_NORMAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_COMPACT) {
            CompactHolder(ItemBookCompactBinding.inflate(inflater, parent, false))
        } else {
            NormalHolder(ItemBookBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        when (holder) {
            is NormalHolder -> holder.bind(item)
            is CompactHolder -> holder.bind(item)
        }
    }

    private fun bindFavorite(button: ImageButton, item: BookListItem) {
        val fav = item.book.isFavorite
        button.setImageResource(if (fav) R.drawable.ic_star else R.drawable.ic_star_border)
        button.contentDescription = button.context.getString(
            if (fav) R.string.action_remove_favorite else R.string.action_add_favorite
        )
        button.setOnClickListener { onToggleFavorite(item) }
    }

    inner class NormalHolder(private val b: ItemBookBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: BookListItem) {
            val book = item.book
            val ctx = b.root.context
            val percent = ReadingProgress.percent(book)
            b.titleText.text = book.title
            b.authorText.text = book.author
            b.categoryText.text = item.categoryName ?: ctx.getString(R.string.uncategorized)
            b.progressIndicator.progress = percent
            b.progressText.text = ctx.getString(
                R.string.progress_format, book.currentPage, book.totalPages, percent
            )
            b.statusText.text = ReadingProgress.status(book).label(ctx)
            b.ratingText.isVisible = book.rating > 0
            b.ratingText.text = ctx.getString(R.string.rating_format, book.rating)
            bindFavorite(b.favoriteButton, item)
            b.root.setOnClickListener { onClick(item) }
        }
    }

    inner class CompactHolder(private val b: ItemBookCompactBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: BookListItem) {
            val book = item.book
            b.titleText.text = book.title
            b.authorText.text = book.author
            b.percentText.text = b.root.context.getString(R.string.percent_format, ReadingProgress.percent(book))
            bindFavorite(b.favoriteButton, item)
            b.root.setOnClickListener { onClick(item) }
        }
    }

    private companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_COMPACT = 1

        val DIFF = object : DiffUtil.ItemCallback<BookListItem>() {
            override fun areItemsTheSame(old: BookListItem, new: BookListItem) = old.book.id == new.book.id
            override fun areContentsTheSame(old: BookListItem, new: BookListItem) = old == new
        }
    }
}
