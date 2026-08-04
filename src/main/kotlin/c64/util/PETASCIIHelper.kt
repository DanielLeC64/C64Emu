package c64.util

/**
 * Helper Class for converting PET &lt;=> ASCII and binary data to ASCII.
 *
 * @author Daniel Schulte 2001-2017
 */
object PETASCIIHelper {
    /**
     * Converts the binary data from an array of short into an string.
     *
     * @param data   the binary array of short
     * @param offset offset where the conversion starts
     * @param length maximum length of conversion
     * @return converted bytes as ASCII String
     */
    fun binary2ASCII(data: ShortArray, offset: Int, length: Int): String {
        val sb = StringBuilder(length)
        for (i in offset..offset + length - 1) {
            val s = data[i]
            if (s.toInt() == 0xA0) {
                return sb.toString()
            } else {
                sb.append(PET2ASCII(s))
            }
        }
        return sb.toString()
    }

    /**
     * Converts the given short to an single character.
     *
     * @param value short to be converted
     * @return given value as character
     */
    fun PET2ASCII(value: Short): Char {
        return value.toInt().toChar()
    }
}
