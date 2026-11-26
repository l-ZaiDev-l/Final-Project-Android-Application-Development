package com.estiam.bookshelf.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryAndSessionValidatorTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `category name is required, limited and unique ignoring case`() {
        val existing = listOf("Fantasy", "History")
        assertEquals(ValidationError.REQUIRED, CategoryValidator.validate("  ", existing))
        assertEquals(ValidationError.TOO_LONG, CategoryValidator.validate("x".repeat(31), existing))
        assertEquals(ValidationError.DUPLICATE, CategoryValidator.validate(" fantasy ", existing))
        assertNull(CategoryValidator.validate("Poetry", existing))
    }

    @Test
    fun `valid session has no error`() {
        val errors = SessionValidator.validate("25", "30", now - 1000, remainingPages = 100, now = now)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `session cannot have more pages than remaining in the book`() {
        val errors = SessionValidator.validate("120", "30", now, remainingPages = 100, now = now)
        assertEquals(ValidationError.EXCEEDS_REMAINING, errors[SessionField.PAGES])
    }

    @Test
    fun `session date cannot be in the future and minutes must be positive`() {
        val errors = SessionValidator.validate("10", "0", now + 60_000, remainingPages = 100, now = now)
        assertEquals(ValidationError.FUTURE_DATE, errors[SessionField.DATE])
        assertEquals(ValidationError.OUT_OF_RANGE, errors[SessionField.MINUTES])
    }
}
