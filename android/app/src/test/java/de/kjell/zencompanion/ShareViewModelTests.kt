package de.kjell.zencompanion

import de.kjell.zencompanion.data.SaveKind
import de.kjell.zencompanion.share.SharePhase
import de.kjell.zencompanion.share.ShareRepository
import de.kjell.zencompanion.share.ShareViewModel
import de.kjell.zencompanion.sync.SpacesSyncService
import de.kjell.zencompanion.sync.SyncError
import de.kjell.zencompanion.sync.ZenSpaces
import de.kjell.zencompanion.ui.components.PinDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShareViewModelTests {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun cachedSnapshotStartsInPick() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val vm = ShareViewModel(
            repository = FakeShareRepository(
                signedIn = true,
                cached = ZenSpaces.ZenSnapshot(listOf(space)),
                fresh = ZenSpaces.ZenSnapshot(listOf(space)),
            ),
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )

        assertEquals(SharePhase.pick, vm.state.value.phase)
        assertEquals(listOf(space), vm.state.value.spaces)
        assertEquals(PinDestination(spaceId = "s1"), vm.state.value.destination)
    }

    @Test
    fun emptyFreshSnapshotIsIgnored() = runTest(mainDispatcherRule.testDispatcher) {
        val cachedSpace = testSpace("s1")
        val vm = ShareViewModel(
            repository = FakeShareRepository(
                signedIn = true,
                cached = ZenSpaces.ZenSnapshot(listOf(cachedSpace)),
                fresh = ZenSpaces.ZenSnapshot(emptyList()),
            ),
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )

        assertEquals(listOf(cachedSpace), vm.state.value.spaces)
        assertEquals(SharePhase.pick, vm.state.value.phase)
    }

    @Test
    fun noCacheAndNoSpacesFailsInsteadOfSpinning() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = ShareViewModel(
            repository = FakeShareRepository(
                signedIn = true,
                cached = null,
                fresh = ZenSpaces.ZenSnapshot(emptyList()),
            ),
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )
        runCurrent()

        assertEquals(SharePhase.failed, vm.state.value.phase)
        assertEquals("no spaces", vm.state.value.errorText)
    }

    @Test
    fun refreshAndSecondTapDuringSaveDoNotSaveTwice() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space)),
            fresh = ZenSpaces.ZenSnapshot(listOf(space)),
        )
        val vm = ShareViewModel(repository = repository, initialUrl = "https://example.com", initialPageTitle = "Example")
        runCurrent()

        var phaseAfterRefresh: SharePhase? = null
        repository.duringAddTab = {
            vm.refreshSpaces()
            phaseAfterRefresh = vm.state.value.phase
            vm.save()
        }
        vm.save()
        runCurrent()

        assertEquals(SharePhase.saving, phaseAfterRefresh)
        assertEquals(1, repository.added.size)
        assertEquals(SharePhase.saved, vm.state.value.phase)
        advanceUntilIdle()
    }

    @Test
    fun refreshAfterAFailedSaveKeepsTheError() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space)),
            fresh = ZenSpaces.ZenSnapshot(listOf(space)),
            failure = SyncError.Conflict(),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )
        runCurrent()

        vm.save()
        runCurrent()
        assertEquals(SharePhase.failed, vm.state.value.phase)

        // The cached snapshot shows the picker before the bootstrap refresh
        // returns, so a refresh can land after the write already failed. It
        // must not replace the failure with the picker.
        vm.refreshSpaces()
        runCurrent()

        assertEquals(SharePhase.failed, vm.state.value.phase)
        assertNotNull(vm.state.value.errorText)
        advanceUntilIdle()
    }

    @Test
    fun vanishedFolderFallsBackToSpaceRoot() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val vm = ShareViewModel(
            repository = FakeShareRepository(
                signedIn = true,
                cached = ZenSpaces.ZenSnapshot(listOf(space)),
                fresh = ZenSpaces.ZenSnapshot(listOf(space)),
            ),
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )

        vm.selectDestination(PinDestination(spaceId = "s1", folderId = "gone"))
        vm.refreshSpaces()

        assertEquals(PinDestination(spaceId = "s1"), vm.state.value.destination)
    }

    @Test
    fun saveAddsTabAndEmitsFinishedAfterAutoCloseDelay() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space)),
            fresh = ZenSpaces.ZenSnapshot(listOf(space)),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        val events = mutableListOf<ShareViewModel.Event>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.save()
        assertEquals(SharePhase.saved, vm.state.value.phase)
        assertEquals(1, repository.added.size)
        assertEquals("https://example.com/page", repository.added[0].url)
        assertEquals("Page", repository.added[0].title)
        assertEquals("s1", repository.added[0].spaceId)
        assertEquals("s1", repository.lastSpace)
        assertTrue(events.isEmpty())

        advanceUntilIdle()
        assertEquals(listOf(ShareViewModel.Event.Finished), events)
    }

    @Test
    fun normalSaveIgnoresFolderAndShowsFallbackNotice() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space), emptyMap(), 0L, normalTabsCapability = ZenSpaces.NormalTabsCapability.ENABLED),
            fresh = ZenSpaces.ZenSnapshot(listOf(space), emptyMap(), 0L, normalTabsCapability = ZenSpaces.NormalTabsCapability.ENABLED),
            saveKind = SaveKind.NORMAL,
            fallBackToPinned = true,
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        val events = mutableListOf<ShareViewModel.Event>()
        backgroundScope.launch { vm.events.collect { events += it } }

        assertTrue(vm.state.value.hideFolders)
        vm.selectDestination(PinDestination(spaceId = "s1", folderId = "folder-1"))
        assertEquals(PinDestination(spaceId = "s1"), vm.state.value.destination)

        vm.save()
        assertEquals(SharePhase.saved, vm.state.value.phase)
        assertTrue(vm.state.value.savedAsPinnedFallback)
        assertEquals(1, repository.added.size)
        assertEquals(SaveKind.NORMAL, repository.added[0].kind)
        assertNull(repository.added[0].folderId)

        // The fallback notice stays up for 2.4s before the sheet closes.
        advanceTimeBy(2_399)
        runCurrent()
        assertTrue(events.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(ShareViewModel.Event.Finished), events)
    }

    @Test
    fun saveReplacesUrlShapedTitleWithResolvedPageTitle() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space)),
            fresh = ZenSpaces.ZenSnapshot(listOf(space)),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://pin.example/page",
            initialPageTitle = "https://pin.example/page/",
            titleResolver = { _, _ -> "Pinned Page" },
        )

        assertEquals("Pinned Page", vm.state.value.pageTitle)
        vm.save()
        assertEquals("Pinned Page", repository.added.first().title)
    }

    @Test
    fun saveFallsBackToHostWhenTheOnlyNameIsTheUrl() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = ZenSpaces.ZenSnapshot(listOf(space)),
            fresh = ZenSpaces.ZenSnapshot(listOf(space)),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://pin.example/page",
            initialPageTitle = "https://pin.example/page",
        )

        vm.save()
        assertEquals("pin.example", repository.added.first().title)
    }

    @Test
    fun perShareNormalChoiceSavesNormalAndLeavesTheSavedDefaultAlone() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
            fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
            saveKind = SaveKind.PINNED,
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()

        assertTrue(vm.state.value.allowsNormalTabs)
        vm.selectDestination(PinDestination(spaceId = "s1", folderId = "folder-1"))
        vm.selectSaveKind(SaveKind.NORMAL)

        assertEquals(SaveKind.NORMAL, vm.state.value.saveKind)
        assertEquals(PinDestination(spaceId = "s1"), vm.state.value.destination)

        vm.save()
        runCurrent()

        assertEquals(1, repository.added.size)
        assertEquals(SaveKind.NORMAL, repository.added[0].kind)
        assertNull(repository.added[0].folderId)

        // The next share starts from the persisted setting again.
        val next = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()
        assertEquals(SaveKind.PINNED, next.state.value.saveKind)
        advanceUntilIdle()
    }

    @Test
    fun pinnedOverrideForOneShareDoesNotChangeTheSavedDefault() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
            fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
            saveKind = SaveKind.NORMAL,
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()
        assertEquals(SaveKind.NORMAL, vm.state.value.saveKind)

        vm.selectSaveKind(SaveKind.PINNED)
        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)

        vm.save()
        runCurrent()

        assertEquals(SaveKind.PINNED, repository.added.first().kind)
        val next = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()
        assertEquals(SaveKind.NORMAL, next.state.value.saveKind)
        advanceUntilIdle()
    }

    /** Without a proven normal-tabs capability the sheet must not offer or
     * write normal records, even if the stored default says normal. */
    @Test
    fun capabilityDisabledForcesPinnedAndHidesTheChoice() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = testSnapshot(space, ZenSpaces.NormalTabsCapability.DISABLED),
            fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.DISABLED),
            saveKind = SaveKind.NORMAL,
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()

        assertFalse(vm.state.value.allowsNormalTabs)
        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)

        vm.selectSaveKind(SaveKind.NORMAL)
        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)

        vm.save()
        runCurrent()
        assertEquals(SaveKind.PINNED, repository.added.first().kind)
        advanceUntilIdle()
    }

    /** A capability that arrives with the fresh snapshot applies the saved
     * default, but never replaces a choice the user already made. */
    @Test
    fun freshCapabilityAppliesTheSavedDefaultUnlessTheUserChose() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = testSnapshot(space, ZenSpaces.NormalTabsCapability.ABSENT),
            fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.ABSENT),
            saveKind = SaveKind.NORMAL,
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()
        assertFalse(vm.state.value.allowsNormalTabs)
        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)

        repository.fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED)
        vm.refreshSpaces()
        runCurrent()

        assertTrue(vm.state.value.allowsNormalTabs)
        assertEquals(SaveKind.NORMAL, vm.state.value.saveKind)

        vm.selectSaveKind(SaveKind.PINNED)
        vm.refreshSpaces()
        runCurrent()

        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)
        advanceUntilIdle()
    }

    /** A capability downgrade must not throw away a folder the user picked
     * while normal tabs were still allowed. */
    @Test
    fun capabilityDowngradeKeepsThePickedFolder() = runTest(mainDispatcherRule.testDispatcher) {
        val space = testSpace("s1", folderId = "folder-1")
        val repository = FakeShareRepository(
            signedIn = true,
            cached = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
            fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.ENABLED),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com/page",
            initialPageTitle = "Page",
        )
        runCurrent()

        vm.selectDestination(PinDestination(spaceId = "s1", folderId = "folder-1"))
        assertEquals("folder-1", vm.state.value.destination?.folderId)

        repository.fresh = testSnapshot(space, ZenSpaces.NormalTabsCapability.DISABLED)
        vm.refreshSpaces()
        runCurrent()

        assertFalse(vm.state.value.allowsNormalTabs)
        assertEquals(SaveKind.PINNED, vm.state.value.saveKind)
        assertEquals(
            PinDestination(spaceId = "s1", folderId = "folder-1"),
            vm.state.value.destination,
        )
        advanceUntilIdle()
    }

    @Test
    fun saveWithNoSpacesFailsWithoutRepositoryWrite() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeShareRepository(
            signedIn = true,
            cached = null,
            fresh = ZenSpaces.ZenSnapshot(emptyList()),
        )
        val vm = ShareViewModel(
            repository = repository,
            initialUrl = "https://example.com",
            initialPageTitle = "Example",
        )

        vm.save()
        advanceUntilIdle()

        assertEquals(SharePhase.failed, vm.state.value.phase)
        assertEquals("no spaces", vm.state.value.errorText)
        assertTrue(repository.added.isEmpty())
        assertNull(repository.lastSpace)
    }
}

