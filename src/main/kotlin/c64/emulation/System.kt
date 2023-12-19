package c64.emulation

import c64.emulation.cia.CIA
import c64.emulation.cia.Keyboard
import c64.emulation.cpu.CPU
import c64.emulation.cpu.Clock
import c64.emulation.cpu.Registers
import c64.emulation.memory.Memory
import c64.emulation.vic.VIC

/**
 * Class which holds all parts of the emulated machine (CPU, VIC, ...) and initialize them in
 * the right order.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
object System {

    internal var registers = Registers()
    internal var memory = Memory()
    internal var vic = VIC()
    internal var cia = CIA()
    internal var keyboard = Keyboard()
    internal var clock = Clock()
    // initialize CPU at last
    internal var cpu = CPU()

    init {
        cpu.initialize()
    }
}