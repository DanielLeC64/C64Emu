package c64.emulation.vic

import c64.emulation.System.memory
import c64.emulation.System.registers
import c64.util.toBinary
import c64.util.toHex
import mu.KotlinLogging
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val logger = KotlinLogging.logger {}

/**
 * Emulation of the C64 video chip VIC-II - MOS 6567/6569.
 * Emulation only for PAL!
 *
 * @author Daniel Schulte 2017-2024
 */
@OptIn(ExperimentalUnsignedTypes::class)
class VIC {

    // TODO: set used video-bank (bit 0+1) in $DD00 (CIA2) (+$DD02 Port A data direction register)

    companion object {
        val VIC_OFFSET = 0xD000
        val VIC_ADDRESS_SPACE = 0xD000..0xD3FF
        val VIC_IO_AREA_SIZE = 64 //$D000-$D03F

        // top vertical blank area 0-15 (16px)
        const val BORDER_TOP: Int = 16
        // top border 16-50 (35px)
        const val SCREEN_TOP: Int = 51
        const val SCREEN_BOTTOM: Int = 250
        // bottom border 251-299 (49px)
        const val BORDER_BOTTOM: Int = 299
        // bottom vertical blank area 300-311 (12px)

        // left horizontal blank area 0-75 (76px)
        const val BORDER_LEFT: Int = 76
        // left border 76-123 (48px)
        const val SCREEN_LEFT: Int = 124
        const val SCREEN_RIGHT: Int = 443
        // right border 443-480 (37px)
        const val BORDER_RIGHT: Int = 480
        // right horizontal blank area 481-503 (23px)

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
        // Sprite n Horizontal Position ($D000-D00F)
        const val VIC_SPRITE_X = 0xD000
        // Sprite n Vertical Position ($D000-D00F)
        const val VIC_SPRITE_Y = 0xD001
        // MSB x-coordinate sprite 0-7
        const val VIC_SPRITE_X_MSB = 0xD010
        // Vertical Fine Scrolling and Control Register
        const val VIC_SCROLY = 0xD011
        // Read Current Raster Scan Line/Write Line to Compare for Raster IRQ
        const val VIC_RASTER = 0xD012
        // Sprite Enable Register
        const val VIC_SPENA = 0xD015
        // Horizontal Fine Scrolling and Control Register
        const val VIC_SCROLX = 0xD016
        // Sprite Vertical Expansion Register
        const val VIC_YXPAND = 0xD017
        // Chip Memory Control Register
        const val VIC_VMCSB = 0xD018
        // Sprite Multicolor Registers
        const val VIC_SPMC = 0xD01C
        // Sprite Horizontal Expansion Register
        const val VIC_XXPAND = 0xD01D
        // Border Color Register
        const val VIC_EXTCOL = 0xD020
        // Background Color 0
        const val VIC_BGCOL0 = 0xD021
        // Background Color 1
        const val VIC_BGCOL1 = 0xD022
        // Background Color 2
        const val VIC_BGCOL2 = 0xD023
        // Background Color 3
        const val VIC_BGCOL3 = 0xD024
        // Sprite Multicolor Register 0
        const val VIC_SPMC0 = 0xD025
        // Sprite Multicolor Register 1
        const val VIC_SPMC1 = 0xD026
        // Sprite n Color Register ($D027-D02E)
        const val VIC_SPCOL = 0xD027

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
        var y: Int = 0,
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
            BufferedImage(BORDER_RIGHT - BORDER_LEFT + 1, BORDER_BOTTOM - BORDER_TOP + 1, BufferedImage.TYPE_3BYTE_BGR)
        vicRam = UByteArray(VIC_IO_AREA_SIZE)
    }

    fun saveScreenshot(filename: String) {
        ImageIO.write(bitmapData, "png", File(filename))
    }

