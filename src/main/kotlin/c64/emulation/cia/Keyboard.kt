package c64.emulation.cia

import c64.emulation.System
import c64.emulation.System.cpu
import c64.emulation.System.memory
import c64.emulation.System.vic
import c64.emulation.cpu.CPU
import c64.emulation.vic.VIC
import mu.KotlinLogging
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

/**
 * Class which handles Keyboard input and translates the incoming keyCodes for the CIA.
 *
 * @author Daniel Schulte 2017-2024
 */
class Keyboard : KeyListener {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val SHIFT_CODE = 0xFF
        val keyboardTranslationMatrix = arrayOf(
            //         DEL   RET   C-RI  F7    F1    F3    F5    C-DO
            intArrayOf(0x08, 0x0A, 0x27, 0x76, 0x70, 0x72, 0x74, 0x28),
            //         3     W     A     4     Z     S     E     LSHI
            intArrayOf(0x33, 0x57, 0x41, 0x34, 0x5A, 0x53, 0x45, 0xFF),
            //         5     R     D     6     C     F     T     X
            intArrayOf(0x35, 0x52, 0x44, 0x36, 0x43, 0x46, 0x54, 0x58),
            //         7     Y     G     8     B     H     U     V
            intArrayOf(0x37, 0x59, 0x47, 0x38, 0x42, 0x48, 0x55, 0x56),
            //         9     I     J     0     M     K     O     N
            intArrayOf(0x39, 0x49, 0x4A, 0x30, 0x4D, 0x4B, 0x4F, 0x4E),
            //         +     P     L     -     .     :     @     ,
            intArrayOf(0x2B, 0x50, 0x4C, 0x2D, 0x2E, 0x3A, 0x40, 0x2C),
            //         £     *     ;     HOME  RSHI  =     ^     /
            intArrayOf(0x9C, 0x2A, 0x3B, 0x00, 0x00, 0x3D, 0x5E, 0x2F),
            //         1     <-    CTRL  2     SPACE C=    Q     RUN/STOP
            intArrayOf(0x31, 0x3C, 0x00, 0x32, 0x20, 0x00, 0x51, 0x1B)
        )
        val keyboardTranslation = hashMapOf(
            KeyEvent.VK_PLUS to 0x2B,
            KeyEvent.VK_LESS to 0x2C,
            KeyEvent.VK_NUMBER_SIGN to 0x33,
            KeyEvent.VK_LEFT to 0x27,
            KeyEvent.VK_RIGHT to 0x27,
            KeyEvent.VK_UP to 0x28,
            KeyEvent.VK_DOWN to 0x28,
            KeyEvent.VK_BACK_SPACE to 0x08,
            KeyEvent.VK_DEAD_CIRCUMFLEX to 0x3C,
            KeyEvent.VK_ESCAPE to 0x1B)
        val keyboardTranslationShiftState = hashSetOf(
            KeyEvent.VK_LESS, KeyEvent.VK_NUMBER_SIGN, KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_F2, KeyEvent.VK_F4,
            KeyEvent.VK_F6, KeyEvent.VK_F8)
        val keyboardShiftTranslation = hashMapOf(
            KeyEvent.VK_0 to 0x3D,
            KeyEvent.VK_7 to 0x2F,
            KeyEvent.VK_PERIOD to 0x3A,
            KeyEvent.VK_COMMA to 0x3B,
            KeyEvent.VK_PLUS to 0x2A,
            KeyEvent.VK_LESS to 0x2E,
            KeyEvent.VK_NUMBER_SIGN to 0x37)
        val keyboardShiftTranslationShiftState = hashSetOf(KeyEvent.VK_LESS, KeyEvent.VK_NUMBER_SIGN)
        val keyboardAltTranslationShiftState = hashSetOf(KeyEvent.VK_8, KeyEvent.VK_9)
        val keyboardAltTranslation = hashMapOf(
            KeyEvent.VK_8 to 0x3A,
            KeyEvent.VK_9 to 0x3B)
    }


    // todo: keys to translate:  C= SHIFT-LOCK CTRL CLR/HOME RESTORE ARROW-UP
    // todo: refactor and use charCode where possible

    private var shiftState: Int = -1
    private var lastKeyCode: Int = -1

    fun getDataPortB(dataPortA: UByte): UByte {
        var result = 0
        if (lastKeyCode > -1) {
            var column = 0
            val columns = ArrayList<Int>()
            var dataPortAInt = dataPortA.toInt() xor 0xFF
            // collect all columns to read
            while (dataPortAInt > 0) {
                if (dataPortAInt and 0x01 == 0x01) {
                    columns.add(column)
                }
                column++
                dataPortAInt = dataPortAInt shr 1
            }
            // find all pressed keys
            for (col in columns) {
                val row = keyboardTranslationMatrix[col]
                // find key code
                var keyIndex = row.indexOf(lastKeyCode)
                if (keyIndex > -1) {
                    result = result or (1 shl keyIndex)
                }
                // find shift
                keyIndex = row.indexOf(shiftState)
                if (keyIndex > -1) {
                    result = result or (1 shl keyIndex)
                }
            }
        }
        return (result xor 0xFF).toUByte()
    }

    override fun keyPressed(e: KeyEvent?) {
        if (e != null) {
            shiftState = -1
            lastKeyCode = e.keyCode
            val shiftDown = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK == InputEvent.SHIFT_DOWN_MASK
            val altDown = e.modifiersEx and InputEvent.ALT_DOWN_MASK == InputEvent.ALT_DOWN_MASK
            if (shiftDown) {
                if (lastKeyCode in KeyEvent.VK_A..KeyEvent.VK_Z ||
                    lastKeyCode in KeyEvent.VK_1..KeyEvent.VK_6 ||
                    lastKeyCode in KeyEvent.VK_8..KeyEvent.VK_9) {
                    // keys A-Z, 1-2, 4-6, 8-9
                    // ==> normal shift handling
                    shiftState = SHIFT_CODE
                }
                if (keyboardShiftTranslationShiftState.contains(lastKeyCode)) {
                    shiftState = SHIFT_CODE
                }
                if (keyboardShiftTranslation.containsKey(lastKeyCode)) {
                    lastKeyCode = keyboardShiftTranslation[lastKeyCode]!!
                }
                // special handling for ?
                if (e.keyChar == '?') {
                    shiftState = SHIFT_CODE
                    // use keyCode for / (because on c64 keyboard layout ? is on the / key)
                    lastKeyCode = 0x2F
                }
            }
            else if (altDown) {
                if (keyboardAltTranslationShiftState.contains(lastKeyCode)) {
                    shiftState = SHIFT_CODE
                }
                if (keyboardAltTranslation.containsKey(lastKeyCode)) {
                    lastKeyCode = keyboardAltTranslation[lastKeyCode]!!
                }
                if (lastKeyCode == KeyEvent.VK_R) {
                    // ALT-R -> soft reset
                    System.registers.PC = memory.fetchWord(CPU.RESET_VECTOR)
                }
                else if (lastKeyCode == KeyEvent.VK_INSERT) {
                    // ALT-INSERT -> paste clipboard
                    pasteFromClipboard()
                }
            }
            else {
                if (keyboardTranslationShiftState.contains(lastKeyCode)) {
                    shiftState = SHIFT_CODE
                }
                if (keyboardTranslation.containsKey(lastKeyCode)) {
                    lastKeyCode = keyboardTranslation[lastKeyCode]!!
                }
                // start debugger with PAUSE key
                if (lastKeyCode == KeyEvent.VK_PAUSE) {
                    cpu.debugger.debugging = true
                }
            }
        } else {
            lastKeyCode = -1
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        lastKeyCode = -1
    }

    override fun keyTyped(e: KeyEvent?) {
        // nothing
    }

    fun pasteFromClipboard() {
        try {
            val c: Transferable = Toolkit.getDefaultToolkit().systemClipboard.getContents(null)
            if (c.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val content: Any? = c.getTransferData(DataFlavor.stringFlavor)
                if (content != null) {
                    pasteText(content.toString())
                }
            }
        }
        catch (e: Exception) {
            logger.error {e}
        }
    }

    /**
     * Pastes the text from the clipboard on the C64 screen. This only works, if the system is in textmode.
     * The operation itself sets the screencodes directly in the screen-memory without triggering any keyboard events.
     */
    private fun pasteText(pastedText: String) {
        // characters for testing:  abc123 ABC!"#$%&()*+,-./:;<>=?@XXX abccdf
        logger.info {"pasting text <${pastedText}>"}
        val text = pastedText.replace(Regex("[^a-zA-Z0-9 !\"#\$%&()*+,-./:;<>=?@]"), "")
        if (text.isNotEmpty()) {
            val bitmapMode = memory.fetch(VIC.VIC_SCROLY) and 0b0010_0000u
            // do pasting only in textmode
            if (bitmapMode.toInt() == 0) {
                var screenMemoryAddress = vic.getVideoBankAddress() + vic.getScreenMemoryAddress()
                // find cursor position + screenmem address
                val row = memory.fetch(0xD6)
                val col = memory.fetch(0xD3)
                var cursorAddress = row.toInt() * 40 + col.toInt()
                // adjust screen memory address according to the cursor pos
                screenMemoryAddress += cursorAddress

                for (i in text.indices) {
                    memory.store(screenMemoryAddress + i, petscii2screencode(text[i]).code.toUByte())
                }
                // set cursor to the end of the pasted text
                cursorAddress += text.length
                // set row
                memory.store(0xD6, (1 + cursorAddress / 40).toUByte())
                // set col
                memory.store(0xD3, (cursorAddress % 40).toUByte())

                // TODO: 58732 ($E56C) 	Cursorposition (und Bildschirm- und Farb-RAM-Zeiger) setzen; Werte in Speicheradresse 211 ($D3), 214 ($D6) (Zeile, Spalte) vorgeben
                //memory.pushWordToStack(registers.PC)
                //memory.pushToStack(registers.getProcessorStatus())
                //registers.PC = 0xE56C
                //registers.PC = 0xFFF0
            }
        }
    }

    private fun petscii2screencode(char: Char): Char {
        return when (char) {
            in 'a'..'z' -> {
                char.minus(0x60)
            }
            '@' -> {
                Char(0)
            }
            else -> {
                char
            }
        }
    }
}