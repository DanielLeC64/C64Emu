package c64.emulation.ui

import c64.emulation.BootstrapC64
import c64.emulation.System
import c64.emulation.System.keyboard
import c64.emulation.System.vic
import c64.emulation.vic.VIC
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KLogging
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.PatternLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.Timer
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

    companion object : KLogging()

    init {
        // PatternLayout("%-5p - %m%n"))
        BasicConfigurator.configure(ConsoleAppender(PatternLayout("%m%n")))

        val frame = JFrame()
        frame.layout = BorderLayout()
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        val bitmapPanel = JPanel()
        // TODO: set visible screen area = viewport, e.g. x=76-470 (394px)  and y=16-286 (270px)
        // TODO: calculate from viewport values
        bitmapPanel.preferredSize = Dimension(VIC.PAL_RASTERCOLUMNS * 2, VIC.PAL_RASTERLINES * 2)
        frame.contentPane.add(bitmapPanel, BorderLayout.CENTER)

        frame.isVisible = true
        frame.pack()
        frame.addKeyListener(keyboard)

        GlobalScope.launch {
            BootstrapC64()
        }

        // 20ms ~= 50 frames/s
        timer("display_refresh", false, 100, 20) {
            // TODO: calculate from viewport values
            bitmapPanel.graphics.drawImage(vic.bitmapData, 0, 0,
                vic.bitmapData.width * 2, vic.bitmapData.height * 2, frame)
        }

        // load basic test program
        Timer().schedule(3000) {
            System.memory.loadPrg("./test-src/c64/prg/interrupt1.prg")
        }
    }
}

@ExperimentalUnsignedTypes
fun main() {
    EmulatorUI()
}