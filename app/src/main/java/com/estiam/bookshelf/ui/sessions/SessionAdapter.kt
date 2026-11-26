package com.estiam.bookshelf.ui.sessions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.local.ReadingSession
import com.estiam.bookshelf.databinding.ItemSessionBinding
import com.estiam.bookshelf.ui.common.formatDate

class SessionAdapter(
    private val onDelete: (ReadingSession) -> Unit
) : ListAdapter<ReadingSession, SessionAdapter.Holder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(private val b: ItemSessionBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(session: ReadingSession) {
            val ctx = b.root.context
            b.dateText.text = formatDate(session.date)
            b.detailText.text = ctx.getString(R.string.session_format, session.pagesRead, session.durationMinutes)
            b.noteText.isVisible = session.note.isNotBlank()
            b.noteText.text = session.note
            b.deleteButton.setOnClickListener { onDelete(session) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<ReadingSession>() {
            override fun areItemsTheSame(old: ReadingSession, new: ReadingSession) = old.id == new.id
            override fun areContentsTheSame(old: ReadingSession, new: ReadingSession) = old == new
        }
    }
}
