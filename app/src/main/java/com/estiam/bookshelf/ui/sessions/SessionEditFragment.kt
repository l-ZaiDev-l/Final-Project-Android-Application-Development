package com.estiam.bookshelf.ui.sessions

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.estiam.bookshelf.R
import com.estiam.bookshelf.databinding.FragmentSessionEditBinding
import com.estiam.bookshelf.domain.ReadingProgress
import com.estiam.bookshelf.domain.SessionField
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import com.estiam.bookshelf.ui.common.formatDate
import com.estiam.bookshelf.ui.common.message
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import java.util.Calendar
import java.util.TimeZone

/** Screen 4 - log a reading session for a book (advances the book progress). */
class SessionEditFragment : Fragment(R.layout.fragment_session_edit) {

    private var _binding: FragmentSessionEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SessionEditViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSessionEditBinding.bind(view)

        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.saveButton.setOnClickListener {
            viewModel.save(
                pages = binding.pagesInput.text?.toString().orEmpty(),
                minutes = binding.minutesInput.text?.toString().orEmpty(),
                note = binding.noteInput.text?.toString().orEmpty()
            )
        }

        // The picker is a DialogFragment: re-attach the listener after a rotation.
        @Suppress("UNCHECKED_CAST")
        (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>)
            ?.addOnPositiveButtonClickListener(::onDatePicked)

        collectOnStarted(viewModel.book) { book ->
            if (book == null) return@collectOnStarted
            val remaining = ReadingProgress.remainingPages(book)
            binding.bookTitleText.text = book.title
            binding.remainingText.text = getString(R.string.pages_remaining, remaining)
            binding.completedText.isVisible = remaining == 0
            binding.saveButton.isEnabled = remaining > 0
        }
        collectOnStarted(viewModel.date) { binding.dateInput.setText(formatDate(it)) }
        collectOnStarted(viewModel.errors) { errors ->
            val ctx = requireContext()
            binding.pagesLayout.error = errors[SessionField.PAGES]?.message(ctx)
            binding.minutesLayout.error = errors[SessionField.MINUTES]?.message(ctx)
            binding.dateLayout.error = errors[SessionField.DATE]?.message(ctx)
        }
        collectOnStarted(viewModel.events) { event ->
            when (event) {
                SessionEditEvent.Saved ->
                    Toast.makeText(requireContext(), R.string.session_saved, Toast.LENGTH_SHORT).show()
                SessionEditEvent.BookMissing ->
                    Toast.makeText(requireContext(), R.string.book_not_found, Toast.LENGTH_SHORT).show()
            }
            findNavController().navigateUp()
        }
    }

    private fun showDatePicker() {
        if (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) return
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.session_date)
            .setSelection(toUtcMidnight(viewModel.date.value))
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            )
            .build()
        picker.addOnPositiveButtonClickListener(::onDatePicked)
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    /** MaterialDatePicker works in UTC: convert the picked day to the local start of that day. */
    private fun onDatePicked(utcMillis: Long) {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val local = Calendar.getInstance().apply {
            clear()
            set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        }
        val now = System.currentTimeMillis()
        // today -> keep the current time, otherwise the start of the chosen day
        viewModel.setDate(if (local.timeInMillis > now - DAY_MS) now else local.timeInMillis)
    }

    private fun toUtcMidnight(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_BOOK_ID = "bookId"
        private const val DATE_PICKER_TAG = "session_date_picker"
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
