package de.kjell.zencompanion.data

import android.content.Context
import androidx.annotation.StringRes
import de.kjell.zencompanion.R

/** Badge artwork for one release note. Mapped to a Material icon in the UI. */
enum class WhatsNewIcon {
    SHARE,
    TAB_MENU,
    SEARCH,
    GRID,
    FIXES,
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
 * changes, in reading order. The Android list is not identical to iOS where a
 * change is platform-specific (rotation here, iPad full screen there).
 */
object WhatsNewRelease {
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
            icon = WhatsNewIcon.TAB_MENU,
        ),
        WhatsNewItem(
            id = "open-into-search",
            titleRes = R.string.whats_new_item_search_title,
            detailRes = R.string.whats_new_item_search_detail,
            icon = WhatsNewIcon.SEARCH,
        ),
        WhatsNewItem(
            id = "essentials-per-row",
            titleRes = R.string.whats_new_item_essentials_title,
            detailRes = R.string.whats_new_item_essentials_detail,
            icon = WhatsNewIcon.GRID,
        ),
        WhatsNewItem(
            id = "more-fixes",
            titleRes = R.string.whats_new_item_more_fixes_title,
            detailRes = R.string.whats_new_item_more_fixes_detail,
            icon = WhatsNewIcon.FIXES,
        ),
    )
}

/** Storage seam for the app version whose notes were already shown. */
interface WhatsNewRepository {
    fun shownVersion(): String?
    fun setShownVersion(version: String)
}

/**
 * Once-per-version decision, kept free of Android types so it is testable on
 * the JVM. The current version is the key: the next store release presents the
 * sheet again without any extra bookkeeping. A fresh install also presents it,
 * which is safer than guessing whether the app was updated.
 */
class WhatsNewController(
    private val repository: WhatsNewRepository,
    private val currentVersion: String,
) {
    fun shouldPresent(isDemo: Boolean): Boolean =
        !isDemo && repository.shownVersion() != currentVersion

    /** Call when the sheet is presented, not when it is dismissed. */
    fun markPresented() {
        repository.setShownVersion(currentVersion)
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
