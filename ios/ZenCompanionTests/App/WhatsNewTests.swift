import UIKit
import XCTest

@testable import ZenCompanion

@MainActor
final class WhatsNewTests: XCTestCase {
    private func makeStore(
        _ preferences: FakePreferences = FakePreferences(),
        version: String = "1.4.1",
        release: WhatsNewRelease = .latest
    ) -> WhatsNewStore {
        WhatsNewStore(preferences: preferences, currentVersion: version, release: release)
    }

    // MARK: - Once per version

    func testPresentsOnceForTheCurrentVersion() {
        let preferences = FakePreferences()
        let store = makeStore(preferences)

        XCTAssertTrue(store.shouldPresent)

        store.markPresented()

        XCTAssertFalse(store.shouldPresent)
        XCTAssertFalse(makeStore(preferences).shouldPresent)
    }

    func testPresentsAgainForANewVersion() {
        let preferences = FakePreferences()
        makeStore(preferences, version: "1.4.1").markPresented()

        XCTAssertFalse(makeStore(preferences, version: "1.4.1").shouldPresent)
        XCTAssertTrue(makeStore(preferences, version: "1.5").shouldPresent)
    }

    func testMarkingTwiceKeepsTheSameVersion() {
        let preferences = FakePreferences()
        let store = makeStore(preferences)

        store.markPresented()
        store.markPresented()

        XCTAssertEqual(
            preferences.string(PreferenceKeys.whatsNewShownVersion, scope: .standard),
            "1.4.1"
        )
    }

    func testVersionComparisonIsExact() {
        let preferences = FakePreferences()
        makeStore(preferences, version: "1.4.1").markPresented()

        XCTAssertTrue(makeStore(preferences, version: "1.4.1.1").shouldPresent)
        XCTAssertTrue(makeStore(preferences, version: "1.4").shouldPresent)
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

    func testReleaseCoversTheShareSaveKindAndBrowserErrors() {
        let ids = Set(WhatsNewRelease.latest.items.map(\.id))

        // PR #45 must be listed even while it is still open, and the error
        // panel (#44) is part of this release.
        XCTAssertTrue(ids.contains("share-save-kind"))
        XCTAssertTrue(ids.contains("browser-error-panel"))
    }

    func testReleaseSplitsIntoTwoPagesWithoutLosingItems() {
        let release = WhatsNewRelease.latest
        let pages = release.pages

        XCTAssertEqual(pages.count, 2)
        XCTAssertEqual(pages.flatMap { $0 }, release.items)
        XCTAssertFalse(pages[0].isEmpty)
        XCTAssertFalse(pages[1].isEmpty)
        XCTAssertLessThanOrEqual(abs(pages[0].count - pages[1].count), 1)
    }

    func testEmptyOrSingleItemReleaseStaysOnOnePage() {
        XCTAssertEqual(WhatsNewRelease(items: []).pages.count, 1)
        let only = WhatsNewItem(
            id: "only",
            systemImage: "sparkles",
            title: "Only",
            detail: "Single item"
        )
        XCTAssertEqual(WhatsNewRelease(items: [only]).pages, [[only]])
    }

    func testCopyStaysConcise() {
        for item in WhatsNewRelease.latest.items {
            XCTAssertLessThanOrEqual(item.title.count, 48, "\(item.id) title is too long")
            XCTAssertLessThanOrEqual(item.detail.count, 160, "\(item.id) detail is too long")
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
