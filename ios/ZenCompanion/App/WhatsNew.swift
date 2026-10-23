import Foundation

/// One user-facing change in a release. Copy lives here rather than in the
/// String Catalog because release notes are content, not chrome: they are
/// replaced wholesale each release and stay English-only like the rest of the
/// app.
struct WhatsNewItem: Identifiable, Hashable {
    let id: String
    /// SF Symbol shown in the tinted badge.
    let systemImage: String
    let title: String
    let detail: String
}

/// The notes shown by the What's New sheet. Keep this to genuinely
/// user-visible changes, in the order they should be read.
struct WhatsNewRelease: Equatable {
    let items: [WhatsNewItem]

    static let latest = WhatsNewRelease(items: [
        WhatsNewItem(
            id: "share-save-kind",
            systemImage: "square.and.arrow.up",
            title: "Choose pin or normal when sharing",
            detail: "A new Save as menu in the share sheet saves each link as a pinned or normal tab, without changing your default."
        ),
        WhatsNewItem(
            id: "tab-controls",
            systemImage: "contextualmenu.and.cursorarrow",
            title: "More control over tabs",
            detail: "Move pinned tabs into folders from the tab menu, or pin a normal tab without opening it."
        ),
        WhatsNewItem(
            id: "open-into-search",
            systemImage: "magnifyingglass",
            title: "Open straight into search",
            detail: "Turn on Open into search to open a fresh search tab whenever you come back to the app."
        ),
        WhatsNewItem(
            id: "folder-icons",
            systemImage: "folder",
            title: "See folder icons",
            detail: "Synced folder icons now show inside the folder flap, like on the desktop."
        ),
        WhatsNewItem(
            id: "essentials-per-row",
            systemImage: "square.grid.2x2",
            title: "Set essentials per row",
            detail: "Pick how many essentials fit on a row under Advanced."
        ),
        WhatsNewItem(
            id: "more-fixes",
            systemImage: "wrench.and.screwdriver",
            title: "Plus many more fixes",
            detail: "Smaller bug fixes and polish across the browser, sharing, and sync."
        )
    ])

    /// Balanced split into at most two pages, so the sheet reads as a short
    /// two-step update instead of one long scroll. With the current seven
    /// items that is 4 + 3.
    var pages: [[WhatsNewItem]] {
        guard items.count > 1 else { return [items] }
        let midpoint = (items.count + 1) / 2
        return [Array(items.prefix(midpoint)), Array(items.dropFirst(midpoint))]
    }
}

/// Remembers the app version whose notes were already shown. The current app
/// version is the key, so a new store release presents the sheet once more
/// without any extra bookkeeping.
@MainActor
struct WhatsNewStore {
    private let preferences: PreferencesStoring
    private let currentVersion: String
    let release: WhatsNewRelease

    init(
        preferences: PreferencesStoring = AppServices.preferences,
        currentVersion: String = WhatsNewStore.bundleVersion,
        release: WhatsNewRelease = .latest
    ) {
        self.preferences = preferences
        self.currentVersion = currentVersion
        self.release = release
    }

    nonisolated static var bundleVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"
    }

    /// True until the notes have been shown for this app version. A fresh
    /// install also returns true, so the first signed-in launch introduces the
    /// release; that is safer than guessing whether the app was updated.
    var shouldPresent: Bool {
        preferences.string(PreferenceKeys.whatsNewShownVersion, scope: .standard) != currentVersion
    }

    /// Call when the sheet is presented, not when it is dismissed: if the app
    /// is killed while the sheet is up, it must not reappear on every launch.
    func markPresented() {
        preferences.setString(currentVersion, PreferenceKeys.whatsNewShownVersion, scope: .standard)
    }
}
