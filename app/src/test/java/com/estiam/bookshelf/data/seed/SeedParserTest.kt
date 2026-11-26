package com.estiam.bookshelf.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedParserTest {

    @Test
    fun `valid records are accepted and trimmed`() {
        val json = """
            { "categories": [" Poetry "],
              "books": [ { "title": "  Leaves of Grass ", "author": "Walt Whitman", "category": "Poetry", "totalPages": 300 } ] }
        """.trimIndent()
        val result = SeedParser.parse(json)
        assertEquals(1, result.books.size)
        assertEquals("Leaves of Grass", result.books[0].title)
        assertEquals(listOf("Poetry"), result.categories)
        assertTrue(result.rejected.isEmpty())
    }

    @Test
    fun `invalid records and duplicates are rejected`() {
        val json = """
            { "books": [
                { "title": "", "author": "Nobody", "totalPages": 10 },
                { "title": "Ok", "author": "Someone", "totalPages": 50, "currentPage": 80 },
                { "title": "Dune", "author": "Frank Herbert", "totalPages": 412 },
                { "title": "DUNE", "author": "frank herbert", "totalPages": 412 }
            ] }
        """.trimIndent()
        val result = SeedParser.parse(json)
        assertEquals(1, result.books.size)
        assertEquals(listOf(0, 1, 3), result.rejected.map { it.index })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `malformed json throws a clear exception`() {
        SeedParser.parse("{ this is not json ")
    }

    @Test
    fun `bundled asset contains at least 20 valid records`() {
        // Unit tests run with the module directory as working directory.
        val file = File("src/main/assets/seed_books.json")
        val result = SeedParser.parse(file.readText())
        assertTrue("expected >= 20 valid books, got ${result.books.size}", result.books.size >= 20)
        assertTrue(result.categories.isNotEmpty())
    }
}
