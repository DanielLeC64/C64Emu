@file:Suppress("unused")

package c64.disk

import org.apache.log4j.BasicConfigurator
import java.io.IOException
import java.util.ArrayList

import org.slf4j.LoggerFactory

/**
 * @author Daniel Schulte 2001-2017
 */
abstract class AbstractDisk @Throws(IOException::class)
internal constructor(private val imageFile: String) : IDisk {

    init {
        BasicConfigurator.configure()
    }

    /**
     * track with directory
     */
    internal var dirTrack = 18

    /**
     * interleave for this diskformat
     */
    internal var interleave = 10

    /**
     * offset-table with offsets of every track in binary data
     */
    lateinit internal var offsetTable: IntArray

    lateinit internal var rawData: ShortArray

    /**
     * list with one entry for each track, each entry is an list with the sectors [DiskSector] of this track
     */
    internal val tracksAndSectors = ArrayList<MutableList<DiskSector>>()

    internal fun loadDiskDataAndInit() {
        this.tracksAndSectors.ensureCapacity(trackCount + 1)
        // add dummy element for track 0x00; but real first track is 0x01
        this.tracksAndSectors.add(mutableListOf())
        for (track in 1..trackCount) {
            this.tracksAndSectors.add(ArrayList<DiskSector>(getSectorsAtTrack(track)))
        }

        readFileData()
        initDiskStructure()
    }

    @Throws(IOException::class)
    private fun readFileData() {
        // todo: write-lock for file
        logger.info("loading c64.disk from imageFile: {}", this.imageFile)
        val reader = BinaryDiskReader(this.imageFile)
        this.rawData = reader.readData()
    }

    private fun closeDiskImage() {
        // todo: unlock file ==> system hook needed
    }

    /**
     * Calculates the offset for any sector on this disk.
     *
     * @param ts TrackSector Link
     * @return calculated offset for given sector
     * @see calcSectorOffset
     */
    internal fun calcSectorOffset(ts: TrackSector): Int {
        return calcSectorOffset(ts.track, ts.sector)
    }

    /**
     * Calculates the offset for any sector on this disk.
     *
     * @param track  track number
     * @param sector sector number
     * @return calculated offset for given sector
     * @see calcSectorOffset
     */
    internal fun calcSectorOffset(track: Int, sector: Int): Int {
        return this.offsetTable[track] + sector * sectorLength
    }

    /**
     * Returns the specified Tracks/Sector as [DiskSector] object.
     *
     * @param ts the track/sector to return
     * @return an [DiskSector] object for the specified track/sector
     */
    override fun getDiskSector(ts: TrackSector): DiskSector {
        return getDiskSector(ts.track, ts.sector)
    }

    /**
     * Returns the specified Tracks/Sector as [DiskSector] object.
     *
     * @param track  track where the sector is located
     * @param sector sector to return
     * @return an [DiskSector] object for the specified track/sector
     * @see getDiskSector
     */
    override fun getDiskSector(track: Int, sector: Int): DiskSector {
        return this.tracksAndSectors[track][sector]
    }

    /**
     * Returns an [FileChain] object starting on the specified track/sector.
     *
     * @param ts the track/sector where the file-chain starts
     * @return an [FileChain] object starting at the specified track/sector
     * @see getFileChain
     */
    override fun getFileChain(ts: TrackSector): FileChain {
        return getFileChain(ts.track, ts.sector)
    }

    /**
     * Returns an [FileChain] object starting on the specified track/sector.

     * @param track  track where the start-sector is located
     * *
     * @param sector start-sector for file-chain
     * *
     * @return an [FileChain] object starting at the specified track/sector
     * *
     * @see .getFileChain
     */
    override fun getFileChain(track: Int, sector: Int): FileChain {
        return getDiskSector(track, sector).fileChain
    }

    /**
     * Returns the type of diskimage. Possible values are defined in [DiskType].
     *
     * @return type of diskimage
     */
    abstract override val diskType: DiskType

    private fun initDiskStructure() {
        // iterate every track and handle the sectors
        for (track in 1..trackCount) {
            val sectorCount = getSectorsAtTrack(track)
            val trackData: MutableList<DiskSector> = this.tracksAndSectors[track]
            (0..sectorCount - 1).mapTo(trackData) { DiskSector(TrackSector(track, it), this) }
        }
    }

    /**
     * Returns the number of tracks on this disk.
     *
     * @return number of tracks on this disk
     */
    abstract override val trackCount: Int

    /**
     * Returns the number of sectors on a given track.
     *
     * @param track track number
     * @return number of sectors on a given track
     */
    protected abstract fun getSectorsAtTrack(track: Int): Int

    /**
     * Returns the [Directory] Objekt for this disk.
     *
     * @return Directory object for this disk
     */
    override val directory: Directory by lazy {Directory(this)}

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractDisk::class.java)

        /**
         * returns the number of bytes for one sector.
         *
         * @return number of bytes on one sector
         */
        internal val sectorLength: Short
            get() = DiskConstants.SECTOR_SIZE

        /**
         * only test method
         */
        fun printRawData(data: ShortArray?): String {
            if (data != null) {
                val sb = StringBuilder(4 * data.size) // Zahl[nn] + Komma[,]
                sb.append("[")
                for (i in data.indices) {
                    sb.append(data[i].toInt())
                    if (i + 1 < data.size) {
                        sb.append(",")
                    }
                }
                sb.append("]")
                return sb.toString()
            }
            return ""
        }
    }
}
