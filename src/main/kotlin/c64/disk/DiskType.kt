package c64.disk

/**
 * Enum of all possible disk types
 *
 * @author Daniel Schulte 2001-2017
 */
enum class DiskType {
    /**
     * unknown disktype
     */
    UNKNOWN,
    /**
     * d64 with 35 tracks
     */
    D64_35TRACK,
    /**
     * d64 with 35 tracks and error informations
     */
    D64_35TRACK_E,
    /**
     * d64 with 40 tracks (extended)
     */
    D64_40TRACK,
    /**
     * d64 with 40 tracks (extended) and error informations
     */
    D64_40TRACK_E
}
