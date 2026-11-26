package com.estiam.bookshelf.domain

/** Validation errors; the UI maps them to localized string resources. */
enum class ValidationError {
    REQUIRED, TOO_LONG, NOT_A_NUMBER, OUT_OF_RANGE, EXCEEDS_TOTAL, EXCEEDS_REMAINING, DUPLICATE, FUTURE_DATE
}

enum class BookField { TITLE, AUTHOR, TOTAL_PAGES, CURRENT_PAGE, RATING }

enum class SessionField { PAGES, MINUTES, DATE }

object BookValidator {
    const val MAX_TITLE = 120
    const val MAX_AUTHOR = 80
    const val MAX_PAGES = 10_000

    /**
     * Validates the raw text of the book form (also used for JSON seed records).
     * Returns an empty map when everything is valid.
     */
    fun validate(
        title: String,
        author: String,
        totalPages: String,
        currentPage: String,
        rating: Int
    ): Map<BookField, ValidationError> {
        val errors = mutableMapOf<BookField, ValidationError>()

        when {
            title.isBlank() -> errors[BookField.TITLE] = ValidationError.REQUIRED
            title.trim().length > MAX_TITLE -> errors[BookField.TITLE] = ValidationError.TOO_LONG
        }
        when {
            author.isBlank() -> errors[BookField.AUTHOR] = ValidationError.REQUIRED
            author.trim().length > MAX_AUTHOR -> errors[BookField.AUTHOR] = ValidationError.TOO_LONG
        }

        val total = totalPages.trim().toIntOrNull()
        when {
            totalPages.isBlank() -> errors[BookField.TOTAL_PAGES] = ValidationError.REQUIRED
            total == null -> errors[BookField.TOTAL_PAGES] = ValidationError.NOT_A_NUMBER
            total !in 1..MAX_PAGES -> errors[BookField.TOTAL_PAGES] = ValidationError.OUT_OF_RANGE
        }

        // An empty "current page" means "not started yet".
        val current = if (currentPage.isBlank()) 0 else currentPage.trim().toIntOrNull()
        when {
            current == null -> errors[BookField.CURRENT_PAGE] = ValidationError.NOT_A_NUMBER
            current < 0 -> errors[BookField.CURRENT_PAGE] = ValidationError.OUT_OF_RANGE
            total != null && total in 1..MAX_PAGES && current > total ->
                errors[BookField.CURRENT_PAGE] = ValidationError.EXCEEDS_TOTAL
        }

        if (rating !in 0..5) errors[BookField.RATING] = ValidationError.OUT_OF_RANGE
        return errors
    }
}

object CategoryValidator {
    const val MAX_NAME = 30

    /**
     * @param existingNames names of the other categories (the one being renamed must be excluded).
     */
    fun validate(name: String, existingNames: Collection<String>): ValidationError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationError.REQUIRED
            trimmed.length > MAX_NAME -> ValidationError.TOO_LONG
            existingNames.any { it.trim().equals(trimmed, ignoreCase = true) } -> ValidationError.DUPLICATE
            else -> null
        }
    }
}

object SessionValidator {
    const val MAX_MINUTES = 24 * 60

    fun validate(
        pages: String,
        minutes: String,
        dateMillis: Long,
        remainingPages: Int,
        now: Long
    ): Map<SessionField, ValidationError> {
        val errors = mutableMapOf<SessionField, ValidationError>()

        val p = pages.trim().toIntOrNull()
        when {
            pages.isBlank() -> errors[SessionField.PAGES] = ValidationError.REQUIRED
            p == null -> errors[SessionField.PAGES] = ValidationError.NOT_A_NUMBER
            p < 1 -> errors[SessionField.PAGES] = ValidationError.OUT_OF_RANGE
            p > remainingPages -> errors[SessionField.PAGES] = ValidationError.EXCEEDS_REMAINING
        }

        val m = minutes.trim().toIntOrNull()
        when {
            minutes.isBlank() -> errors[SessionField.MINUTES] = ValidationError.REQUIRED
            m == null -> errors[SessionField.MINUTES] = ValidationError.NOT_A_NUMBER
            m !in 1..MAX_MINUTES -> errors[SessionField.MINUTES] = ValidationError.OUT_OF_RANGE
        }

        if (dateMillis > now) errors[SessionField.DATE] = ValidationError.FUTURE_DATE
        return errors
    }
}
