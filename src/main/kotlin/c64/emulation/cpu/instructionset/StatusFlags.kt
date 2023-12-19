package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.registers

/**
 * Class collecting all "Status flag" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class StatusFlags {

    init {
        cpu.registerInstruction(0x18, ::opCLC)
        cpu.registerInstruction(0x38, ::opSEC)
        cpu.registerInstruction(0x58, ::opCLI)
        cpu.registerInstruction(0x78, ::opSEI)
        cpu.registerInstruction(0xB8, ::opCLV)
        cpu.registerInstruction(0xD8, ::opCLD)
        cpu.registerInstruction(0xF8, ::opSED)
    }

    /**
     * Clear carry flag
     */
    private fun opCLC() {
        // cycles: 2
        registers.C = false
        registers.cycles += 2
    }

    /**
     * Set carry flag
     */
    private fun opSEC() {
        // cycles: 2
        registers.C = true
        registers.cycles += 2
    }

    /**
     * Set interrupt disable flag
     */
    private fun opSEI() {
        // cycles: 2
        registers.I = true
        registers.cycles += 2
    }

    /**
     * Clear interrupt disable flag
     */
    private fun opCLI() {
        // cycles: 2
        registers.I = false
        registers.cycles += 2
    }

    /**
     * Clear decimal mode flag
     */
    private fun opCLD() {
        // cycles: 2
        registers.D = false
        registers.cycles += 2
    }

    /**
     * Set decimal mode flag
     */
    private fun opSED() {
        // cycles: 2
        registers.D = true
        registers.cycles += 2
    }

    /**
     * Clear overflow flag.
     */
    private fun opCLV() {
        // cycles: 2
        registers.V = false
        registers.cycles += 2

    }
}