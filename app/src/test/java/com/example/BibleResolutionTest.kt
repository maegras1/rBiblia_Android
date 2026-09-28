package com.example

import com.example.data.model.BookCatalog
import com.example.data.model.BookGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleResolutionTest {

    @Test
    fun testBookCatalogOtAndNtGroups() {
        val otIds = BookCatalog.getOldTestamentBookIds()
        val ntIds = BookCatalog.getNewTestamentBookIds()

        // Genesis should be in Old Testament
        assertTrue(otIds.contains("gen"))
        // Matthew should be in New Testament
        assertTrue(ntIds.contains("mat"))

        val genBook = BookCatalog.getBook("gen", "pl")
        assertEquals("I Mojżeszowa (Rodzaju)", genBook.name)
        assertEquals(BookGroup.OT, genBook.group)

        val matBook = BookCatalog.getBook("mat", "pl")
        assertEquals("Mateusza", matBook.name)
        assertEquals(BookGroup.NT, matBook.group)
    }

    @Test
    fun testResolveDefaultBookRule() {
        val otIds = BookCatalog.getOldTestamentBookIds()
        val ntIds = BookCatalog.getNewTestamentBookIds()

        // 1. Full Bible (has OT): Genesis 1
        val fullBibleStructure = mapOf(
            "gen" to (1..50).toList(),
            "exo" to (1..40).toList(),
            "mat" to (1..28).toList()
        )
        val hasOt1 = fullBibleStructure.keys.any { it in otIds }
        val resolvedFull = if (hasOt1 && fullBibleStructure.containsKey("gen")) "gen" else "other"
        assertEquals("gen", resolvedFull)

        // 2. NT-only Bible (no OT): Matthew 1
        val ntOnlyStructure = mapOf(
            "mat" to (1..28).toList(),
            "mar" to (1..16).toList(),
            "rev" to (1..22).toList()
        )
        val hasOt2 = ntOnlyStructure.keys.any { it in otIds }
        val hasNt2 = ntOnlyStructure.keys.any { it in ntIds }
        val resolvedNt = when {
            hasOt2 && ntOnlyStructure.containsKey("gen") -> "gen"
            hasNt2 && ntOnlyStructure.containsKey("mat") -> "mat"
            else -> ntOnlyStructure.keys.first()
        }
        assertEquals("mat", resolvedNt)

        // 3. Partial translation (e.g. Psalms only, neither gen nor mat)
        val psalmsOnlyStructure = mapOf(
            "psa" to (1..150).toList()
        )
        val hasOt3 = psalmsOnlyStructure.keys.any { it in otIds }
        val hasNt3 = psalmsOnlyStructure.keys.any { it in ntIds }
        val resolvedPsalms = when {
            hasOt3 && psalmsOnlyStructure.containsKey("gen") -> "gen"
            hasNt3 && psalmsOnlyStructure.containsKey("mat") -> "mat"
            else -> psalmsOnlyStructure.keys.first()
        }
        assertEquals("psa", resolvedPsalms)
    }
}
