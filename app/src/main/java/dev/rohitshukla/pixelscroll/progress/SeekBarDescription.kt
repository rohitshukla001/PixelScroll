package dev.rohitshukla.pixelscroll.progress

/**
 * Reads a Short's length from YouTube's seek bar description, for example
 * "0 minutes 2 seconds of 1 minute 5 seconds". Pure Kotlin.
 *
 * The words follow the phone's language, so when the English pattern does not match, the
 * numbers alone are used: the first half is the position, the second half the length, each as
 * seconds, minutes and seconds, or hours, minutes and seconds.
 */
object SeekBarDescription {

    private val PART = Regex("""(?:(\d+)\s+hours?\s*)?(?:(\d+)\s+minutes?\s*)?(?:(\d+)\s+seconds?)?""")
    private val NUMBER = Regex("""\d+""")

    /** The video's total length in seconds, or null when the text can't be read. */
    fun durationSec(description: CharSequence?): Int? {
        val text = description?.toString()?.trim() ?: return null
        if (" of " in text) english(text.substringAfter(" of ").trim())?.let { return it }
        return fromNumbers(text)
    }

    private fun english(part: String): Int? {
        if (part.isEmpty()) return null
        val m = PART.matchEntire(part) ?: return null
        val (h, min, s) = m.destructured
        if (h.isEmpty() && min.isEmpty() && s.isEmpty()) return null
        return (h.toIntOrNull() ?: 0) * 3600 + (min.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
    }

    private fun fromNumbers(text: String): Int? {
        val numbers = NUMBER.findAll(text).map { it.value.toIntOrNull() }.toList()
        if (numbers.size !in setOf(2, 4, 6) || numbers.any { it == null }) return null
        val length = numbers.subList(numbers.size / 2, numbers.size)
        return length.fold(0) { total, n -> total * 60 + n!! }.takeIf { it > 0 }
    }
}
