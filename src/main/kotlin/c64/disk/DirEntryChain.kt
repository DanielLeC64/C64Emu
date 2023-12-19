package c64.disk

/**
 * This class implements an iterator for a chain of directory entries.
 * With [hasNextDirEntry] you can check existence of an following directory entry.
 * With [nextDirEntry] you can get the next [DirEntry].
 *
 * @author Daniel Schulte 2001-2017
 */
class DirEntryChain internal constructor(val disk: AbstractDisk, diskSector: DiskSector) {
    private var currentDirIndex = 0
    private var currentDiskSector: DiskSector?

    init {
        this.currentDiskSector = diskSector
    }

    /**
     * Returns `true` if yet another dir entry exists, otherwise `false`.
     * @return `true` if yet another dir entry exists
     */
    fun hasNextDirEntry(): Boolean {
        return findNextDirEntry(false)
    }

    /**
     * Returns the next dir entry in this "dirEntry-chain". If there's no next dir entry, 'null' is returned.
     * @return next dir entry
     */
    fun nextDirEntry(): DirEntry? {
        if (findNextDirEntry(true)) {
            val result = currentDirEntry
            this.currentDirIndex++
            if (this.currentDirIndex > 7) {
                this.currentDirIndex = 0
                this.currentDiskSector = this.currentDiskSector!!.nextDiskSectorInChain
            }
            return result
        }
        return null
    }

    /**
     * Returns the current DirEntry ([currentDirIndex] in [currentDiskSector]) without testing
     * any parameter.
     * @return the current [DirEntry]
     */
    private val currentDirEntry: DirEntry?
        get() {
            if (this.currentDiskSector != null) {
                return DirEntry(this.disk, this.currentDiskSector!!.trackSector, this.currentDirIndex)
            }
            return null
        }

    /**
     * Finds the next valid [DirEntry] (no scratched or unknown files) and sets the
     * currentDirIndex and currentDiskSector values to this dirEntry if parameter *moveInternalMarker*
     * is `true`.
     * @param moveInternalMarker should this method set internal "current entry" fields?
     * @param currentDiskSector  current value of disk sector
     * @param currentDirIndex    current value of dir index
     * @return `true` if there's another dirEntry, `false` otherwise
     */
    private fun findNextDirEntry(moveInternalMarker: Boolean, currentDiskSector: DiskSector? = this.currentDiskSector,
                                 currentDirIndex: Int = this.currentDirIndex): Boolean {
        var _currentDiskSector = currentDiskSector
        var _currentDirIndex = currentDirIndex
        if (_currentDiskSector != null) {
            if (_currentDirIndex > 7) {
                _currentDirIndex = 0
                _currentDiskSector = currentDiskSector!!.nextDiskSectorInChain
                if (_currentDiskSector == null) {
                    return false
                }
            }

            val entry = DirEntry(this.disk, _currentDiskSector.trackSector, _currentDirIndex)
            val filetype = entry.fileType
            if (filetype === FileType.PRG || filetype === FileType.REL || filetype === FileType.SEQ ||
                    filetype === FileType.USR || filetype === FileType.DEL || filetype === FileType.CBM) {
                if (moveInternalMarker) {
                    this.currentDiskSector = _currentDiskSector
                    this.currentDirIndex = _currentDirIndex
                }
                return true
            } else {
                // scratched or unknown filetype
                // ==> check next dirEntry
                return findNextDirEntry(moveInternalMarker, _currentDiskSector, ++_currentDirIndex)
            }
        }
        return false
    }
}
