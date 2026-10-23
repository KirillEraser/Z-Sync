package de.kjell.zencompanion.data

import android.content.Context
import androidx.annotation.StringRes
import de.kjell.zencompanion.R

/** Badge artwork for one release note. Mapped to a Material icon in the UI. */
enum class WhatsNewIcon {
    SHARE,
    TABS,
    SEARCH,
    MORE,
}

/**
 * One user-facing change in a release. Copy stays in `strings.xml` because it
 * is UI text, and the list is replaced wholesale each release.
 */
data class WhatsNewItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val detailRes: Int,
    val icon: WhatsNewIcon,
)

/**
 * The notes the What's New sheet shows. Keep this to genuinely user-visible
 * changes, in reading order. The version is the store release these notes
 * belong to, not the build currently running.
 */
object WhatsNewRelease {
    const val version: String = "1.5"

    val items: List<WhatsNewItem> = listOf(
        WhatsNewItem(
            id = "share-save-kind",
            titleRes = R.string.whats_new_item_share_title,
            detailRes = R.string.whats_new_item_share_detail,
            icon = WhatsNewIcon.SHARE,
        ),
        WhatsNewItem(
            id = "tab-controls",
            titleRes = R.string.whats_new_item_tab_controls_title,
            detailRes = R.string.whats_new_item_tab_controls_detail,
            icon = WhatsNewIcon.TABS,
        ),
        WhatsNewItem(
            id = "open-into-search",
            titleRes = R.string.whats_new_item_search_title,
            detailRes = R.string.whats_new_item_search_detail,
            icon = WhatsNewIcon.SEARCH,
        ),
        WhatsNewItem(
            id = "more",
            titleRes = R.string.whats_new_item_more_title,
            detailRes = R.string.whats_new_item_more_detail,
            icon = WhatsNewIcon.MORE,
        ),
    )
}

/** Storage seam for the release version whose notes were already shown. */
interface WhatsNewRepository {
    fun shownVersion(): String?
    fun setShownVersion(version: String)
}

/**
 * Once-per-release decision, kept free of Android types so it is testable on
 * the JVM. The notes only appear once the running app reaches their release
 * version, and then only until they have been shown for it. A fresh 1.5
 * install also presents them, which is safer than guessing whether the app
 * was updated.
 */
class WhatsNewController(
    private val repository: WhatsNewRepository,
    private val currentVersion: String,
    private val releaseVersion: String = WhatsNewRelease.version,
) {
    fun shouldPresent(isDemo: Boolean): Boolean =
        !isDemo &&
            currentVersion == releaseVersion &&
            repository.shownVersion() != releaseVersion

    /** Call when the sheet is presented, not when it is dismissed. */
    fun markPresented() {
        repository.setShownVersion(releaseVersion)
    }
}

class AndroidWhatsNewRepository(context: Context) : WhatsNewRepository {
    private val preferences = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    override fun shownVersion(): String? = preferences.getString(KEY_SHOWN_VERSION, null)

    override fun setShownVersion(version: String) {
        preferences.edit().putString(KEY_SHOWN_VERSION, version).apply()
    }

    private companion object {
        const val KEY_SHOWN_VERSION = "whatsNewShownVersion"
    }
}
