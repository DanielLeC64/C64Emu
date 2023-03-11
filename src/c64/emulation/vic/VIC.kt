package c64.emulation.vic

import c64.emulation.System.memory
import c64.emulation.System.registers
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Emulation of the C64 video chip VIC-II - MOS 6567/6569.
 * Emulation only for PAL!
 *
 * @author Daniel Schulte 2017-2023
 */
@ExperimentalUnsignedTypes
class VIC {

    // TODO: handle interrupt status in $D019
    // TODO: set used video-bank (bit 0+1) in $DD00 (CIA2) (+$DD02 Port A data direction register)

    companion object {
        val VIC_OFFSET = 0xD000
        val VIC_ADDRESS_SPACE = 0xD000..0xD02E
        val VIC_IO_AREA_SIZE = 47 //$D000-$D02E

        // top vertical blank area 0-15 (16px)
        const val V_BLANK_TOP: Int = 15
        // top border 16-50 (35px)
        const val BORDER_TOP: Int = 50
        const val SCREEN_BOTTOM: Int = 250
        // bottom border 251-299 (49px)
        const val BORDER_BOTTOM: Int = 299
        // bottom vertical blank area 300-311 (12px)
        //const val V_BLANK_BOTTOM: Int = 311

        // left horizontal blank area 0-75 (76px)
        const val H_BLANK_LEFT: Int = 75
        // left border 76-123 (48px)
        const val BORDER_LEFT: Int = 123
        const val SCREEN_RIGHT: Int = 443
        // right border 443-480 (37px)
        const val BORDER_RIGHT: Int = 480
        // right horizontal blank area 481-503 (23px)
        //const val H_BLANK_RIGHT: Int = 503

        const val PAL_RASTERLINES: Int = 312
        const val PAL_RASTERCOLUMNS: Int = 504
        // 504 columns with 8 column per cycle: 504 / 8 = 63 cycles
        const val PAL_CYCLES_PER_RASTERLINE: Int = 63
        const val PAL_CYCLES_PER_FRAME: Int = PAL_RASTERLINES * PAL_CYCLES_PER_RASTERLINE

        const val NTSC_RASTERLINES: Int = 263
        const val NTSC_CYCLES_PER_RASTERLINE: Int = 65

        ///////////////////////////////////////////
        // registers are from $D000-D3FF, repeating every 64bytes 16x ($D000, $D040, $D080,...)
        ///////////////////////////////////////////
        // Vertical Fine Scrolling and Control Register
        const val VIC_SCROLY = 0xD011
        // Read Current Raster Scan Line/Write Line to Compare for Raster IRQ
        const val VIC_RASTER = 0xD012
        // Horizontal Fine Scrolling and Control Register
        const val VIC_SCROLX = 0xD016
        // Chip Memory Control Register
        const val VIC_VMCSB = 0xD018
        // Border Color Register
        const val VIC_EXTCOL = 0xD020
        // Background Color 0
        const val VIC_BGCOL1 = 0xD021

        // color ram $D800-DBFF
        const val COLOR_RAM = 0xD800

        /*val COLOR_TABLE = arrayOf(
            0x000000, 0xFFFFFF, 0xAF2A29, 0x62D8CC, 0xB03FB6, 0x4AC64A, 0x3739C4, 0xE4ED4E,
            0xB6591C, 0x683808, 0xEA746C, 0x4D4D4D, 0x848484, 0xA6FA9E, 0x707CE6, 0xB6B6B5
        )*/
        val COLOR_TABLE = arrayOf(
            //black,  white,    red,      cyan,     purple,   green,    blue,     yellow
            0x000000, 0xFFFFFF, 0x924A40, 0x84C5CC, 0x9351B6, 0x72B14B, 0x483AAA, 0xD5DF7C,
            //orange, brown,    pink,    dark grey, gray,    lt green, lt blue,   lt gray
            0x99692D, 0x675200, 0xC18178, 0x606060, 0x8A8A8A, 0xB3EC91, 0x867ADE, 0xB3B3B3
        )
    }

    private data class RastererState(
        var charY: Int = 0,
        var textRow: Int = 0,
        var textRowAddr: Int = 0,
        var textCol: Int = 0,
        var colorRamRowAddress: Int = 0,
        var backgroundColor: Int = 0,
        var videoBankAddress: Int = 0,
        var bitmapAddress: Int = 0,
        var bitmapColorAddress: Int = 0,
        var bitmapColorRowAddress: Int = 0,
        var bitmapRowAddress: Int = 0,
        var screenMemoryAddress: Int = 0
    )

    private val vicRam: UByteArray
    val bitmapData: BufferedImage
    private var lastRasterLine: Int = 0
    private val rastererState = RastererState()

