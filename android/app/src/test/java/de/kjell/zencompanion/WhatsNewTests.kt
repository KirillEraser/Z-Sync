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
    fun releaseTargetsVersion15() {
        assertEquals("1.5", WhatsNewRelease.version)
    }

    @Test
    fun presentsOnceForTheReleaseVersion() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.5")

        assertTrue(controller.shouldPresent(isDemo = false))

        controller.markPresented()

        assertFalse(controller.shouldPresent(isDemo = false))
        assertFalse(WhatsNewController(repository, "1.5").shouldPresent(isDemo = false))
    }

    @Test
    fun doesNotPresentBeforeTheReleaseVersion() {
        assertFalse(WhatsNewController(MemoryWhatsNewRepository(), "1.4.1").shouldPresent(isDemo = false))
        assertFalse(WhatsNewController(MemoryWhatsNewRepository(), "1.5.1").shouldPresent(isDemo = false))
        assertTrue(WhatsNewController(MemoryWhatsNewRepository(), "1.5").shouldPresent(isDemo = false))
    }

    @Test
    fun presentsAgainForANewerRelease() {
        val repository = MemoryWhatsNewRepository()
        WhatsNewController(repository, "1.5").markPresented()

        assertFalse(WhatsNewController(repository, "1.5").shouldPresent(isDemo = false))
        assertFalse(WhatsNewController(repository, "1.6").shouldPresent(isDemo = false))
        assertTrue(
            WhatsNewController(repository, "1.6", releaseVersion = "1.6")
                .shouldPresent(isDemo = false),
        )
    }

    @Test
    fun neverMarksTheSheetInDemoMode() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.5")

        assertFalse(controller.shouldPresent(isDemo = true))
        assertNull(repository.version)
    }

    @Test
    fun markingTwiceKeepsTheSameVersion() {
        val repository = MemoryWhatsNewRepository()
        val controller = WhatsNewController(repository, "1.5")

        controller.markPresented()
        controller.markPresented()

        assertEquals("1.5", repository.version)
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
    fun releaseCoversTheShareSaveKindAndTabControls() {
        val ids = WhatsNewRelease.items.map { it.id }.toSet()

        // PR #45 must be listed even while it is still open. Tab controls
        // cover moving pinned tabs into folders and pinning normal tabs.
        assertTrue(ids.contains("share-save-kind"))
        assertTrue(ids.contains("tab-controls"))
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
