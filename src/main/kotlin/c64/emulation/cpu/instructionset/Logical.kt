package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.registers
import c64.emulation.cpu.AddressingMode

/**
 * Class collecting all "Logical" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class Logical {

    init {
        cpu.registerInstruction(0x01, ::opORA, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0x05, ::opORA, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0x09, ::opORA, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0x0D, ::opORA, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0x11, ::opORA, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0x15, ::opORA, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0x19, ::opORA, AddressingMode.AbsoluteY, 4)
        cpu.registerInstruction(0x1D, ::opORA, AddressingMode.AbsoluteX, 4)
        cpu.registerInstruction(0x21, ::opAND, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0x24, ::opBIT, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0x25, ::opAND, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0x29, ::opAND, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0x2C, ::opBIT, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0x2D, ::opAND, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0x31, ::opAND, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0x35, ::opAND, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0x39, ::opAND, AddressingMode.AbsoluteY, 4)
        cpu.registerInstruction(0x3D, ::opAND, AddressingMode.AbsoluteX, 4)
        cpu.registerInstruction(0x41, ::opEOR, AddressingMode.IndexedIndirectX, 6)
        cpu.registerInstruction(0x45, ::opEOR, AddressingMode.ZeroPage, 3)
        cpu.registerInstruction(0x49, ::opEOR, AddressingMode.Immediate, 2)
        cpu.registerInstruction(0x4D, ::opEOR, AddressingMode.Absolute, 4)
        cpu.registerInstruction(0x51, ::opEOR, AddressingMode.IndirectIndexedY, 5)
        cpu.registerInstruction(0x55, ::opEOR, AddressingMode.ZeroPageX, 4)
        cpu.registerInstruction(0x59, ::opEOR, AddressingMode.AbsoluteY, 4)
        cpu.registerInstruction(0x5D, ::opEOR, AddressingMode.AbsoluteX, 4)
    }

    /**
     * Logical AND
     */
    private fun opAND(value: UByte) {
        registers.A = registers.A and value
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Logical Inclusive OR
     */
    private fun opORA(value: UByte) {
        registers.A = registers.A or value
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Exclusive OR
     */
    private fun opEOR(value: UByte) {
        registers.A = registers.A xor value
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Bit Test
     */
    private fun opBIT(value: UByte) {
        // overflow flag get's bit 6 of operand
        registers.V = (value.toInt() and 0b0100_0000) == 0b0100_0000
        // negative flag get's bit 7 of operand
        registers.N = (value.toInt() and 0b1000_0000) == 0b1000_0000
        // set zero flag from result of accu AND operand
        registers.setZeroFlagFromValue(registers.A and value)
    }
}