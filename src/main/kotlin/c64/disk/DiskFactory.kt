package c64.disk

import c64.util.PETASCIIHelper
import org.apache.log4j.BasicConfigurator
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * @author Daniel Schulte 2001-2017
 */
object DiskFactory {
    private val logger = LoggerFactory.getLogger(DiskFactory::class.java)

    // todo any longer needed?
    init {
        BasicConfigurator.configure()
    }

    @Throws(DiskException::class, IOException::class)
    fun getDisk(imageFile: String): IDisk {
        val fImage = File(imageFile)
        if (fImage.exists() && fImage.isFile /* todo: readonly auswerten? */) {
            when (fImage.length().toInt()) {
                DiskConstants.SIZE_D64_35,
                DiskConstants.SIZE_D64_35E,
                DiskConstants.SIZE_D64_40,
                DiskConstants.SIZE_D64_40E -> {
                    logger.info("found D64 disk.")
                    return D64Disk(imageFile)
                }
                DiskConstants.SIZE_D71_70,
                DiskConstants.SIZE_D71_70E -> {
                    logger.info("found D71 disk.")
                    throw DiskException("D71 diskformat not supported yet!")
                }
                DiskConstants.SIZE_D81_80,
                DiskConstants.SIZE_D81_80E -> {
                    logger.info("found D81 disk.")
                    throw DiskException("D81 diskformat not supported yet!")
                }
            }
            // test for G64 disks
            val reader = BinaryDiskReader(imageFile)
            val buf = reader.readData(0, 8)
            val header = PETASCIIHelper.binary2ASCII(buf, 0, 8)
            if (header.startsWith("GCR-1541")) {
                logger.info("found G64 disk.")
                throw DiskException("G64 diskformat not supported yet!")
            }

            throw DiskException("Cannot read image file - diskformat unknown!")
        } else {
            throw FileNotFoundException("file <$imageFile> not found!")
        }
    }
}
