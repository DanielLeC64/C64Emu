package c64.disk

/**
 * @author Daniel Schulte 2001-2017
 */
object DiskConstants {
    /**
     * size of sector in byte
     */
    val SECTOR_SIZE: Short = 256
    /**
     * bytes used for 'real' data per sector
     */
    val SECTOR_DATA: Short = 254
    /**
     * directory-entries per sector
     */
    val DIR_ENTRIES_PER_SECTOR: Byte = 8
    /**
     * filesize for 35  Track D64-Diskimage
     */
    val SIZE_D64_35 = 174848
    /**
     * filesize for 35E Track D64-Diskimage
     */
    val SIZE_D64_35E = 175531
    /**
     * filesize for 40  Track D64-Diskimage (extended)
     */
    val SIZE_D64_40 = 196608
    /**
     * filesize for 40E Track D64-Diskimage (extended)
     */
    val SIZE_D64_40E = 197376
    /**
     * filesize for 70  Track D71-Diskimage
     */
    val SIZE_D71_70 = 349696
    /**
     * filesize for 70E Track D71-Diskimage
     */
    val SIZE_D71_70E = 351062
    /**
     * filesize for 80  Track D81-Diskimage
     */
    val SIZE_D81_80 = 819200
    /**
     * filesize for 80E Track D81-Diskimage
     */
    val SIZE_D81_80E = 822400
    /**
     * block count on 35 track D64-diskimage
     */
    val BC_D64_35 = 664
    /**
     * block count on 40 track D64-diskimage
     */
    val BC_D64_40 = 749
}
