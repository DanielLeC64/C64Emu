package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.registers

/**
 * Class collecting all "Register Transfer" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class RegisterTransfers {

    init {
        cpu.registerInstruction(0x8A, ::opTXA)
        cpu.registerInstruction(0x98, ::opTYA)
        cpu.registerInstruction(0xA8, ::opTAY)
        cpu.registerInstruction(0xAA, ::opTAX)
    }

    /**
     * Transfer Accumulator to X
     */
    private fun opTAX() {
        // cycles: 2
        registers.cycles += 2
        registers.X = registers.A
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Transfer X to Accumulator
     */
    private fun opTXA() {
        // cycles: 2
        registers.cycles += 2
        registers.A = registers.X
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Transfer Accumulator to Y
     */
    private fun opTAY() {
        // cycles: 2
        registers.cycles += 2
        registers.Y = registers.A
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }

    /**
     * Transfer Y to Accumulator
     */
    private fun opTYA() {
        // cycles: 2
        registers.cycles += 2
        registers.A = registers.Y
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

}