private fun testSpace(id: String, folderId: String? = null): ZenSpaces.ZenSpace = ZenSpaces.ZenSpace(
    id = id,
    name = id,
    icon = null,
    containerGuid = null,
    theme = null,
    pinned = folderId?.let {
        listOf(
            ZenSpaces.ZenItem.Folder(
                ZenSpaces.ZenFolder(
                    id = it,
                    name = it,
                    icon = null,
                    tabs = emptyList(),
                ),
            ),
        )
    } ?: emptyList(),
)

private fun testSnapshot(
    space: ZenSpaces.ZenSpace,
    capability: ZenSpaces.NormalTabsCapability,
): ZenSpaces.ZenSnapshot = ZenSpaces.ZenSnapshot(
    spaces = listOf(space),
    essentials = emptyMap(),
    fetchedAtMillis = 0L,
    normalTabsCapability = capability,
)

private class FakeShareRepository(
    private val signedIn: Boolean,
    private val cached: ZenSpaces.ZenSnapshot?,
    var fresh: ZenSpaces.ZenSnapshot,
    private val saveKind: SaveKind = SaveKind.PINNED,
    private val fallBackToPinned: Boolean = false,
    /** When set, [addTab] fails instead of saving. */
    private val failure: Exception? = null,
) : ShareRepository {
    data class AddedTab(
        val url: String,
        val title: String,
        val spaceId: String,
        val folderId: String?,
        val kind: SaveKind,
    )

    val added = mutableListOf<AddedTab>()
    var lastSpace: String? = null

    /** Runs inside [addTab], before it returns, to simulate work landing mid-save. */
    var duringAddTab: (suspend () -> Unit)? = null

    override fun isSignedIn(): Boolean = signedIn

    override val cachedSnapshot: ZenSpaces.ZenSnapshot?
        get() = cached

    override fun lastSpaceId(): String? = lastSpace

    override fun setLastSpaceId(id: String?) {
        lastSpace = id
    }

    override suspend fun refresh(): ZenSpaces.ZenSnapshot = fresh

    override suspend fun addTab(
        url: String,
        title: String,
        spaceId: String,
        folderId: String?,
        kind: SaveKind,
    ): SpacesSyncService.AddTabOutcome {
        failure?.let { throw it }
        added += AddedTab(url, title, spaceId, folderId, kind)
        duringAddTab?.let { hook ->
            duringAddTab = null
            hook()
        }
        val effective = if (fallBackToPinned) SaveKind.PINNED else kind
        return SpacesSyncService.AddTabOutcome(
            recordId = "tab-${added.size}",
            kind = effective,
            fellBackToPinned = fallBackToPinned,
        )
    }

    override fun errorText(error: Exception): String = error.message ?: "error"

    override fun noSpacesErrorText(): String = "no spaces"

    override fun saveKind(): SaveKind = saveKind
}
