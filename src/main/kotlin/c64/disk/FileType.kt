package c64.disk

/**
 * Enum for all possible types of files.
 *
 * @author Daniel Schulte 2001-2017
 */
enum class FileType {
    /**
     * unknown filetype
     */
    UNKNOWN,
    /**
     * scratched file
     */
    SCRATCHED,
    /**
     * deleted file
     */
    DEL,
    /**
     * filetype SEQ
     */
    SEQ,
    /**
     * filetype PRG
     */
    PRG,
    /**
     * filetype USR
     */
    USR,
    /**
     * filetype REL
     */
    REL,
    /**
     * filetype CBM
     */
    CBM
}
