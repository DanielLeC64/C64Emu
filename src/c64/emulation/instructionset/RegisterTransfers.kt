package c64.emulation.instructionset

import c64.emulation.CPU
import c64.emulation.Memory
import c64.emulation.Registers

/**
 * Class collecting all "Register Transfer" instructions.
 *
 * @author schulted 2017-2018
 */
@ExperimentalUnsignedTypes
class RegisterTransfers(cpu: CPU, private var registers: Registers, @Suppress("unused") private var memory: Memory) {

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