package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.registers
import c64.emulation.cpu.AddressingMode

/**
 * Class collecting all "Shift" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class Shift {

    init {
        cpu.registerInstructionWithResult(0x06, ::opASL, AddressingMode.ZeroPage, 5)
        cpu.registerInstructionWithResult(0x0A, ::opASL, AddressingMode.Accumulator, 2)
        cpu.registerInstructionWithResult(0x0E, ::opASL, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0x16, ::opASL, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0x1E, ::opASL, AddressingMode.AbsoluteX, 7)
        cpu.registerInstructionWithResult(0x26, ::opROL, AddressingMode.ZeroPage, 5)
        cpu.registerInstructionWithResult(0x2A, ::opROL, AddressingMode.Accumulator, 2)
        cpu.registerInstructionWithResult(0x2E, ::opROL, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0x36, ::opROL, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0x3E, ::opROL, AddressingMode.AbsoluteX, 7)
        cpu.registerInstructionWithResult(0x46, ::opLSR, AddressingMode.ZeroPage, 5)
        cpu.registerInstructionWithResult(0x4A, ::opLSR, AddressingMode.Accumulator, 2)
        cpu.registerInstructionWithResult(0x4E, ::opLSR, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0x56, ::opLSR, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0x5E, ::opLSR, AddressingMode.AbsoluteX, 7)
        cpu.registerInstructionWithResult(0x66, ::opROR, AddressingMode.ZeroPage, 5)
        cpu.registerInstructionWithResult(0x6A, ::opROR, AddressingMode.Accumulator, 2)
        cpu.registerInstructionWithResult(0x6E, ::opROR, AddressingMode.Absolute, 6)
        cpu.registerInstructionWithResult(0x76, ::opROR, AddressingMode.ZeroPageX, 6)
        cpu.registerInstructionWithResult(0x7E, ::opROR, AddressingMode.AbsoluteX, 7)
    }

    /**
     * Rotate Left.
     */
    private fun opROL(value: UByte): UByte {
        // shift left by 1
        var result = value.toInt() shl 1
        // fill bit 0 with the value of the carry flag
        if (registers.C) {
            result = result or 0x1
        }
        val byteResult = result.toUByte()
        // save bit 8 in the carry flag...
        registers.C = result and 0x100 == 0x100
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Rotate Right.
     */
    private fun opROR(value: UByte): UByte {
        // save carry
        val carry = registers.C
        // move bit 0 in the carry flag...
        registers.C = value.toInt() and 0x01 == 0x01
        // shift right by 1
        var result = value.toInt() shr 1
        // fill bit 7 with the saved value of the carry flag
        if (carry) {
            result = result or 0b1000_0000
        }
        val byteResult = result.toUByte()
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Arithmetic Shift Left.
     */
    private fun opASL(value: UByte): UByte {
        // shift left by 1
        val result: Int = value.toInt() shl 1
        val byteResult = result.toUByte()
        // save bit 8 in the carry flag...
        registers.C = result and 0x100 == 0x100
        registers.setZeroFlagFromValue(byteResult)
        registers.setNegativeFlagFromValue(byteResult)
        return byteResult
    }

    /**
     * Logical Shift Right
     */
    private fun opLSR(value: UByte): UByte {
        // save bit 0 in the carry flag...
        registers.C = value.toInt() and 0x01 == 0x01
        // shift right by 1
        val byteResult = (value.toInt() shr 1).toUByte()
        registers.setZeroFlagFromValue(byteResult)
        // negative flag always 0 after this operation
        registers.N = false
        return byteResult
    }
}