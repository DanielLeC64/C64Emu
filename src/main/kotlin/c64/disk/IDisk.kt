package c64.disk

/**
 * Generic Disk Interface for all types of diskimages.
 *
 * @author Daniel Schulte 2001-2017
 */
interface IDisk {
    /**
     * Type of the diskimage. Possible values are defined in [DiskType].
     */
    val diskType: DiskType

    /**
     * Number of tracks on this disk.
     */
    val trackCount: Int

    /**
     * The [Directory] object for this disk.
     */
    val directory: Directory

    /**
     * Returns the specified Tracks/Sector as [DiskSector] object.
     *
     * @param ts the track/sector to return
     * @return an [DiskSector] object for the specified track/sector
     * @see getDiskSector
     */
    fun getDiskSector(ts: TrackSector): DiskSector

    /**
     * Returns the specified Tracks/Sector as [DiskSector] object.
     *
     * @param track  track where the sector is located
     * @param sector sector to return
     * @return an [DiskSector] object for the specified track/sector
     * @see getDiskSector
     */
    fun getDiskSector(track: Int, sector: Int): DiskSector

    /**
     * Returns an [FileChain] object starting on the specified track/sector.
     *
     * @param ts the track/sector where the file-chain starts
     * @return an [FileChain] object starting at the specified track/sector
     * @see getFileChain
     */
    fun getFileChain(ts: TrackSector): FileChain

    /**
     * Returns an [FileChain] object starting on the specified track/sector.
     *
     * @param track  track where the start-sector is located
     * @param sector start-sector for file-chain
     * @return an [FileChain] object starting at the specified track/sector
     * @see getFileChain
     */
    fun getFileChain(track: Int, sector: Int): FileChain

}
