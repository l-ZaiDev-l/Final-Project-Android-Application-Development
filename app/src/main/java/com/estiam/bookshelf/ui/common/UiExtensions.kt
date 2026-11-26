package com.estiam.bookshelf.ui.common

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.estiam.bookshelf.R
import com.estiam.bookshelf.domain.ReadingStatus
import com.estiam.bookshelf.domain.ValidationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Collects [flow] only while the fragment view is at least STARTED (lifecycle-safe, rotation-safe). */
fun <T> Fragment.collectOnStarted(flow: Flow<T>, action: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(action)
        }
    }
}

fun ValidationError.message(context: Context): String = context.getString(
    when (this) {
        ValidationError.REQUIRED -> R.string.error_required
        ValidationError.TOO_LONG -> R.string.error_too_long
        ValidationError.NOT_A_NUMBER -> R.string.error_not_a_number
        ValidationError.OUT_OF_RANGE -> R.string.error_out_of_range
        ValidationError.EXCEEDS_TOTAL -> R.string.error_exceeds_total
        ValidationError.EXCEEDS_REMAINING -> R.string.error_exceeds_remaining
        ValidationError.DUPLICATE -> R.string.error_duplicate
        ValidationError.FUTURE_DATE -> R.string.error_future_date
    }
)

fun ReadingStatus.label(context: Context): String = context.getString(
    when (this) {
        ReadingStatus.NOT_STARTED -> R.string.status_not_started
        ReadingStatus.READING -> R.string.status_reading
        ReadingStatus.COMPLETED -> R.string.status_completed
    }
)

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
