package c64.disk

@Suppress("unused")
/**
 * This class presents a disk sector on a standard c64 disk. It implements operations for getting data from
 * this sector and for getting informations about the sectorchain (linking with next sector).

 * @author Daniel Schulte 2001-2017
 */
class DiskSector internal constructor(
        /**
         * The Track/Sector link as [TrackSector] object for this disk sector.
         */
        val trackSector: TrackSector,
        private val diskImage: AbstractDisk) {

    /**
     * The Track/Sector link ([TrackSector]) for the next sector in filechain.
     */
    val nextTrackSectorInChain: TrackSector?

    init {
        var offset = this.diskImage.calcSectorOffset(trackSector)

        if (this.diskImage.rawData[offset].toInt() > 0) {
            this.nextTrackSectorInChain = TrackSector(this.diskImage.rawData[offset++].toInt(), this.diskImage.rawData[offset].toInt())
        } else {
            this.nextTrackSectorInChain = null
        }
    }

    /**
     * Returns the next [DiskSector] which is linked with this sector. If there's no next sector it
     * returns `null`.
     * @return next sector or `null`
     */
    val nextDiskSectorInChain: DiskSector?
        get() {
            if (this.nextTrackSectorInChain != null && this.nextTrackSectorInChain.track > 0) {
                val trackData = this.diskImage.tracksAndSectors[this.nextTrackSectorInChain.track]
                return trackData[this.nextTrackSectorInChain.sector]
            }
            return null
        }

    /**
     * Returns a **copy** of the sector data as an array of short.
     * @return copy of the sector data as an array of short
     */
    val sectorData: ShortArray
        get() {
            val result = ShortArray(AbstractDisk.sectorLength.toInt())
            val offset = this.diskImage.calcSectorOffset(this.trackSector)
            System.arraycopy(this.diskImage.rawData, offset, result, 0, AbstractDisk.sectorLength.toInt())
            return result
        }

    /**
     * Returns an [FileChain] object with the filechain starting on this disk sector.
     * @return FileChain object
     */
    val fileChain: FileChain
        get() = FileChain(this)

    override fun toString() = "Sector $trackSector -> next ${this.nextTrackSectorInChain ?: "null"}"
}
