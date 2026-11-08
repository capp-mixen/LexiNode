package com.lexinode.app.ui

/** Helpers for correct display of Hebrew (RTL) mixed with Russian (LTR). */
object Bidi {
    private const val FSI = '⁨' // First Strong Isolate
    private const val PDI = '⁩' // Pop Directional Isolate

    private val controlChars = setOf(
        '‎', '‏', '؜',
        '‪', '‫', '‬', '‭', '‮',
        '⁦', '⁧', '⁨', '⁩',
    )

    /** Wraps text so its direction does not affect the surrounding line. */
    fun isolate(text: String): String = "$FSI$text$PDI"

    fun containsHebrew(text: String): Boolean =
        text.any { it in '֐'..'׿' || it in 'יִ'..'ﭏ' }

    /** Removes stray direction control characters and extra whitespace from user input. */
    fun clean(text: String): String =
        text.filterNot { it in controlChars }
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
}
