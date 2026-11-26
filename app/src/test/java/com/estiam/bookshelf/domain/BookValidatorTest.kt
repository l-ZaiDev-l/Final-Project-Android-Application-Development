package com.estiam.bookshelf.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookValidatorTest {

    private fun validate(
        title: String = "Dune",
        author: String = "Frank Herbert",
        total: String = "412",
        current: String = "0",
        rating: Int = 0
    ) = BookValidator.validate(title, author, total, current, rating)

    @Test
    fun `valid book has no error`() {
        assertTrue(validate().isEmpty())
    }

    @Test
    fun `blank title and author are required`() {
        val errors = validate(title = "   ", author = "")
        assertEquals(ValidationError.REQUIRED, errors[BookField.TITLE])
        assertEquals(ValidationError.REQUIRED, errors[BookField.AUTHOR])
    }

    @Test
    fun `total pages must be a number between 1 and 10000`() {
        assertEquals(ValidationError.NOT_A_NUMBER, validate(total = "abc")[BookField.TOTAL_PAGES])
        assertEquals(ValidationError.OUT_OF_RANGE, validate(total = "0")[BookField.TOTAL_PAGES])
        assertEquals(ValidationError.OUT_OF_RANGE, validate(total = "10001")[BookField.TOTAL_PAGES])
        assertEquals(ValidationError.REQUIRED, validate(total = "")[BookField.TOTAL_PAGES])
    }

    @Test
    fun `current page cannot exceed total pages`() {
        assertEquals(ValidationError.EXCEEDS_TOTAL, validate(total = "100", current = "101")[BookField.CURRENT_PAGE])
        assertNull(validate(total = "100", current = "100")[BookField.CURRENT_PAGE])
    }

    @Test
    fun `empty current page means not started`() {
        assertTrue(validate(current = "").isEmpty())
    }

    @Test
    fun `rating must be between 0 and 5`() {
        assertEquals(ValidationError.OUT_OF_RANGE, validate(rating = 6)[BookField.RATING])
        assertNull(validate(rating = 5)[BookField.RATING])
    }

    @Test
    fun `title longer than the limit is rejected`() {
        val longTitle = "x".repeat(BookValidator.MAX_TITLE + 1)
        assertEquals(ValidationError.TOO_LONG, validate(title = longTitle)[BookField.TITLE])
    }
}
