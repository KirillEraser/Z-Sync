package de.kjell.zencompanion

import android.webkit.WebView
import de.kjell.zencompanion.ui.screens.BrowserLinkAction
import de.kjell.zencompanion.ui.screens.browserLinkAction
import de.kjell.zencompanion.ui.screens.isWebScheme
import de.kjell.zencompanion.ui.screens.sameTabUrl
import de.kjell.zencompanion.ui.screens.tappedLinkUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserLinkPolicyTests {

    private val ownPackage = "de.kjell.zencompanion"
    private val browsers = setOf("com.android.chrome", "org.mozilla.firefox")
    private val youtubePackage = "com.google.android.youtube"

    private fun action(
        scheme: String?,
        isRedirect: Boolean = false,
        hasGesture: Boolean = true,
        isMainFrame: Boolean = true,
        resolvedPackage: String? = youtubePackage,
    ): BrowserLinkAction = browserLinkAction(
        scheme = scheme,
        isRedirect = isRedirect,
        hasGesture = hasGesture,
        isMainFrame = isMainFrame,
        resolvedPackage = resolvedPackage,
        browserPackages = browsers,
        ownPackage = ownPackage,
    )

    @Test
    fun webUrlClaimedByAppOpensExternally() {
        assertEquals(BrowserLinkAction.OPEN_EXTERNALLY, action("https"))
        assertEquals(BrowserLinkAction.OPEN_EXTERNALLY, action("http"))
    }

    @Test
    fun schemeMatchingIsCaseInsensitive() {
        assertEquals(BrowserLinkAction.OPEN_EXTERNALLY, action("HTTPS"))
        assertEquals(BrowserLinkAction.OPEN_EXTERNALLY, action("HtTp"))
    }

    @Test
    fun webUrlResolvedToBrowserStaysInWebView() {
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("https", resolvedPackage = "com.android.chrome"),
        )
    }

    @Test
    fun webUrlWithoutHandlerStaysInWebView() {
        assertEquals(BrowserLinkAction.LOAD_IN_WEBVIEW, action("https", resolvedPackage = null))
    }

    @Test
    fun webUrlResolvedToOwnAppStaysInWebView() {
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("https", resolvedPackage = ownPackage),
        )
    }

    @Test
    fun typedUrlWithoutGestureStaysInWebView() {
        assertEquals(BrowserLinkAction.LOAD_IN_WEBVIEW, action("https", hasGesture = false))
    }

    @Test
    fun redirectStaysInWebView() {
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("https", isRedirect = true),
        )
    }

    @Test
    fun nonWebSchemesOpenExternallyOnlyAfterATap() {
        for (scheme in listOf("mailto", "tel", "intent", "youtube")) {
            assertEquals(
                scheme,
                BrowserLinkAction.OPEN_EXTERNALLY,
                action(scheme, hasGesture = true, resolvedPackage = null),
            )
            assertEquals(
                scheme,
                BrowserLinkAction.BLOCK,
                action(scheme, hasGesture = false, resolvedPackage = null),
            )
        }
    }

    /**
     * A tap inside an iframe must not leave the app; only a tap in the main
     * frame may. Mirrors iOS `BrowserNavigationPolicy.opensExternally`.
     */
    @Test
    fun appSchemesOpenOnlyFromAMainFrameTap() {
        for (scheme in listOf("mailto", "tel", "intent", "youtube")) {
            assertEquals(
                scheme,
                BrowserLinkAction.OPEN_EXTERNALLY,
                action(scheme, hasGesture = true, isMainFrame = true, resolvedPackage = null),
            )
            assertEquals(
                scheme,
                BrowserLinkAction.BLOCK,
                action(scheme, hasGesture = true, isMainFrame = false, resolvedPackage = null),
            )
        }
    }

    @Test
    fun webSchemeCheckMatchesThePolicy() {
        assertTrue(isWebScheme("HTTPS"))
        assertTrue(isWebScheme(null))
        assertFalse(isWebScheme("mailto"))
        assertFalse(isWebScheme("intent"))
    }

    @Test
    fun newWindowKeepsHttpAndHttpsInTheSameTab() {
        assertEquals(
            "http://www.org.chemie.uni-muenchen.de/ac/kornath/flusssaeure-hf/",
            sameTabUrl("http://www.org.chemie.uni-muenchen.de/ac/kornath/flusssaeure-hf/"),
        )
        assertEquals(
            "https://www.google.com/goto?url=abc",
            sameTabUrl("https://www.google.com/goto?url=abc"),
        )
    }

    @Test
    fun blankNewWindowDoesNotReplaceThePage() {
        assertEquals(null, sameTabUrl("about:blank"))
        assertEquals(null, sameTabUrl("javascript:void(0)"))
        assertEquals(null, sameTabUrl(null))
        assertEquals(null, sameTabUrl("  "))
    }

    @Test
    fun tappedTextLinkLoadsInTheSameTab() {
        assertEquals(
            "https://example.com/next",
            tappedLinkUrl(WebView.HitTestResult.SRC_ANCHOR_TYPE, "https://example.com/next"),
        )
    }

    @Test
    fun tappedImageLinkLeavesTheAddressToThePopup() {
        // For an image link the hit test reports the image, not where the link goes.
        assertEquals(
            null,
            tappedLinkUrl(WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE, "https://example.com/picture.png"),
        )
        assertEquals(null, tappedLinkUrl(WebView.HitTestResult.UNKNOWN_TYPE, null))
    }

    @Test
    fun inPageSchemesStayInWebView() {
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("about", hasGesture = false, resolvedPackage = null),
        )
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("data", hasGesture = false, resolvedPackage = null),
        )
        assertEquals(
            BrowserLinkAction.LOAD_IN_WEBVIEW,
            action("javascript", hasGesture = false, resolvedPackage = null),
        )
    }
}
