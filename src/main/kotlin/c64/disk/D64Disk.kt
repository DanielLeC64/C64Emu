package c64.disk

import java.io.IOException

/**
 * Implementation of [IDisk] for D64 Disktypes.
 *
 * @author Daniel Schulte 2001-2017
 */
class D64Disk @Throws(IOException::class)
internal constructor(imageFile: String) : AbstractDisk(imageFile) {

    init {
        this.dirTrack = 18
        this.interleave = 10
        // offset table is also for 40track format
        this.offsetTable = intArrayOf(0x00000, // feld 0 nur ein dummy, da der erste Track immer 0x01 ist
                0x00000, 0x01500, 0x02A00, 0x03F00, 0x05400,
                0x06900, 0x07E00, 0x09300, 0x0A800, 0x0BD00,
                0x0D200, 0x0E700, 0x0FC00, 0x11100, 0x12600,
                0x13B00, 0x15000, 0x16500, 0x17800, 0x18B00,
                0x19E00, 0x1B100, 0x1C400, 0x1D700, 0x1EA00,
                0x1FC00, 0x20E00, 0x22000, 0x23200, 0x24400,
                0x25600, 0x26700, 0x27800, 0x28900, 0x29A00,
                0x2AB00, 0x2BC00, 0x2CD00, 0x2DE00, 0x2EF00)
        loadDiskDataAndInit()
    }

    /**
     * Returns the type of diskimage. Possible values are defined in [DiskType].
     *
     * @return type of diskimage
     */
    override val diskType: DiskType
        get() = DiskType.D64_35TRACK

    /**
     * Returns the number of tracks on this disk.
     *
     * @return number of tracks on this disk
     */
    override val trackCount: Int
        get() = 35

    /**
     * Returns the number of sectors on a given track.
     *
     * @param track track number
     * @return number of sectors on a given track
     */
    override fun getSectorsAtTrack(track: Int): Int {
        when (track) {
            in 1..17 -> return 21
            in 18..24 -> return 19
            in 25..30 -> return 18
            in 31..40 -> return 17
            else -> return 0
        }
    }
}
