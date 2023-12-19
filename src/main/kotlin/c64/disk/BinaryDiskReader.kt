package c64.disk

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This class implements an FileInputStream which can read data from an image file and converts the data in
 * an array of short (short[]).
 *
 * @author Daniel Schulte 2001-2017
 */
class BinaryDiskReader(file: String) {
    private val file: String

    init {
        if (file.isEmpty()) {
            throw IllegalArgumentException("BinaryDiskReader.<init>: argument file may not be <empty>!")
        }
        this.file = file
    }

    /**
     * Reads the complete file and convert it to short[]. After this operation the FileInputStream will be closed.
     *
     * @return array of short with converted data
     * @throws FileNotFoundException file not found
     * @throws IOException i/o error while reading file
     */
    @Throws(IOException::class)
    fun readData(): ShortArray {
        val buffer = Files.readAllBytes(Paths.get(this.file))
        val data = ShortArray(buffer.size)
        for (i in 0 until buffer.size) {
            data[i] = convertByte(buffer[i])
        }
        return data
    }

    /**
     * Reads the data from offset to offset+length and convert it to short[].
     *
     * @param off offset - where to start reading
     * @param len how many bytes to read
     * @return array of short with converted data
     * @throws FileNotFoundException file not found
     * @throws IOException i/o error while reading file
     */
    @Throws(IOException::class)
    fun readData(off: Int, len: Int): ShortArray {
        BufferedInputStream(FileInputStream(this.file)).use { bis ->
            val data = ShortArray(len)
            val buffer = ByteArray(len)
            if (bis.read(buffer, off, len) > 0) {
                for (i in 0 until len) {
                    data[i] = convertByte(buffer[i])
                }
            }
            return data
        }
    }

    companion object {

        /**
         * Method to convert an java-byte (-128..127) to an machine byte (0..255) represented as an java-short.
         *
         * @param b java-byte to be converted
         * @return the converted value as short
         */
        fun convertByte(b: Byte): Short {
            return (b.toInt() and 0xFF).toShort()
        }
    }
}
