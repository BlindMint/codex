package us.blindmint.codex.data.parser

/**
 * Compares two strings using natural ordering (alphanumeric sorting).
 * This respects numerical values within strings, e.g., "page2" < "page10".
 *
 * Based on the typical natural sorting algorithm used in Windows Explorer.
 */
object NaturalOrderComparator {
    fun compare(s1: String?, s2: String?): Int {
        if (s1 === s2) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        var i1 = 0
        var i2 = 0
        val len1 = s1.length
        val len2 = s2.length

        while (i1 < len1 && i2 < len2) {
            val c1 = s1[i1]
            val c2 = s2[i2]

            // Check if both characters are digits
            if (Character.isDigit(c1) && Character.isDigit(c2)) {
                // Find the start of the numeric part
                var start1 = i1
                var start2 = i2
                while (start1 < len1 && Character.isDigit(s1[start1])) start1++
                while (start2 < len2 && Character.isDigit(s2[start2])) start2++

                val num1 = s1.substring(i1, start1).toLong()
                val num2 = s2.substring(i2, start2).toLong()

                // Compare numeric values
                val cmp = num1.compareTo(num2)
                if (cmp != 0) return cmp

                // If numeric values are equal, continue with the rest
                i1 = start1
                i2 = start2
            } else {
                // Compare characters normally
                if (c1 != c2) {
                    return c1.compareTo(c2)
                }
                i1++
                i2++
            }
        }

        // If one string is a prefix of the other, the shorter one comes first
        return (len1 - i1).compareTo(len2 - i2)
    }
}