    init {
        logger.info { "init VIC" }
        bitmapData =
            BufferedImage(BORDER_RIGHT - H_BLANK_LEFT, BORDER_BOTTOM - V_BLANK_TOP, BufferedImage.TYPE_3BYTE_BGR)
        vicRam = UByteArray(VIC_IO_AREA_SIZE)
    }

    fun saveScreenshot(filename: String) {
        ImageIO.write(bitmapData, "png", File(filename))
    }

    /**
     * Stores a single byte at the given VIC address.
     */
    fun store(address: Int, byte: UByte) {
        // translate address to $00-$2E
        vicRam[address - VIC_OFFSET] = byte

        // todo - do we need this?
        /*when (address and 0x000F) {
            else -> {
                // todo - implementation
                logger.info { "missing IMPL for VIC:write ${address.toHex()}" }
                0x00.toUByte()
            }
        }*/
    }

    /**
     * Fetches a single byte from the given VIC address.
     */
    fun fetch(address: Int): UByte {
        // translate address to $00-$2E
        return vicRam[address - VIC_OFFSET]

        // todo - do we need this?
        // use only bit 0-4, mask out all higher bits
        /*when (address and 0x000F) {
            else -> {
                logger.info { "missing IMPL for VIC:read ${address.toHex()}" }
                return vicRam[address and 0x000F]
            }
        }*/
    }

    internal fun refresh() {
        // PAL systems (50Hz) uses ~312 rasterlines (means ~63 cycles per line), visible lines: 284 (16-299)
        // NTSC systems (60Hz) uses ~263 rasterlines (means ~65 cycles per line), visible lines: 235 (...)
        // calc current rasterline
        val line: Int = (registers.cycles.rem(PAL_CYCLES_PER_FRAME) / PAL_CYCLES_PER_RASTERLINE).toInt()
        if (lastRasterLine != line) {
            // new rasterline starts now
            // store new line positon in $D012 + $D011
            store(VIC_RASTER, line.toUByte())
            // TODO: store bit 8 of current line in bit 7 of $D011
            // raster last finished line to bitmap
            rasterLine(lastRasterLine)
            lastRasterLine = line
        }
    }

    private fun rasterLine(rasterline: Int) {
        val bitmapMode = fetch(VIC_SCROLY) and 0b0010_0000u
        val y: Int = rasterline - BORDER_TOP - 1
        val borderColor = COLOR_TABLE[fetch(VIC_EXTCOL).toInt() and 0b0000_1111]
        val isMulticolorMode = fetch(VIC_SCROLX).toInt() and 0b0001_0000 == 0b0001_0000

        // popuplate rasterState
        rastererState.charY = y.rem(8)
        rastererState.textRow = y / 8
        rastererState.textRowAddr = rastererState.textRow * 40
        rastererState.colorRamRowAddress = COLOR_RAM + rastererState.textRowAddr
        rastererState.backgroundColor = COLOR_TABLE[fetch(VIC_BGCOL1).toInt() and 0b0000_1111]
        rastererState.videoBankAddress = getVideoBankAddress()
        rastererState.bitmapAddress = getBitmapAddress()
        rastererState.bitmapColorAddress = getBitmapColorAddress()
        rastererState.bitmapColorRowAddress =
            rastererState.videoBankAddress + rastererState.bitmapColorAddress + rastererState.textRowAddr
        rastererState.bitmapRowAddress =
            rastererState.videoBankAddress + rastererState.bitmapAddress + rastererState.textRowAddr * 8 + rastererState.charY
        rastererState.screenMemoryAddress = getScreenMemoryAddress()

        for (rastercolumn in 0 until PAL_RASTERCOLUMNS) {
            var color: Int
            val x = rastercolumn - BORDER_LEFT - 1

            // popuplate rasterState
            rastererState.textCol = x / 8

            if (rasterline <= V_BLANK_TOP || rasterline > BORDER_BOTTOM ||
                rastercolumn <= H_BLANK_LEFT || rastercolumn > BORDER_RIGHT) {
                // blank area
                continue
            } else if (rasterline <= BORDER_TOP || rasterline > SCREEN_BOTTOM ||
                rastercolumn <= BORDER_LEFT || rastercolumn > SCREEN_RIGHT) {
                // outer border color
                color = borderColor
            } else {
                if (bitmapMode.toInt() == 0) {
                    // text-mode
                    // todo: SCROLX bit 4 - isMulticolorMode - multicolor text mode
                    // todo: SCROLY bit 6 - Extended Background Color Mode
                    // todo: SCROLX bit 3 - 38/40 column mode
                    // TODO: SCROLY bit 3 - 25/24 row mode 
                    // TODO: SCROLY bit 4 - screen on/off
                    color = rasterTextMode(x)
                } else {
                    // bitmap mode
                    if (!isMulticolorMode){
                        // hires bitmap mode
                        color = rasterHiresMode(x)
                    } else {
                        // multicolor bitmap mode
                        color = rasterColorMode(x)
                    }
                }
            }
            bitmapData.setRGB(rastercolumn - H_BLANK_LEFT - 1, rasterline - V_BLANK_TOP - 1, color)
        }
    }

