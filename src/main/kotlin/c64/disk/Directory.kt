package c64.disk

import c64.util.PETASCIIHelper

/**
 * This class represents the directory of an diskimage. You can access all informations of this disk, like
 * diskname, dostype, write protection...
 *
 * @author Daniel Schulte 2001-2017
 */
class Directory internal constructor(private val disk: AbstractDisk) {

    // todo: bam

    /**
     * Returns the [DirEntryChain] for this disk.
     *
     * @return [DirEntryChain] of this disk
     */
    val dirEntryChain: DirEntryChain
        get() = DirEntryChain(this.disk, this.disk.getDiskSector(18, 1))

    /**
     * Returns the name of the disk as an string of max. length 16.
     * Characters are already converted from PET to ASCII.
     *
     * @return Name of the disk
     */
    val diskName: String
        get() {
            val offset = OFFS_DISK_NAME + this.disk.calcSectorOffset(18, 0)
            return PETASCIIHelper.binary2ASCII(this.disk.rawData, offset, 16)
        }

    /**
     * Returns the Disk-ID as string of max. length 3. This value is normally something like "00 ".
     * Characters are already converted from PET to ASCII.
     *
     * @return Disk-ID as string
     */
    val diskID: String
        get() {
            val offset = OFFS_DISK_ID + this.disk.calcSectorOffset(18, 0)
            return PETASCIIHelper.binary2ASCII(this.disk.rawData, offset, 3)
        }

    /**
     * Returns the DOS-Type of this diskimage, usually "2A".
     * Characters are already converted from PET to ASCII.
     *
     * @return DOC-Type of this diskimage
     */
    val dosType: String
        get() {
            val offset = OFFS_DOS_TYPE + this.disk.calcSectorOffset(18, 0)
            return PETASCIIHelper.binary2ASCII(this.disk.rawData, offset, 2)
        }

    /**
     * Returns the DOS version type, usually "A". This value is also responsible for the "SoftWriteProtection".
     * Characters are already converted from PET to ASCII.
     *
     * @return DOS version type of this diskimage
     * *
     * @see isSoftWriteProtected
     */
    val dosVersionType: String
        get() {
            val offset = OFFS_DOS_VERSION + this.disk.calcSectorOffset(18, 0)
            return PETASCIIHelper.binary2ASCII(this.disk.rawData, offset, 1)
        }

    /**
     * Returns `true` if this diskimage is softwrite protected via the DOS version type.
     *
     * @return is this disk softwrite protected?
     * @see dosVersionType
     */
    val isSoftWriteProtected: Boolean
        get() {
            val offset = OFFS_DOS_VERSION + this.disk.calcSectorOffset(18, 0)
            val s = this.disk.rawData[offset]
            // softwrite-protected if DosVersionType other than $41 or $00
            return !(s.toInt() == 0x41 || s.toInt() == 0x00)
        }

    companion object {
        //private final static int OFFS_FIRST_DIR_T = 0x00; // track of first dir sector (may be anything, don't trust)
        //private final static int OFFS_FIRST_DIR_S = 0x01; // sector of first dir sector (may be anything, don't trust)
        private val OFFS_DOS_VERSION = 0x02
        //private final static int OFFS_UNUSED1     = 0x03;
        private val OFFS_BAM_LAYOUT = 0x04
        private val OFFS_DISK_NAME = 0x90
        //private final static int OFFS_UNUSED2     = 0xA0;
        private val OFFS_DISK_ID = 0xA2
        //private final static int OFFS_UNUSED3     = 0xA4; // also DiskID
        private val OFFS_DOS_TYPE = 0xA5
        //private final static int OFFS_UNUSED4     = 0xA7;
        private val OFFS_EBAM_LAYOUT = 0xAB // extended BAM Layout (for extended disks with 40 tracks)
    }
}
