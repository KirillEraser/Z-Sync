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
/// user-visible changes, in the order they should be read. The version is the
/// store release these notes belong to, not the build currently running.
struct WhatsNewRelease: Equatable {
    let version: String
    let items: [WhatsNewItem]

    static let latest = WhatsNewRelease(version: "1.5", items: [
        WhatsNewItem(
            id: "share-save-kind",
            systemImage: "square.and.arrow.up",
            title: "Choose pinned or normal when sharing",
            detail: "Pick pinned or normal for each link you share. Default unchanged."
        ),
        WhatsNewItem(
            id: "tab-controls",
            systemImage: "rectangle.on.rectangle",
            title: "More control over tabs",
            detail: "Move pinned tabs into folders or pin normal tabs from their context menu."
        ),
        WhatsNewItem(
            id: "open-into-search",
            systemImage: "magnifyingglass",
            title: "Open straight into search",
            detail: "Start a fresh search each time you open the app."
        ),
        WhatsNewItem(
            id: "more",
            systemImage: "sparkles",
            title: "Plus a few more things",
            detail: "Folder icons, essentials per row, and many smaller fixes."
        )
    ])
}

/// Remembers the release whose notes were already shown. The release version
/// is the key, so the notes appear once when the app reaches 1.5 and are not
/// repeated on later launches.
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

    /// True only once the running app has reached the release these notes
    /// describe, and until they have been shown for it. A fresh 1.5 install
    /// also returns true, so the first signed-in launch introduces the
    /// release; that is safer than guessing whether the app was updated.
    var shouldPresent: Bool {
        currentVersion == release.version
            && preferences.string(PreferenceKeys.whatsNewShownVersion, scope: .standard) != release.version
    }

    /// Call when the sheet is presented, not when it is dismissed: if the app
    /// is killed while the sheet is up, it must not reappear on every launch.
    func markPresented() {
        preferences.setString(release.version, PreferenceKeys.whatsNewShownVersion, scope: .standard)
    }
}
