import UIKit
import XCTest

@testable import ZenCompanion

@MainActor
final class WhatsNewTests: XCTestCase {
    private func makeStore(
        _ preferences: FakePreferences = FakePreferences(),
        version: String = "1.5",
        release: WhatsNewRelease = .latest
    ) -> WhatsNewStore {
        WhatsNewStore(preferences: preferences, currentVersion: version, release: release)
    }

    // MARK: - Once per release

    func testReleaseTargetsVersion15() {
        XCTAssertEqual(WhatsNewRelease.latest.version, "1.5")
    }

    func testPresentsOnceForTheReleaseVersion() {
        let preferences = FakePreferences()
        let store = makeStore(preferences)

        XCTAssertTrue(store.shouldPresent)

        store.markPresented()

        XCTAssertFalse(store.shouldPresent)
        XCTAssertFalse(makeStore(preferences).shouldPresent)
    }

    func testDoesNotPresentBeforeTheReleaseVersion() {
        XCTAssertFalse(makeStore(FakePreferences(), version: "1.4.1").shouldPresent)
        XCTAssertFalse(makeStore(FakePreferences(), version: "1.5.1").shouldPresent)
        XCTAssertTrue(makeStore(FakePreferences(), version: "1.5").shouldPresent)
    }

    func testPresentsAgainForANewerRelease() {
        let preferences = FakePreferences()
        makeStore(preferences, version: "1.5").markPresented()

        XCTAssertFalse(makeStore(preferences, version: "1.5").shouldPresent)
        XCTAssertFalse(makeStore(preferences, version: "1.6").shouldPresent)

        let nextRelease = WhatsNewRelease(version: "1.6", items: WhatsNewRelease.latest.items)
        XCTAssertTrue(makeStore(preferences, version: "1.6", release: nextRelease).shouldPresent)
    }

    func testMarkingTwiceKeepsTheSameVersion() {
        let preferences = FakePreferences()
        let store = makeStore(preferences)

        store.markPresented()
        store.markPresented()

        XCTAssertEqual(
            preferences.string(PreferenceKeys.whatsNewShownVersion, scope: .standard),
            "1.5"
        )
    }

    // MARK: - Release notes model

    func testItemsHaveUniqueIdsAndCompleteCopy() {
        let items = WhatsNewRelease.latest.items

        XCTAssertFalse(items.isEmpty)
        XCTAssertEqual(Set(items.map(\.id)).count, items.count, "item ids must be unique")
        for item in items {
            XCTAssertFalse(item.id.isEmpty)
            XCTAssertFalse(item.systemImage.isEmpty, "\(item.id) needs an icon")
            XCTAssertFalse(item.title.isEmpty, "\(item.id) needs a title")
            XCTAssertFalse(item.detail.isEmpty, "\(item.id) needs a detail")
        }
    }

    func testReleaseCoversTheShareSaveKindAndTabControls() {
        let ids = Set(WhatsNewRelease.latest.items.map(\.id))

        // PR #45 must be listed even while it is still open. Tab controls
        // cover moving pinned tabs into folders and pinning normal tabs.
        XCTAssertTrue(ids.contains("share-save-kind"))
        XCTAssertTrue(ids.contains("tab-controls"))
    }

    func testFolderIconsAndEssentialsLiveInTheMoreItem() {
        let more = WhatsNewRelease.latest.items.first { $0.id == "more" }

        XCTAssertNotNil(more)
        XCTAssertTrue(more?.detail.localizedCaseInsensitiveContains("folder icons") == true)
        XCTAssertTrue(more?.detail.localizedCaseInsensitiveContains("essentials") == true)
        XCTAssertFalse(WhatsNewRelease.latest.items.contains { $0.id == "folder-icons" })
        XCTAssertFalse(WhatsNewRelease.latest.items.contains { $0.id == "essentials-per-row" })
    }

    func testCopyStaysConcise() {
        for item in WhatsNewRelease.latest.items {
            XCTAssertLessThanOrEqual(item.title.count, 48, "\(item.id) title is too long")
            // Two lines at the default text size; Dynamic Type may wrap more.
            XCTAssertLessThanOrEqual(item.detail.count, 74, "\(item.id) detail is too long")
        }
    }

    func testEveryNotificationStyleIconExists() {
        // A missing SF Symbol renders as a blank badge, so pin the names used
        // by the release instead of discovering it visually.
        for item in WhatsNewRelease.latest.items {
            XCTAssertNotNil(
                UIImage(systemName: item.systemImage),
                "\(item.id) uses an unknown SF Symbol: \(item.systemImage)"
            )
        }
    }
}
