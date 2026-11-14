package dev.rohitshukla.pixelscroll.service

/** What is on screen: the foreground app's package name and the view ids in its window. */
data class ScreenSnapshot(
    val packageName: String,
    val viewIds: Set<String>,
)

/**
 * Tells whether the screen shows YouTube Shorts. Pure Kotlin: the service
 * turns the accessibility node tree into a [ScreenSnapshot] and passes it in.
 */
class TargetScreenDetector(private val targets: List<TargetScreen> = TargetScreens.all) {

    /** The target screen that is showing, or null for any other app or screen. */
    fun detect(snapshot: ScreenSnapshot): TargetApp? =
        targets.firstOrNull { target ->
            target.packageName == snapshot.packageName &&
                target.markerViewIds.any { it in snapshot.viewIds }
        }?.app
}
