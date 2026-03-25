package org.example.urlshortnerapi.service

object Base62Encoder {
    const val ALPHABET: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    const val FIXED_LENGTH: Int = 7 // ← this is the "size" you asked for
    const val MAX_LONG_SUPPORTED: Long = 3521614606208L

    fun encode(long: Long): String {
        var value = long
        check(value < MAX_LONG_SUPPORTED) { "Long exceeded 7-char base62 limit" }

        val sb = StringBuilder()
        do {
            sb.append(ALPHABET[(value % 62).toInt()])
            value /= 62
        } while (value > 0)

        // Pad on the LEFT with '0' to exactly 7 chars
        while (sb.length < FIXED_LENGTH) {
            sb.append('0') // append first because we built reversed
        }
        return sb.reverse().toString() // now it's correctly ordered + padded
    }

}