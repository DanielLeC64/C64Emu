package c64.emulation.disassemble

import c64.emulation.C64ExecutionException
import c64.emulation.Memory
import c64.emulation.Registers
import c64.util.toHex
import c64.util.toUnprefixedHex

/**
 * Class wich can be used to disassemble code.
 *
 * @author schulted
 */
@ExperimentalUnsignedTypes
class Disassembly(private var registers: Registers, private var memory: Memory) {

    class DisassembleInfo(val op: String, val addrMode: AddressingMode)

    enum class AddressingMode(val numByte: Int) {
        Implied(1), ZeroPage(2), ZeroPageX(2), ZeroPageY(2),
        Immediate(2), Relative(2), Indirect(3),
        Absolute(3), AbsoluteX(3), AbsoluteY(3),
        IndirectIndexedY(2), IndexedIndirectX(2)

    }

    companion object {
        // table with all debugging methods indexed with their opcode
        val INSTRUCTION_TABLE = arrayOfNulls<DisassembleInfo>(0x100)
    }

    // current opcode
    private var opcode: UByte = 0x00u

    init {
        // initialize debug table
        INSTRUCTION_TABLE[0x00] = DisassembleInfo("BRK", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x08] = DisassembleInfo("PHP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x09] = DisassembleInfo("ORA", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x0A] = DisassembleInfo("ASL", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x0D] = DisassembleInfo("ORA", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x10] = DisassembleInfo("BPL", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x18] = DisassembleInfo("CLC", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x20] = DisassembleInfo("JSR", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x24] = DisassembleInfo("BIT", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x2C] = DisassembleInfo("BIT", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x28] = DisassembleInfo("PLP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x29] = DisassembleInfo("AND", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x2A] = DisassembleInfo("ROL", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x30] = DisassembleInfo("BMI", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x38] = DisassembleInfo("SEC", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x40] = DisassembleInfo("RTI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x48] = DisassembleInfo("PHA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x49] = DisassembleInfo("EOR", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x4A] = DisassembleInfo("LSR", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x4C] = DisassembleInfo("JMP", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x50] = DisassembleInfo("BVC", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x55] = DisassembleInfo("EOR", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x58] = DisassembleInfo("CLI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x60] = DisassembleInfo("RTS", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x65] = DisassembleInfo("ADC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x68] = DisassembleInfo("PLA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x69] = DisassembleInfo("ADC", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x6C] = DisassembleInfo("JMP", AddressingMode.Indirect)
        INSTRUCTION_TABLE[0x70] = DisassembleInfo("BVS", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x78] = DisassembleInfo("SEI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x81] = DisassembleInfo("STA", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0x84] = DisassembleInfo("STY", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x85] = DisassembleInfo("STA", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x86] = DisassembleInfo("STX", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x88] = DisassembleInfo("DEY", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x8A] = DisassembleInfo("TXA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x8C] = DisassembleInfo("STY", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x8D] = DisassembleInfo("STA", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x8E] = DisassembleInfo("STX", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x90] = DisassembleInfo("BCC", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x91] = DisassembleInfo("STA", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0x94] = DisassembleInfo("STY", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x95] = DisassembleInfo("STA", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x96] = DisassembleInfo("STX", AddressingMode.ZeroPageY)
        INSTRUCTION_TABLE[0x98] = DisassembleInfo("TYA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x99] = DisassembleInfo("STA", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0x9A] = DisassembleInfo("TXS", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x9D] = DisassembleInfo("STA", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xA0] = DisassembleInfo("LDY", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xA1] = DisassembleInfo("LDA", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0xA2] = DisassembleInfo("LDX", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xA4] = DisassembleInfo("LDY", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xA5] = DisassembleInfo("LDA", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xA6] = DisassembleInfo("LDX", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xA9] = DisassembleInfo("LDA", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xA8] = DisassembleInfo("TAY", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xAA] = DisassembleInfo("TAX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xAC] = DisassembleInfo("LDY", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xAD] = DisassembleInfo("LDA", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xAE] = DisassembleInfo("LDX", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xB0] = DisassembleInfo("BCS", AddressingMode.Relative)
        INSTRUCTION_TABLE[0xB1] = DisassembleInfo("LDA", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0xB4] = DisassembleInfo("LDY", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xB5] = DisassembleInfo("LDA", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xB6] = DisassembleInfo("LDX", AddressingMode.ZeroPageY)
        INSTRUCTION_TABLE[0xB9] = DisassembleInfo("LDA", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0xB8] = DisassembleInfo("CLV", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xBA] = DisassembleInfo("TSX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xBC] = DisassembleInfo("LDY", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xBD] = DisassembleInfo("LDA", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xBE] = DisassembleInfo("LDX", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0xC1] = DisassembleInfo("CMP", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0xC4] = DisassembleInfo("CPY", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xC5] = DisassembleInfo("CMP", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xC8] = DisassembleInfo("INY", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xC9] = DisassembleInfo("CMP", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xC0] = DisassembleInfo("CPY", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xCA] = DisassembleInfo("DEX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xCC] = DisassembleInfo("CPY", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xCD] = DisassembleInfo("CMP", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xD1] = DisassembleInfo("CMP", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0xD5] = DisassembleInfo("CMP", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xD8] = DisassembleInfo("CLD", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xD9] = DisassembleInfo("CMP", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0xD0] = DisassembleInfo("BNE", AddressingMode.Relative)
        INSTRUCTION_TABLE[0xDD] = DisassembleInfo("CMP", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xE0] = DisassembleInfo("CPX", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xE4] = DisassembleInfo("CPX", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xE6] = DisassembleInfo("INC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xE8] = DisassembleInfo("INX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xE9] = DisassembleInfo("SBC", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xEA] = DisassembleInfo("NOP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xEC] = DisassembleInfo("CPX", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xF0] = DisassembleInfo("BEQ", AddressingMode.Relative)
        INSTRUCTION_TABLE[0xF8] = DisassembleInfo("SED", AddressingMode.Implied)
    }

    @Throws(C64ExecutionException::class)
    fun disassemble(opcode: UByte): String {
        this.opcode = opcode
        val info = INSTRUCTION_TABLE[opcode.toInt()]
        if (info != null) {
            val addr = (registers.PC - 1).toUnprefixedHex()
            val bytes = memory.printMemoryLine(registers.PC - 1, info.addrMode.numByte).padEnd(9)
            val args: String = when (info.addrMode) {
                AddressingMode.ZeroPage -> getArgsForZeroPage()
                AddressingMode.ZeroPageX -> getArgsForZeroPageX()
                AddressingMode.ZeroPageY -> getArgsForZeroPageY()
                AddressingMode.Immediate -> getArgsForImmediate()
                AddressingMode.Absolute -> getArgsForAbsolute()
                AddressingMode.AbsoluteX -> getArgsForAbsoluteX()
                AddressingMode.AbsoluteY -> getArgsForAbsoluteY()
                AddressingMode.Indirect -> getArgsForIndirect()
                AddressingMode.Relative -> getArgsForRelative()
                AddressingMode.IndirectIndexedY -> getArgsForIndirectIndexedY()
                AddressingMode.IndexedIndirectX -> getArgsForIndexedIndirectX()
                else -> {
                    ""
                }
            }
            return "$addr: $bytes ${info.op} $args"
        }
        else {
            throw C64ExecutionException("Missing disassembly info for opcode $opcode")
        }
    }

    /**
     * Returns the arguments for ops with immediate addressing mode.
     */
    private fun getArgsForImmediate(): String {
        return "#${memory.fetch(registers.PC).toHex()}"
    }

    /**
     * Returns the arguments for ops with indirect addressing mode.
     */
    private fun getArgsForIndirect(): String {
        return "(${memory.fetchWord(registers.PC).toHex()})"
    }

    /**
     * Returns the arguments for ops with zero-page addressing mode.
     */
    private fun getArgsForZeroPage(): String {
        return memory.fetch(registers.PC).toHex()
    }

    /**
     * Returns the arguments for ops with zero-page,X-indexed addressing mode.
     */
    private fun getArgsForZeroPageX(): String {
        return "${memory.fetch(registers.PC).toHex()},X"
    }

    /**
     * Returns the arguments for ops with zero-page,Y-indexed addressing mode.
     */
    private fun getArgsForZeroPageY(): String {
        return "${memory.fetch(registers.PC).toHex()},Y"
    }

    /**
     * Returns the arguments for ops with absolute addressing mode.
     */
    private fun getArgsForAbsolute(): String {
        return memory.fetchWord(registers.PC).toHex()
    }

    /**
     * Returns the arguments for ops with absolute,X addressing mode.
     */
    private fun getArgsForAbsoluteX(): String {
        return "${memory.fetchWord(registers.PC).toHex()},X"
    }

    /**
     * Returns the arguments for ops with absolute,Y addressing mode.
     */
    private fun getArgsForAbsoluteY(): String {
        return "${memory.fetchWord(registers.PC).toHex()},Y"
    }

    /**
     * Returns the arguments for ops with indirect indexed addressing mode.
     */
    private fun getArgsForIndirectIndexedY(): String {
        return "(${memory.fetch(registers.PC).toHex()}),Y"
    }

    /**
     * Returns the arguments for ops with indexed indirect addressing mode.
     */
    private fun getArgsForIndexedIndirectX(): String {
        return "(${memory.fetch(registers.PC).toHex()},X)"
    }

    /**
     * Returns the arguments for ops with relative addressing mode.
     */
    private fun getArgsForRelative(): String {
        val relativeOffset = memory.fetch(registers.PC).toByte()
        val branchAddress = registers.PC + 1 + relativeOffset
        return branchAddress.toHex()
    }
}