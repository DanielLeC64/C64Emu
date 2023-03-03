package c64.emulation.ui

import c64.emulation.BootstrapC64
import c64.emulation.System
import c64.emulation.System.keyboard
import c64.emulation.System.vic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.concurrent.schedule
import kotlin.concurrent.timer

/**
 * Simple UI to show the display of the emulator.
 *
 * @author Daniel Schulte 2017-2023
 */
@ExperimentalUnsignedTypes
class EmulatorUI {

    companion object {
        const val VIEWPORT_LEFT: Int = 11
        const val VIEWPORT_RIGHT: Int = 405
        const val VIEWPORT_TOP: Int = 0
        const val VIEWPORT_BOTTOM: Int = 270
        const val VIEWPORT_WIDTH: Int = VIEWPORT_RIGHT - VIEWPORT_LEFT
        const val VIEWPORT_HEIGHT: Int = VIEWPORT_BOTTOM - VIEWPORT_TOP
    }

    init {
        // PatternLayout("%-5p - %m%n"))
        BasicConfigurator.configure(ConsoleAppender(PatternLayout("%m%n")))

        val frame = JFrame()
        frame.layout = BorderLayout()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val bitmapPanel = JPanel()
        bitmapPanel.preferredSize = Dimension(VIEWPORT_WIDTH * 2, VIEWPORT_HEIGHT * 2)
        frame.contentPane.add(bitmapPanel, BorderLayout.CENTER)

        frame.isVisible = true
        frame.pack()
        frame.addKeyListener(keyboard)

        GlobalScope.launch {
            BootstrapC64()
        }

        // 20ms ~= 50 frames/s
        timer("display_refresh", false, 100, 20) {
            bitmapPanel.graphics.drawImage(vic.bitmapData, 0, 0,
                VIEWPORT_WIDTH * 2, VIEWPORT_HEIGHT * 2,
                VIEWPORT_LEFT, VIEWPORT_TOP, VIEWPORT_RIGHT, VIEWPORT_BOTTOM, frame)
        }

        // load basic test program
        Timer().schedule(3000) {
            System.memory.loadPrg("./test-src/c64/prg/s1 demo 1.prg")
        }
    }
}

@ExperimentalUnsignedTypes
fun main() {
    EmulatorUI()
}