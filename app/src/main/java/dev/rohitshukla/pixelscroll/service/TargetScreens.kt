package dev.rohitshukla.pixelscroll.service

/** The short-video screen Pixel Scroll works on: YouTube Shorts only. */
enum class TargetApp { YOUTUBE_SHORTS }

/**
 * One app's short-video screen: the app's exact package name, plus view ids that appear only
 * on that screen. The screen counts as showing when any one marker id is on screen.
 */
data class TargetScreen(
    val app: TargetApp,
    val packageName: String,
    val markerViewIds: Set<String>,
)

/**
 * Every view id the app depends on lives here, so an app update means changing one file.
 * The package name is also in res/xml/accessibility_service_config.xml (packageNames).
 *
 * YouTube ids are confirmed on a phone (docs/progress-detection-test.md, 13 Nov 2026).
 */
object TargetScreens {
    val all: List<TargetScreen> = listOf(
        TargetScreen(
            app = TargetApp.YOUTUBE_SHORTS,
            packageName = "com.google.android.youtube",
            markerViewIds = setOf(
                "com.google.android.youtube:id/reel_recycler",
                "com.google.android.youtube:id/reel_player_page_container",
            ),
        ),
    )
}
