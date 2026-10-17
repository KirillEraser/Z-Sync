import SwiftUI

/// How the quick browser is presented.
///
/// Regular width fills the screen. Compact width, and a missing size class,
/// keep the phone sheet (one large detent, visible drag indicator).
enum QuickBrowserPresentation: Equatable {
    case sheet
    case fullScreen

    /// A full-screen cover cannot be dragged away. That presentation adds Close
    /// to the existing bottom toolbar, beside Pin. The phone sheet does not.
    var showsToolbarClose: Bool { self == .fullScreen }
}

enum QuickBrowserPresentationPolicy {
    static func presentation(for horizontalSizeClass: UserInterfaceSizeClass?) -> QuickBrowserPresentation {
        horizontalSizeClass == .regular ? .fullScreen : .sheet
    }
}
