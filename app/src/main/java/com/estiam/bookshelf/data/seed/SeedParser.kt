package com.estiam.bookshelf.data.seed

import com.estiam.bookshelf.domain.BookValidator
import com.google.gson.Gson
import com.google.gson.JsonParseException

/** Raw structure of assets/seed_books.json. Every field is nullable because the file is untrusted. */
data class SeedFile(
    val version: Int? = null,
    val categories: List<String>? = null,
    val books: List<SeedBook>? = null
)

data class SeedBook(
    val title: String? = null,
    val author: String? = null,
    val category: String? = null,
    val totalPages: Int? = null,
    val currentPage: Int? = null,
    val rating: Int? = null,
    val favorite: Boolean? = null,
    val description: String? = null
)

data class RejectedRecord(val index: Int, val reason: String)

data class SeedParseResult(
    val categories: List<String>,
    val books: List<SeedBook>,
    val rejected: List<RejectedRecord>
)

/**
 * Parses and validates the seed JSON. Pure JVM code (Gson) so it can be unit-tested.
 * Invalid records and in-file duplicates are rejected, valid ones are trimmed.
 */
object SeedParser {

    private val gson = Gson()

    fun parse(json: String): SeedParseResult {
        val file = try {
            gson.fromJson(json, SeedFile::class.java)
        } catch (e: JsonParseException) {
            throw IllegalArgumentException("Malformed seed JSON: ${e.message}", e)
        } ?: SeedFile()

        val rejected = mutableListOf<RejectedRecord>()
        val accepted = mutableListOf<SeedBook>()
        val seenKeys = mutableSetOf<String>()

        file.books.orEmpty().forEachIndexed { index, raw ->
            val reason = validationError(raw)
            if (reason != null) {
                rejected += RejectedRecord(index, reason)
                return@forEachIndexed
            }
            val book = raw.copy(
                title = raw.title!!.trim(),
                author = raw.author!!.trim(),
                category = raw.category?.trim()?.takeIf { it.isNotEmpty() },
                description = raw.description?.trim().orEmpty()
            )
            val key = "${book.title!!.lowercase()}|${book.author!!.lowercase()}"
            if (!seenKeys.add(key)) {
                rejected += RejectedRecord(index, "duplicate of a previous record")
            } else {
                accepted += book
            }
        }

        val categories = (file.categories.orEmpty() + accepted.mapNotNull { it.category })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

        return SeedParseResult(categories, accepted, rejected)
    }

    /** Returns a human readable reason, or null when the record is valid. */
    fun validationError(book: SeedBook): String? {
        val errors = BookValidator.validate(
            title = book.title.orEmpty(),
            author = book.author.orEmpty(),
            totalPages = book.totalPages?.toString().orEmpty(),
            currentPage = (book.currentPage ?: 0).toString(),
            rating = book.rating ?: 0
        )
        return if (errors.isEmpty()) null
        else errors.entries.joinToString { (field, error) -> "$field: $error" }
    }
}
