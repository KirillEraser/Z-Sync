import Foundation

enum BrowserNavigationPolicy {
    private static let webSchemes: Set<String> = ["http", "https", "about", "data", "blob", "file", "javascript"]

    static func loadsInWebView(_ url: URL) -> Bool {
        guard let scheme = url.scheme?.lowercased() else { return true }
        return webSchemes.contains(scheme)
    }

    /// URL a `target=_blank` / `window.open` request should load in the only tab.
    /// `about:blank` is skipped: Google opens a blank window and then assigns the
    /// real address, and loading the blank request would wipe the current page.
    static func sameTabURL(_ url: URL?) -> URL? {
        guard let url, loadsInWebView(url) else { return nil }
        switch url.scheme?.lowercased() {
        case "http", "https":
            return url
        default:
            return nil
        }
    }

    /// Another app opens only from a link the user tapped in the main page.
    /// Script, redirect and iframe navigations to app schemes are dropped,
    /// matching the Android policy.
    static func opensExternally(userTapped: Bool, isMainFrame: Bool) -> Bool {
        userTapped && isMainFrame
    }
}
