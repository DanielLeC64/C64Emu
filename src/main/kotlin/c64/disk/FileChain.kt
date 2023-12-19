package c64.disk

import java.util.NoSuchElementException

/**
 * This class implements an iterator for a chain of sectors (filechain). Every sector contains an link to the next
 * sector in an filechain. With [hasNextSector] you can check existence of an following sector.
 * With [nextSector] you can get an [DiskSector] object of the next sector in a chain.
 *
 * @author Daniel Schulte 2001-2017
 */
class FileChain internal constructor(diskSector: DiskSector) {
    private var start = true
    private var currentDiskSector: DiskSector

    init {
        this.currentDiskSector = diskSector
    }

    /**
     * Returns `true` if there's an link to another sector, otherwise it returns `false`
     *
     * @return `true` if there's an link to another sector
     * @see nextSector
     */
    fun hasNextSector(): Boolean {
        return this.start || this.currentDiskSector.nextDiskSectorInChain != null
    }

    /**
     * Returns the next sector in the filechain as [DiskSector] object. If there's no following sector
     * it throws an [NoSuchElementException]!
     *
     * @return the next sector as [DiskSector] object.
     * @throws NoSuchElementException if there's no next sector
     * @see hasNextSector
     */
    @Throws(NoSuchElementException::class)
    fun nextSector(): DiskSector {
        if (hasNextSector()) {
            if (!this.start) {
                this.currentDiskSector = this.currentDiskSector.nextDiskSectorInChain!!
                return this.currentDiskSector
            }
            this.start = false
            return this.currentDiskSector
        } else {
            throw NoSuchElementException("no more sectors in filechain!")
        }
    }
}
