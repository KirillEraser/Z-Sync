import UIKit
import WebKit
import XCTest

@testable import ZenCompanion

/// The keyboard accessory has to have a real size: an input accessory is laid
/// out from its frame, so a zero frame means the bar silently disappears.
@MainActor
final class WebKeyboardAccessoryTests: XCTestCase {
    func testAccessoryHasAHeightAndItsGlassBar() {
        let accessory = WebKeyboardAccessory(webView: WKWebView(frame: .zero))

        XCTAssertEqual(accessory.frame.height, 56)
        XCTAssertEqual(accessory.sizeThatFits(CGSize(width: 390, height: 0)).height, 56)
        XCTAssertEqual(accessory.intrinsicContentSize.height, 56)
        XCTAssertTrue(
            accessory.subviews.contains { $0 is UIVisualEffectView },
            "the bar's own surface is missing"
        )
    }
}
