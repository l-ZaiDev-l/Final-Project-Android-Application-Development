package com.estiam.bookshelf.ui.stats

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.estiam.bookshelf.R
import com.estiam.bookshelf.databinding.FragmentStatsBinding
import com.estiam.bookshelf.databinding.ItemCategoryStatBinding
import com.estiam.bookshelf.databinding.ViewStatCardBinding
import com.estiam.bookshelf.domain.ReadingStats
import com.estiam.bookshelf.ui.common.AppViewModelProvider
import com.estiam.bookshelf.ui.common.collectOnStarted
import java.util.Locale

/** Screen 6 - reading statistics, daily goal and streak. */
class StatsFragment : Fragment(R.layout.fragment_stats) {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatsBinding.bind(view)
        collectOnStarted(viewModel.stats) { stats -> if (stats != null) render(stats) }
    }

    private fun render(s: ReadingStats) {
        binding.emptyView.isVisible = s.totalBooks == 0
        binding.content.isVisible = s.totalBooks > 0
        if (s.totalBooks == 0) return

        binding.goalProgress.progress = s.goalPercent
        binding.goalText.text = getString(R.string.goal_format, s.pagesToday, s.dailyGoal)
        binding.streakText.text = resources.getQuantityString(
            R.plurals.streak_days, s.currentStreakDays, s.currentStreakDays
        )

        card(binding.statBooks, s.totalBooks.toString(), R.string.stat_books)
        card(binding.statCompleted, s.completedBooks.toString(), R.string.stat_completed)
        card(binding.statReading, s.inProgressBooks.toString(), R.string.stat_reading)
        card(binding.statFavorites, s.favoriteBooks.toString(), R.string.stat_favorites)
        card(binding.statPages, s.pagesRead.toString(), R.string.stat_pages)
        card(binding.statTime, getString(R.string.duration_format, s.totalMinutes / 60, s.totalMinutes % 60), R.string.stat_time)
        card(binding.statSpeed, s.pagesPerHour.toString(), R.string.stat_speed)
        card(binding.statRating, String.format(Locale.getDefault(), "%.1f", s.averageRating), R.string.stat_rating)

        binding.estimateText.text = when (val days = s.estimatedDaysToFinish) {
            null -> getString(R.string.estimate_unknown)
            else -> resources.getQuantityString(R.plurals.estimate_days, days, s.pagesRemaining, days)
        }

        binding.categoryContainer.removeAllViews()
        val maxPages = s.categoryStats.maxOfOrNull { it.pagesRead }?.coerceAtLeast(1) ?: 1
        s.categoryStats.forEach { stat ->
            val row = ItemCategoryStatBinding.inflate(layoutInflater, binding.categoryContainer, true)
            row.nameText.text = stat.name ?: getString(R.string.uncategorized)
            row.valueText.text = getString(R.string.category_stat_format, stat.bookCount, stat.pagesRead)
            row.barIndicator.progress = stat.pagesRead * 100 / maxPages
        }
    }

    private fun card(card: ViewStatCardBinding, value: String, label: Int) {
        card.statValue.text = value
        card.statLabel.setText(label)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