    private fun rasterTextMode(x: Int): Int {
        val screenMemoryRowAddress = rastererState.videoBankAddress + rastererState.screenMemoryAddress + rastererState.textRowAddr
        val char = memory.fetch(screenMemoryRowAddress + rastererState.textCol)
        val charColor = COLOR_TABLE[memory.fetch(rastererState.colorRamRowAddress + rastererState.textCol).toInt() and 0b0000_1111]
        val fetchFromCharMemoryFunction = getFetchFromCharMemoryFunction()

        val charX = x and 0b0000_0111
        val rawCharData = fetchFromCharMemoryFunction(char.toInt() * 8 + rastererState.charY)
        val pixelMask: UByte = (0b1000_0000u shr charX).toUByte()
        return if (rawCharData and pixelMask == pixelMask)
            charColor
        else
            rastererState.backgroundColor
    }

    private fun rasterHiresMode(x: Int): Int {
        val pxBit = x and 0b0000_0111
        val colors = memory.fetch(rastererState.bitmapColorRowAddress + rastererState.textCol).toInt()
        // 8x8 colorblock - hi-nibble: pixel-color, lo-nibble: background-color
        val bgColor = COLOR_TABLE[colors and 0b0000_1111]
        val pxColor = COLOR_TABLE[colors and 0b1111_0000 shr 4]

        // get byte from bitmap
        val bitmapByte = memory.fetch(rastererState.bitmapRowAddress + rastererState.textCol * 8).toInt()
        // read the correct bit
        val bitTest = (0b1000_0000u shr pxBit).toInt()
        return if (bitmapByte and bitTest == bitTest) {
            pxColor
        } else {
            bgColor
        }
    }

    private fun rasterColorMode(x: Int): Int {
        val pxBit = x and 0b0000_0110
        val colors = memory.fetch(rastererState.bitmapColorRowAddress + rastererState.textCol).toInt()
        // 8x8 colorblock - hi-nibble: hi-color, lo-nibble: lo-color
        val lo_color = COLOR_TABLE[colors and 0b0000_1111]
        val hi_color = COLOR_TABLE[colors and 0b1111_0000 shr 4]
        val color_ram = COLOR_TABLE[memory.fetch(rastererState.colorRamRowAddress + rastererState.textCol).toInt() and 0b0000_1111]

        // get byte from bitmap
        val bitmapByte = memory.fetch(rastererState.bitmapRowAddress + rastererState.textCol * 8).toInt()
        // read the correct bit
        val bitTest = (0b1100_0000u shr pxBit).toInt()
        return when ((bitmapByte and bitTest) shr (6 - pxBit)) {
            0b00 -> {
                rastererState.backgroundColor
            }
            0b01 -> {
                hi_color
            }
            0b10 -> {
                lo_color
            }
            else -> {
                color_ram
            }
        }
    }

    private fun getVideoBank(): Int {
        return (memory.fetch(0xDD00) and 0b0000_0011u).toInt()
    }

    private fun getVideoBankAddress(): Int {
        // select video bank + translate address
        // vic bank
        // 0: bank 3 $C000-$FFFF
        // 1: bank 2 $8000-$BFFF
        // 2: bank 1 $4000-$7FFF
        // 3: bank 0 $0000-$3FFF
        return 0xC000 - getVideoBank() * 0x4000
    }

    private fun fetchFromVideoBank(address: Int): UByte {
        return memory.fetch(address + getVideoBankAddress())
    }

    private fun getFetchFromCharMemoryFunction(): (offset: Int) -> UByte {
        val vicBank = getVideoBank()
        val characterDotDataIndex = (fetch(VIC_VMCSB) and 0b0000_1110u).toInt() shr 1
        return if ((characterDotDataIndex == 2 || characterDotDataIndex == 3) && (vicBank == 3 || vicBank == 1)) {
            // get value from char ROM if
            // * VIC bank 0 or 2 is selected  AND
            // * char mem pointer is 2 or 3
            memory::fetchFromCharROM
        } else {
            ::fetchFromVideoBank
        }
    }

    private fun getScreenMemoryAddress(): Int {
        return 0x0400 * ((fetch(VIC_VMCSB) and 0b1111_0000u).toInt() shr 4)
    }

    private fun getBitmapAddress(): Int {
        // bit 3 of VIC_VMCSB controls start of the bitmap data
        return if (fetch(VIC_VMCSB).toInt() and 0b0000_01000 == 0b0000_01000) {
            0x2000
        } else {
            0x0000
        }
    }

    private fun getBitmapColorAddress(): Int {
        return getScreenMemoryAddress()
    }
}