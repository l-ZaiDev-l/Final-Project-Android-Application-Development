package com.estiam.bookshelf.ui

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.estiam.bookshelf.MainActivity
import com.estiam.bookshelf.R
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Espresso UI tests, run against the real app (real database and DataStore). */
@RunWith(AndroidJUnit4::class)
class BookShelfUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun catalogueIsDisplayedOnLaunch() {
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()))
        onView(withId(R.id.filterChips)).check(matches(isDisplayed()))
        onView(withId(R.id.addBookFab)).check(matches(isDisplayed()))
    }

    @Test
    fun savingEmptyBookFormShowsValidationErrors() {
        onView(withId(R.id.addBookFab)).perform(click())
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        val requiredError = getString(R.string.error_required)
        onView(allOf(withText(requiredError), isDescendantOfA(withId(R.id.titleLayout))))
            .check(matches(isDisplayed()))
        onView(allOf(withText(requiredError), isDescendantOfA(withId(R.id.authorLayout))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun addingValidBookShowsItInTheCatalogue() {
        val title = "Espresso Book ${System.currentTimeMillis()}"

        onView(withId(R.id.addBookFab)).perform(click())
        onView(withId(R.id.titleInput)).perform(replaceText(title))
        onView(withId(R.id.authorInput)).perform(replaceText("Test Author"))
        onView(withId(R.id.totalPagesInput)).perform(replaceText("250"))
        onView(withId(R.id.currentPageInput)).perform(replaceText("50"), closeSoftKeyboard())
        onView(withId(R.id.saveButton)).perform(scrollTo(), click())

        // Back on the catalogue: search the new book (Room + Paging are asynchronous -> wait for it).
        waitForView(withId(R.id.searchInput))
        onView(withId(R.id.searchInput)).perform(replaceText(title), closeSoftKeyboard())
        waitForView(allOf(withId(R.id.titleText), withText(title)))
        onView(allOf(withId(R.id.bookRecycler), hasDescendant(withText(title))))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigationOpensCategoriesStatisticsAndSettings() {
        onView(withId(R.id.categoryListFragment)).perform(click())
        onView(withId(R.id.addCategoryFab)).check(matches(isDisplayed()))

        onView(withId(R.id.statsFragment)).perform(click())
        waitForView(withText(R.string.daily_goal))

        onView(withId(R.id.settingsFragment)).perform(click())
        onView(withId(R.id.compactSwitch)).check(matches(isDisplayed()))
        onView(withId(R.id.themeGroup)).check(matches(isDisplayed()))
    }

    private fun getString(id: Int): String {
        var text = ""
        activityRule.scenario.onActivity { text = it.getString(id) }
        return text
    }

    /** Polls until the view is displayed (Room queries run on background threads Espresso does not track). */
    private fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 5_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                if (System.currentTimeMillis() > end) throw e
                Thread.sleep(100)
            }
        }
    }
}