    /**
     * Stores a single byte at the given VIC address.
     */
    fun store(address: Int, byte: UByte) {
        // translate address to $00-$3F
        vicRam[address and 0b0011_1111] = byte

        // this is only to log writing to yet unhandled VIC registers
        when (address) {
            // TODO: VIC_RASTER,
            0xD013, 0xD014,
            in 0xD019..0xD01B,
            in 0xD01E..0xD01F,
            in 0xD022..0xD026,
            in 0xD02F..0xD03F,
            -> {
                logger.info { "missing IMPL for VIC:write ${address.toHex()}: ${byte.toHex()} (${byte.toBinary()})" }
            }
            VIC_SCROLY -> {
                if (byte.toInt() and 0b1100_0111 > 0) {
                    logger.warn { "not handled BITS for VIC_SCROLY register: ${(byte and 0b1100_0111u).toBinary()}" }
                }
            }
            VIC_SCROLX -> {
                if (byte.toInt() and 0b0010_0000 > 0) {
                    logger.warn { "not handled BITS for VIC_SCROLX register: ${(byte and 0b0010_0000u).toBinary()}" }
                }
            }
        }
    }

    /**
     * Fetches a single byte from the given VIC address.
     */
    fun fetch(address: Int): UByte {
        // translate address to $00-$3F
        return vicRam[address and 0b0011_1111]
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
        // display enabled (DEN)
        val displayEnabled = fetch(VIC_SCROLY).toInt() and 0b0001_0000 == 0b0001_0000
        val bitmapMode = fetch(VIC_SCROLY) and 0b0010_0000u
        val borderColor = COLOR_TABLE[fetch(VIC_EXTCOL).toInt() and 0b0000_1111]
        val isMulticolorMode = fetch(VIC_SCROLX).toInt() and 0b0001_0000 == 0b0001_0000

        var screenTop = SCREEN_TOP
        var screenLeft = SCREEN_LEFT
        var screenBottom = SCREEN_BOTTOM
        var screenRight = SCREEN_RIGHT

        // 24 rows mode (RSEL)
        if (fetch(VIC_SCROLY).toInt() and 0b0000_1000 == 0b0000_0000) {
            // extend borders by 4px
            screenTop += 4
            screenBottom -= 4
        }
        // 38 columns mode (CSEL)
        if (fetch(VIC_SCROLX).toInt() and 0b0000_1000 == 0b0000_0000) {
            // extend left by 7px and right by 9px
            screenLeft += 7
            screenRight -= 9
        }

        // popuplate rasterState
        rastererState.y = rasterline - SCREEN_TOP
        rastererState.charY = rastererState.y.rem(8)
        rastererState.textRow = rastererState.y / 8
        rastererState.textRowAddr = rastererState.textRow * 40
        rastererState.colorRamRowAddress = COLOR_RAM + rastererState.textRowAddr
        rastererState.backgroundColor = COLOR_TABLE[fetch(VIC_BGCOL0).toInt() and 0b0000_1111]
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
            var scrolX = 0

            if (rasterline < BORDER_TOP || rasterline > BORDER_BOTTOM ||
                rastercolumn < BORDER_LEFT || (rastercolumn + scrolX) > BORDER_RIGHT) {
                // blank area
                continue
            } else if (!displayEnabled ||
                rasterline < screenTop || rasterline > screenBottom ||
                rastercolumn < screenLeft || rastercolumn > screenRight) {
                // outer border color
                color = borderColor
            } else {
                val x = rastercolumn - SCREEN_LEFT
                scrolX = fetch(VIC_SCROLX).toInt() and 0b0000_0111
                // popuplate rasterState
                rastererState.textCol = x / 8
                color = if (bitmapMode.toInt() == 0) {
                    // text-mode
                    // todo: SCROLX bit 4 - isMulticolorMode - multicolor text mode
                    // todo: SCROLY bit 6 - Extended Background Color Mode
                    rasterTextMode(x)
                } else {
                    // bitmap mode
                    if (!isMulticolorMode){
                        // hires bitmap mode
                        rasterHiresMode(x)
                    } else {
                        // multicolor bitmap mode
                        rasterColorMode(x)
                    }
                }
                color = rasterSprites(x, color)
            }
            bitmapData.setRGB(rastercolumn - BORDER_LEFT + scrolX, rasterline - BORDER_TOP, color)
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

    private fun rasterSprites(x: Int, color: Int): Int {

        var spriteColor = color
        for (spriteNum in 7 downTo 0) {
            // check whether sprite is visible
            if ((fetch(VIC_SPENA).toInt() shr spriteNum) and 0b0000_0001 == 0b0000_0001) {

                // get position
                var spritePosX = fetch(VIC_SPRITE_X + 2 * spriteNum).toInt()
                // add MSB for x-coordinate
                // first mask out MSB for spriteNum:  shr + and %0000_0001
                // then shift it by 8 to the left
                spritePosX += (((fetch(VIC_SPRITE_X_MSB).toInt() shr spriteNum) and 0b0000_0001) shl 8)
                var spritePosY = fetch(VIC_SPRITE_Y + 2 * spriteNum).toInt()

                // calc position in visible screen
                spritePosX -= 24
                spritePosY -= 50

                var spriteW = 24
                var spriteH = 21

                // Sprite Horizontal Expansion
                val xxpand = (fetch(VIC_XXPAND).toInt() shr spriteNum) and 0b0000_0001 == 0b0000_0001
                if (xxpand) {
                    spriteW *= 2
                }
                // Sprite Vertical Expansion
                val yxpand = (fetch(VIC_YXPAND).toInt() shr spriteNum) and 0b0000_0001 == 0b0000_0001
                if (yxpand) {
                    spriteH *= 2
                }

                if (spritePosX <= x && x < (spritePosX + spriteW) &&
                    spritePosY <= rastererState.y && rastererState.y < (spritePosY + spriteH))
                {
                    var spriteX = x - spritePosX
                    if (xxpand) {
                        spriteX /= 2
                    }
                    var spriteY = rastererState.y - spritePosY
                    if (yxpand) {
                        spriteY /= 2
                    }
                    // sprite pointer addresses from $07F8-$07FF (respecting videobank + block)
                    val spritePointerAddress = rastererState.videoBankAddress +
                            rastererState.screenMemoryAddress + 0x3F8 + spriteNum
                    val spriteAddress = rastererState.videoBankAddress + 64 * memory.fetch(spritePointerAddress).toInt()
                    // get byte from bitmap
                    val spriteByteAddress = spriteAddress + spriteY * 3 + spriteX / 8

                    val multiColorMode = (fetch(VIC_SPMC).toInt() shr spriteNum) and 0b0000_0001 == 0b0000_0001
                    if (multiColorMode) {
                        // multicolor mode
                        // get shiftCount, 6,4,2 or 0
                        val shiftPxBy = (spriteX.inv() and 0b0000_0110)
                        // read the correct bits (use only the two lowest bits)
                        val pxColorBits = (memory.fetch(spriteByteAddress).toInt() shr shiftPxBy) and 0b0000_0011
                        // %00 = transparent
                        if (pxColorBits == 0b0000_0010) {
                            // %10 = sprite-color
                            spriteColor = COLOR_TABLE[fetch(VIC_SPCOL + spriteNum).toInt() and 0b0000_1111]
                        } else if (pxColorBits == 0b0000_0001) {
                            // %01 = multicolor #0 (VIC_SPMC0)
                            spriteColor = COLOR_TABLE[fetch(VIC_SPMC0).toInt() and 0b0000_1111]
                        } else if (pxColorBits == 0b0000_0011) {
                            // %11 = multicolor #1 (VIC_SPMC1)
                            spriteColor = COLOR_TABLE[fetch(VIC_SPMC1).toInt() and 0b0000_1111]
                        }
                    }
                    else {
                        // hires mode
                        val pxBit = spriteX and 0b0000_0111
                        // read the correct bit
                        val bitTest = (0b1000_0000u shr pxBit).toInt()
                        if (memory.fetch(spriteByteAddress).toInt() and bitTest == bitTest) {
                            spriteColor = COLOR_TABLE[fetch(VIC_SPCOL + spriteNum).toInt() and 0b0000_1111]
                        }
                    }
                }
            }
        }

        return spriteColor
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