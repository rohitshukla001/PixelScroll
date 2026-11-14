package dev.rohitshukla.pixelscroll.progress

/**
 * Reads a Short's length from YouTube's seek bar description, for example
 * "0 minutes 2 seconds of 1 minute 5 seconds". Pure Kotlin.
 *
 * The words follow the phone's language, so when the English pattern does not match, the
 * numbers alone are used: the first half is the position, the second half the length, each as
 * seconds, minutes and seconds, or hours, minutes and seconds. Digits may be in any script
 * (for example ٣٥ or ३५), not only 0–9.
 */
object SeekBarDescription {

    private val PART = Regex("""(?:(\d+)\s+hours?\s*)?(?:(\d+)\s+minutes?\s*)?(?:(\d+)\s+seconds?)?""")

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
        val numbers = numbersIn(text)
        if (numbers.size !in setOf(2, 4, 6)) return null
        val length = numbers.subList(numbers.size / 2, numbers.size)
        return length.fold(0) { total, n -> total * 60 + n }.takeIf { it > 0 }
    }

    /** Every run of digits in [text], in any script, as numbers. */
    private fun numbersIn(text: String): List<Int> {
        val numbers = mutableListOf<Int>()
        var current = -1
        for (ch in text) {
            val digit = Character.digit(ch, 10)
            if (digit >= 0) {
                current = (if (current < 0) 0 else current) * 10 + digit
                if (current > 99_999) return emptyList()
            } else if (current >= 0) {
                numbers += current
                current = -1
            }
        }
        if (current >= 0) numbers += current
        return numbers
    }
}
