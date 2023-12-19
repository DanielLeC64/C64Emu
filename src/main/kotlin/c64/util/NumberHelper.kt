package c64.util

/**
 * @author Daniel Schulte 2017-2021
 */

/**
 * Returns the given Int as printed hex string in format $AAAA.
 */
fun Int.toHex(): String {
    return "$${this.toUnprefixedHex()}"
}

/**
 * Returns the given Int as printed hex string in format AAAA.
 */
fun Int.toUnprefixedHex(): String {
    return this.toString(16).toUpperCase().padStart(4, '0')
}

/**
 * Returns the given UByte as printed hex string in format $AA.
 */
@ExperimentalUnsignedTypes
fun UByte.toHex(): String {
    return "$${this.toUnprefixedHex()}"
}

/**
 * Returns the given UByte as printed hex string in format AA.
 */
@ExperimentalUnsignedTypes
fun UByte.toUnprefixedHex(): String {
    return this.toString(16).toUpperCase().padStart(2, '0')
}

/**
 * Returns the given UByte as printed binary string in format %1000 1001.
 */
@ExperimentalUnsignedTypes
fun UByte.toBinary(): String {
    return "%${this.toUnprefixedBinary()}"
}

/**
 * Returns the given UByte as printed binary string in format 1000 1001.
 */
@ExperimentalUnsignedTypes
fun UByte.toUnprefixedBinary(): String {
    return StringBuilder(this.toString(2).padStart(8, '0')).insert(4, ' ').toString()
}

/**
 * Returns the boolean represented as a bit-string, "0" or "1".
 */
fun Boolean.toBitString(): String {
    return if (this) "1" else "0"
}

/**
 * Increments the UByte by the given value and returns a new UByte.
 */
@ExperimentalUnsignedTypes
fun UByte.incBy(value: UByte): UByte {
    return this.plus(value).toUByte()
}

/**
 * Decrements the UByte by the given value and returns a new UByte.
 */
@ExperimentalUnsignedTypes
fun UByte.decBy(value: UByte): UByte {
    return this.minus(value).toUByte()
}

/**
 * Converts this BCD coded byte to int.
 */
@ExperimentalUnsignedTypes
fun UByte.bcdToInt(): Int {
    // lo nibble (byte % 16)  +  hi nibble +(byte / 16 * 10)
    return (this and 0b00001111u).toInt() + (toInt() shr 4) * 10
}

/**
 * Converts this byte to a BCD coded byte.
 */
@ExperimentalUnsignedTypes
fun UByte.toBcd(): UByte {
    // (byte / 10 * 16) + (byte % 10)
    return (this / 10u * 16u + this % 10u).toUByte()
}
