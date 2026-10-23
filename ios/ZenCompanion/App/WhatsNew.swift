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
            detail: "Pick pin or normal per share, without changing your default."
        ),
        WhatsNewItem(
            id: "tab-controls",
            systemImage: "rectangle.on.rectangle",
            title: "More control over tabs",
            detail: "Move pinned tabs into folders or pin normal tabs from the menu."
        ),
        WhatsNewItem(
            id: "open-into-search",
            systemImage: "magnifyingglass",
            title: "Open straight into search",
            detail: "Open a fresh search tab whenever you come back to the app."
        ),
        WhatsNewItem(
            id: "more",
            systemImage: "sparkles",
            title: "Plus a few more things",
            detail: "Folder icons, an essentials-per-row option, and many smaller fixes."
        )
    ])
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
