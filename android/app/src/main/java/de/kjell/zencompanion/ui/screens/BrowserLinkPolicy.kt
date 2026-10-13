package de.kjell.zencompanion.ui.screens

import android.webkit.WebView

internal enum class BrowserLinkAction {
    LOAD_IN_WEBVIEW,
    OPEN_EXTERNALLY,

    /** A page tried to open another app without a tap; drop it, as Chrome does. */
    BLOCK,
}

private val WEB_SCHEMES = setOf("http", "https", "about", "data", "blob", "file", "javascript")

/** True when a URL with [scheme] loads in the WebView instead of another app. */
internal fun isWebScheme(scheme: String?): Boolean = scheme == null || scheme.lowercase() in WEB_SCHEMES

internal fun browserLinkAction(
    scheme: String?,
    isRedirect: Boolean,
    hasGesture: Boolean,
    isMainFrame: Boolean,
    resolvedPackage: String?,
    browserPackages: Set<String>,
    ownPackage: String,
): BrowserLinkAction {
    val normalizedScheme = scheme?.lowercase()
    if (normalizedScheme == null || normalizedScheme !in WEB_SCHEMES) {
        // Another app opens only from a tap in the main frame; a tap inside an
        // iframe is dropped, matching the iOS policy.
        return if (hasGesture && isMainFrame) {
            BrowserLinkAction.OPEN_EXTERNALLY
        } else {
            BrowserLinkAction.BLOCK
        }
    }
    if (normalizedScheme != "http" && normalizedScheme != "https") {
        return BrowserLinkAction.LOAD_IN_WEBVIEW
    }
    if (isRedirect || !hasGesture) {
        return BrowserLinkAction.LOAD_IN_WEBVIEW
    }
    if (resolvedPackage == null || resolvedPackage == ownPackage) {
        return BrowserLinkAction.LOAD_IN_WEBVIEW
    }
    if (resolvedPackage in browserPackages) {
        return BrowserLinkAction.LOAD_IN_WEBVIEW
    }
    return BrowserLinkAction.OPEN_EXTERNALLY
}

/** http(s) address a new window should load in the only tab. Blank windows stay put. */
internal fun sameTabUrl(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val scheme = trimmed.substringBefore(':', missingDelimiterValue = "").lowercase()
    if (scheme != "http" && scheme != "https") return null
    return trimmed
}

/**
 * Address a tapped new-window link should load in the only tab. Only plain links count:
 * for an image link the hit test reports the image, not the link, so the popup finds it.
 */
internal fun tappedLinkUrl(hitType: Int, extra: String?): String? {
    if (hitType != WebView.HitTestResult.SRC_ANCHOR_TYPE) return null
    return sameTabUrl(extra)
}
