package com.estiam.bookshelf.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.estiam.bookshelf.R
import com.estiam.bookshelf.data.prefs.UserPreferences
import com.estiam.bookshelf.databinding.FragmentSettingsBinding
import com.estiam.bookshelf.domain.SortOrder
import com.estiam.bookshelf.domain.ThemeMode
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import kotlin.math.roundToInt

/** Screen 7 - user preferences stored in DataStore (applied reactively everywhere). */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels { AppViewModelProvider.Factory }

    /** Prevents listeners from writing back values that were just set from the DataStore. */
    private var bindingUi = false

    private val themeButtons = mapOf(
        R.id.themeSystem to ThemeMode.SYSTEM,
        R.id.themeLight to ThemeMode.LIGHT,
        R.id.themeDark to ThemeMode.DARK
    )

    private val sortButtons = mapOf(
        R.id.sortTitle to SortOrder.TITLE,
        R.id.sortAuthor to SortOrder.AUTHOR,
        R.id.sortRecent to SortOrder.RECENT,
        R.id.sortRating to SortOrder.RATING,
        R.id.sortProgress to SortOrder.PROGRESS
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)

        binding.themeGroup.setOnCheckedChangeListener { _, id ->
            if (!bindingUi) themeButtons[id]?.let(viewModel::setTheme)
        }
        binding.sortGroup.setOnCheckedChangeListener { _, id ->
            if (!bindingUi) sortButtons[id]?.let(viewModel::setSortOrder)
        }
        binding.compactSwitch.setOnCheckedChangeListener { _, checked ->
            if (!bindingUi) viewModel.setCompactList(checked)
        }
        binding.goalSlider.addOnChangeListener { _, value, fromUser ->
            binding.goalValue.text = getString(R.string.goal_value_format, value.roundToInt())
            if (fromUser && !bindingUi) viewModel.setDailyGoal(value.roundToInt())
        }

        collectOnStarted(viewModel.preferences) { prefs -> if (prefs != null) render(prefs) }
    }

    private fun render(prefs: UserPreferences) {
        bindingUi = true
        themeButtons.entries.first { it.value == prefs.themeMode }.key.let(binding.themeGroup::check)
        sortButtons.entries.first { it.value == prefs.sortOrder }.key.let(binding.sortGroup::check)
        binding.compactSwitch.isChecked = prefs.compactList

        val slider = binding.goalSlider
        val goal = (prefs.dailyGoal / slider.stepSize).roundToInt() * slider.stepSize
        slider.value = goal.coerceIn(slider.valueFrom, slider.valueTo)
        binding.goalValue.text = getString(R.string.goal_value_format, slider.value.roundToInt())
        bindingUi = false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
