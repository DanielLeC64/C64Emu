package c64.disk

/**
 * This object represents a link to a sector on disk. It contains the track number and the sector number.
 *
 * @author Daniel Schulte 2001-2017
 */
class TrackSector internal constructor(
        /**
         * track number
         */
        val track: Int,
        /**
         * sector number
         */
        val sector: Int) {

    override fun toString(): String {
        return "[T:${this.track} S:${this.sector}]"
    }
}
