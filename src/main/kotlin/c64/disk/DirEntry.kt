package c64.disk

import c64.util.PETASCIIHelper
import java.io.Serializable

@Suppress("unused")
/**
 * Represents an directory entry on disk. The values of this entry are calculated each access from the
 * [D64Disk.rawData] Field of the attached disk.
 *
 * @author Daniel Schulte 2001-2017
 */
class DirEntry internal constructor(private val disk: AbstractDisk, private val ts: TrackSector,
                                    private val dirIndex: Int) : Serializable {

    /**
     * Name of this file as ASCII string.
     */
    val filename: String
        get() {
            val offset = OFFS_FILENAME + calcDirEntryOffset()
            return PETASCIIHelper.binary2ASCII(this.disk.rawData, offset, 16)
        }

    /**
     * Returns the length of this file in number of blocks.
     */
    val filesize: Int
        get() {
            var offset = OFFS_FILE_LENGTH + calcDirEntryOffset()
            return this.disk.rawData[offset++] + (this.disk.rawData[offset].toInt() shl 8) // *256
        }

    // todo: implementation
    val filesizeInBytes: Int
        get() = 0

    /**
     * The first [TrackSector] for this file.
     */
    val firstTrackSector: TrackSector
        get() {
            var offset = OFFS_FIRST_LINK + calcDirEntryOffset()
            return TrackSector(this.disk.rawData[offset++].toInt(), this.disk.rawData[offset].toInt())
        }

    /**
     * The [TrackSector] for the first side sector of this file. (only REL Files)
     */
    val firstSideSector: TrackSector
        get() {
            var offset = OFFS_FIRST_SS_LINK + calcDirEntryOffset()
            return TrackSector(this.disk.rawData[offset++].toInt(), this.disk.rawData[offset].toInt())
        }

    /**
     * Contains `true` if this file is locked. This will be represented by an ">" character on a C64.
     */
    val isLocked: Boolean
        get() {
            val offset = OFFS_FILE_TYPE + calcDirEntryOffset()
            return 0x40 == this.disk.rawData[offset].toInt() and 0x40
        }

    /**
     * Contains `false` if this file is not closed. This will be represented by an "*" character on c64.
     */
    val isClosed: Boolean
        get() {
            val offset = OFFS_FILE_TYPE + calcDirEntryOffset()
            return 0x80 == this.disk.rawData[offset].toInt() and 0x80
        }

    /**
     * Contains the type of this file, e.g. DEL, PRG, ...
     */
    val fileType: FileType
        get() {
            val offset = OFFS_FILE_TYPE + calcDirEntryOffset()
            var value = this.disk.rawData[offset].toInt()
            if (value == 0) {
                return FileType.SCRATCHED
            }
            value = value and 0x07 // AND 00000111
            when (value) {
                0 -> return FileType.DEL
                1 -> return FileType.SEQ
                2 -> return FileType.PRG
                3 -> return FileType.USR
                4 -> return FileType.REL
                5 -> return FileType.CBM
                else -> return FileType.UNKNOWN
            }
        }

    override fun toString(): String {
        var result = "file[ $filename, $filesize blocks, ${getFileTypeAsStr(fileType)}"
        if (!isClosed) {
            result += "*"
        }
        if (isLocked) {
            result += ">"
        }
        result += "]"
        return result

        /*val sb = StringBuilder(50)
        sb.append("file[ ").append(filename).append(", ").append(filesize).append(" blocks, ")
        sb.append(getFileTypeAsStr(fileType))
        if (!isClosed) {
            sb.append("*")
        }
        if (isLocked) {
            sb.append(">")
        }
        sb.append("]")
        return sb.toString()*/
    }

    private fun calcDirEntryOffset(): Int {
        return this.disk.calcSectorOffset(this.ts) + (this.dirIndex shl 5) // *32
    }

    companion object {
        private val OFFS_NEXT_SECTOR_TS = 0x00
        private val OFFS_FILE_TYPE = 0x02
        private val OFFS_FIRST_LINK = 0x03
        private val OFFS_FILENAME = 0x05
        private val OFFS_FIRST_SS_LINK = 0x15
        private val OFFS_REL_REC_LENGTH = 0x17
        private val OFFS_GEOS_RESERVED = 0x18
        private val OFFS_FILE_LENGTH = 0x1E // file size in sectors; lo/hi byte order

        private fun getFileTypeAsStr(filetype: FileType): String {
            when (filetype) {
                FileType.SCRATCHED -> return "SCR"
                FileType.DEL -> return "DEL"
                FileType.SEQ -> return "SEQ"
                FileType.PRG -> return "PRG"
                FileType.USR -> return "USR"
                FileType.REL -> return "REL"
                FileType.CBM -> return "CBM"
                else -> return "???"
            }
        }
    }
}
