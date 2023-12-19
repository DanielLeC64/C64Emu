package c64.emulation

import mu.KotlinLogging

/**
 * Bootstrapper for C64 emulator.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class BootstrapC64 {

    private val logger = KotlinLogging.logger {}

    init {
        logger.info { "booting c64 system" }
        System.cpu.reset()

        // TODO: test, see http://visual6502.org/wiki/index.php?title=6502TestPrograms
        //memory.loadIntoRam("./roms/6502_functional_test.bin")
        //registers.PC = 0x0400
        System.cpu.runMachine()
    }
}

@ExperimentalUnsignedTypes
fun main (args : Array<String>) {
    BootstrapC64()
}