package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.registers
import c64.emulation.cpu.AddressingMode

/**
 * Class collecting all "Increment / Decrement" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class IncrementsDecrements {

    init {
        cpu.registerInstruction(0x88, ::opDEY)
        cpu.registerInstructionWithResult(0xC6, ::opDEC, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0xC8, ::opINY)
        cpu.registerInstruction(0xCA, ::opDEX)
        cpu.registerInstructionWithResult(0xCE, ::opDEC, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0xD6, ::opDEC, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0xDE, ::opDEC, AddressingMode.AbsoluteX, 7)
        cpu.registerInstructionWithResult(0xE6, ::opINC, AddressingMode.ZeroPage, 5)
        cpu.registerInstruction(0xE8, ::opINX)
        cpu.registerInstructionWithResult(0xEE, ::opINC, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0xF6, ::opINC, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0xFE, ::opINC, AddressingMode.AbsoluteX, 7)
    }

    /**
     * Increment memory
     */
    private fun opINC(value: UByte): UByte {
        val result = (value + 1u).toUByte()
        registers.setZeroFlagFromValue(result)
        registers.setNegativeFlagFromValue(result)
        return result
    }

    /**
     * Increment X Register
     */
    private fun opINX() {
        // cycles: 2
        registers.X++
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Increment Y Register
     */
    private fun opINY() {
        // cycles: 2
        registers.Y++
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }

    /**
     * Decrement memory
     */
    private fun opDEC(value: UByte): UByte {
        val result = (value - 1u).toUByte()
        registers.setZeroFlagFromValue(result)
        registers.setNegativeFlagFromValue(result)
        return result
    }

    /**
     * Decrement the X register
     */
    private fun opDEX() {
        // cycles: 2
        registers.X--
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Decrement the Y register
     */
    private fun opDEY() {
        // cycles: 2
        registers.Y--
        registers.cycles += 2
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }
}