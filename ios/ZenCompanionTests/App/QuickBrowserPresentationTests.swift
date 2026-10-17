import SwiftUI
import XCTest

@testable import ZenCompanion

final class QuickBrowserPresentationTests: XCTestCase {
    func testRegularWidthFillsTheScreen() {
        let presentation = QuickBrowserPresentationPolicy.presentation(for: .regular)
        XCTAssertEqual(presentation, .fullScreen)
        XCTAssertTrue(presentation.showsToolbarClose)
    }

    func testCompactWidthKeepsTheSheet() {
        let presentation = QuickBrowserPresentationPolicy.presentation(for: .compact)
        XCTAssertEqual(presentation, .sheet)
        XCTAssertFalse(presentation.showsToolbarClose)
    }

    func testMissingSizeClassKeepsTheSheet() {
        let presentation = QuickBrowserPresentationPolicy.presentation(for: nil)
        XCTAssertEqual(presentation, .sheet)
        XCTAssertFalse(presentation.showsToolbarClose)
    }
}
