package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.memory
import c64.emulation.System.registers
import c64.emulation.memory.Memory.Companion.checkForPageBoundaryCross

/**
 * Class collecting all "Branch" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class Branch {

    init {
        cpu.registerInstruction(0x10, ::opBPL)
        cpu.registerInstruction(0x30, ::opBMI)
        cpu.registerInstruction(0x50, ::opBVC)
        cpu.registerInstruction(0x70, ::opBVS)
        cpu.registerInstruction(0x90, ::opBCC)
        cpu.registerInstruction(0xB0, ::opBCS)
        cpu.registerInstruction(0xD0, ::opBNE)
        cpu.registerInstruction(0xF0, ::opBEQ)
    }

    /**
     * Branch if Not Equal (zero flag not set).
     */
    private fun opBNE() {
        branch(!registers.Z)
    }

    /**
     * Branch if equal (zero flag set).
     */
    private fun opBEQ() {
        branch(registers.Z)
    }

    /**
     * Branch if Carry Set.
     */
    private fun opBCS() {
        branch(registers.C)
    }

    /**
     * Branch if Carry Clear.
     */
    private fun opBCC() {
        branch(!registers.C)
    }

    /**
     * Branch if Positive (negative flag not set).
     */
    private fun opBPL() {
        branch(!registers.N)
    }

    /**
     * Branch if Minus (negative flag is set).
     */
    private fun opBMI() {
        branch(registers.N)
    }

    /**
     * Branch if overflow flag clear.
     */
    private fun opBVC() {
        branch(!registers.V)
    }

    /**
     * Branch if overflow flag set.
     */
    private fun opBVS() {
        branch(registers.V)
    }

    private fun branch(condition: Boolean) {
        // cycles: 2 (+1 if branch succeeds, +2 if to a new page)
        registers.cycles += 2
        if (condition) {
            registers.cycles++
            // attention: relative offset to the current PC is SIGNED!
            val relativeOffset = memory.fetchWithPC().toByte()
            val branchAddress = registers.PC + relativeOffset
            registers.cycles += checkForPageBoundaryCross(registers.PC, branchAddress)
            registers.PC = branchAddress
        } else {
            registers.PC++
        }
    }
}