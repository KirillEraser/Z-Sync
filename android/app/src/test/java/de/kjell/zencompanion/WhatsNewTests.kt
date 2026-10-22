package de.kjell.zencompanion

import de.kjell.zencompanion.data.WhatsNewController
import de.kjell.zencompanion.data.WhatsNewIcon
import de.kjell.zencompanion.data.WhatsNewRelease
import de.kjell.zencompanion.data.WhatsNewRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewTests {
    private class MemoryWhatsNewRepository(var version: String? = null) : WhatsNewRepository {
        override fun shownVersion(): String? = version

        override fun setShownVersion(version: String) {
            this.version = version
        }
    }

    @Test
    fun presentsOnceForTheCurrentVersion() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.4.1")

        assertTrue(controller.shouldPresent(isDemo = false))

        controller.markPresented()

        assertFalse(controller.shouldPresent(isDemo = false))
        assertFalse(WhatsNewController(repository, "1.4.1").shouldPresent(isDemo = false))
    }

    @Test
    fun presentsAgainForANewVersion() {
        val repository = MemoryWhatsNewRepository()
        WhatsNewController(repository, "1.4.1").markPresented()

        assertFalse(WhatsNewController(repository, "1.4.1").shouldPresent(isDemo = false))
        assertTrue(WhatsNewController(repository, "1.5").shouldPresent(isDemo = false))
    }

    @Test
    fun neverMarksTheSheetInDemoMode() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.4.1")

        assertFalse(controller.shouldPresent(isDemo = true))
        assertNull(repository.version)
    }

    @Test
    fun markingTwiceKeepsTheSameVersion() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.4.1")

        controller.markPresented()
        controller.markPresented()

        assertEquals("1.4.1", repository.version)
    }

    @Test
    fun versionComparisonIsExact() {
        val repository = MemoryWhatsNewRepository()
        WhatsNewController(repository, "1.4.1").markPresented()

        assertTrue(WhatsNewController(repository, "1.4.1.1").shouldPresent(isDemo = false))
        assertTrue(WhatsNewController(repository, "1.4").shouldPresent(isDemo = false))
    }

    @Test
    fun releaseItemsHaveUniqueIdsAndResources() {
        val items = WhatsNewRelease.items

        assertTrue(items.isNotEmpty())
        assertEquals(items.size, items.map { it.id }.toSet().size)
        items.forEach { item ->
            assertTrue(item.id.isNotBlank())
            assertTrue("${item.id} needs a title", item.titleRes != 0)
            assertTrue("${item.id} needs a detail", item.detailRes != 0)
        }
    }

    @Test
    fun releaseCoversTheShareSaveKindAndBrowserErrors() {
        val ids = WhatsNewRelease.items.map { it.id }.toSet()

        // PR #45 must be listed even while it is still open, and the error
        // panel (#44) is part of this release.
        assertTrue(ids.contains("share-save-kind"))
        assertTrue(ids.contains("browser-error-panel"))
    }

    @Test
    fun everyIconIsMapped() {
        // The sheet maps every enum case to a Material icon; a new case must
        // not silently render as a blank badge.
        assertEquals(
            WhatsNewIcon.entries.toSet(),
            WhatsNewRelease.items.map { it.icon }.toSet(),
        )
    }
}
