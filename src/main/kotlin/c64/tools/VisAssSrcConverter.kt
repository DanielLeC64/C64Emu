package c64.tools

import c64.util.toHex
import c64.util.toUnprefixedHex
import mu.KLogging
import mu.KotlinLogging
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout
import java.io.File
import java.nio.file.Files

/**
 * Converter for C64 VisAss sourcefiles to text.
 *
 * @author schulted
 */
@ExperimentalUnsignedTypes
class VisAssSrcConverter {

    companion object : KLogging()

    private val logger = KotlinLogging.logger {}

    private val tokens: HashMap<String, String> = HashMap()

    init {
        // PatternLayout("%-5p - %m%n"))
        BasicConfigurator.configure(ConsoleAppender(PatternLayout("%m%n")))
        tokens["00"] = ""
        tokens["55"] = ""
        tokens["48"] = "£la "
        tokens["49"] = "£ba "
        tokens["4A"] = "£by "
        tokens["4B"] = "£br "
        tokens["4D"] = "£md "
        tokens["4E"] = "£de "
        tokens["4F"] = "£ma "
        tokens["52"] = "£on "
        tokens["01"] = "cpx "
        tokens["02"] = "cpy "
        tokens["03"] = "ldx "
        tokens["04"] = "ldy "
        tokens["05"] = "stx "
        tokens["06"] = "sty "
        tokens["16"] = "adc "
        tokens["17"] = "and "
        tokens["19"] = "bit "
        tokens["1A"] = "bcs "
        tokens["1B"] = "beq "
        tokens["1C"] = "bcc "
        tokens["1D"] = "bmi "
        tokens["1E"] = "bne "
        tokens["1F"] = "bpl "
        tokens["23"] = "clc "
        tokens["25"] = "cli "
        tokens["27"] = "cmp "
        tokens["28"] = "dec "
        tokens["29"] = "dex "
        tokens["2A"] = "dey "
        tokens["2B"] = "eor "
        tokens["2C"] = "inc "
        tokens["2D"] = "inx "
        tokens["2E"] = "iny "
        tokens["2F"] = "jmp "
        tokens["30"] = "jsr "
        tokens["31"] = "lda "
        tokens["32"] = "lsr "
        tokens["33"] = "nop "
        tokens["34"] = "ora "
        tokens["35"] = "pha "
        tokens["37"] = "pla "
        tokens["3A"] = "ror "
        tokens["3C"] = "rts "
        tokens["3D"] = "sbc "
        tokens["3E"] = "sec "
        tokens["40"] = "sei "
        tokens["41"] = "sta "
        tokens["42"] = "tax "
        tokens["43"] = "tay "
        tokens["45"] = "txa "
        tokens["47"] = "tya "
    }

    fun convertFile(srcFilePath: String) {
        logger.info { "start converting VisAss source file <$srcFilePath>..." }
        val srcFile = File(srcFilePath)
        val outFile = File("$srcFilePath.asm")
        if (srcFile.exists()) {
            val textBuffer = StringBuffer()
            val rawFile: UByteArray = Files.readAllBytes(srcFile.toPath()).toUByteArray()
            var i = 5
            while (i < rawFile.size) {
                val b1: UByte = rawFile[i]
                var b3: UByte = 0x00u
                if (b1.toInt() == 0xFF) {
                    // handle tokens
                    if (i < rawFile.size - 2) {
                        b3 = rawFile[i + 2]
                    }
                    if (tokens.containsKey(b3.toUnprefixedHex())) {
                        if (b3.toInt() > 0x00) {
                            textBuffer.append("\n")
                        }
                        if (b3 in 0x01u..0x47u) {
                            textBuffer.append("          ")
                        } else if (b3 in 0x048u..0x54u) {
                            textBuffer.append(" ")
                        }
                        textBuffer.append(tokens[b3.toUnprefixedHex()])
                        i += 2
                    } else {
                        textBuffer.append("\n UNKNOWN TOKEN CODE: ${b3.toHex()}\n")
                    }
                } else {
                    textBuffer.append(pet2acsii(b1))
                }
                i++
            }
            logger.info { textBuffer.toString() }
            Files.writeString(outFile.toPath(), textBuffer)
        }
        logger.info { "...done" }
    }

    private fun pet2acsii(byte: UByte): Char {
        // 01-1A
        if (byte in 0x01u..0x1Au) {
            return (byte.toInt() + 96).toChar()
        } else if (byte.toInt() == 0x1C) {
            return '£'
        } else if (byte.toInt() == 0x00) {
            return ' '
        }
        return byte.toInt().toChar()
    }
}

@ExperimentalUnsignedTypes
fun main() {
    VisAssSrcConverter().convertFile("workblock   .src.prg")
}