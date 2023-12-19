package c64.emulation.disassemble

import c64.emulation.C64ExecutionException
import c64.emulation.System.memory
import c64.emulation.System.registers
import c64.emulation.cpu.AddressingMode
import c64.util.toHex
import c64.util.toUnprefixedHex


/**
 * Class wich can be used to disassemble code.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class Disassembly {

    class DisassembleInfo(val op: String, val addrMode: AddressingMode)

    companion object {
        // table with all debugging methods indexed with their opcode
        private val INSTRUCTION_TABLE = arrayOfNulls<DisassembleInfo>(0x100)
    }

    var startDisassemblerAt: Int = 0

    // should the executed code printed as disassembly
    var printDisassembledCode: Boolean = false

    init {
        // initialize debug table
        INSTRUCTION_TABLE[0x00] = DisassembleInfo("BRK", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x01] = DisassembleInfo("ORA", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0x05] = DisassembleInfo("ORA", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x06] = DisassembleInfo("ASL", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x08] = DisassembleInfo("PHP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x09] = DisassembleInfo("ORA", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x0A] = DisassembleInfo("ASL", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x0D] = DisassembleInfo("ORA", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x0E] = DisassembleInfo("ASL", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x10] = DisassembleInfo("BPL", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x11] = DisassembleInfo("ORA", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0x15] = DisassembleInfo("ORA", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x16] = DisassembleInfo("ASL", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x18] = DisassembleInfo("CLC", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x19] = DisassembleInfo("ORA", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0x1D] = DisassembleInfo("ORA", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x1E] = DisassembleInfo("ASL", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x20] = DisassembleInfo("JSR", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x21] = DisassembleInfo("AND", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0x24] = DisassembleInfo("BIT", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x25] = DisassembleInfo("AND", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x26] = DisassembleInfo("ROL", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x28] = DisassembleInfo("PLP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x29] = DisassembleInfo("AND", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x2A] = DisassembleInfo("ROL", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x2C] = DisassembleInfo("BIT", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x2D] = DisassembleInfo("AND", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x2E] = DisassembleInfo("ROL", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x30] = DisassembleInfo("BMI", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x31] = DisassembleInfo("AND", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0x35] = DisassembleInfo("AND", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x36] = DisassembleInfo("ROL", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x38] = DisassembleInfo("SEC", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x39] = DisassembleInfo("AND", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0x3D] = DisassembleInfo("AND", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x3E] = DisassembleInfo("ROL", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x40] = DisassembleInfo("RTI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x41] = DisassembleInfo("EOR", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0x45] = DisassembleInfo("EOR", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x46] = DisassembleInfo("LSR", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x48] = DisassembleInfo("PHA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x49] = DisassembleInfo("EOR", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x4A] = DisassembleInfo("LSR", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x4C] = DisassembleInfo("JMP", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x4D] = DisassembleInfo("EOR", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x4E] = DisassembleInfo("LSR", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x50] = DisassembleInfo("BVC", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x51] = DisassembleInfo("EOR", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0x55] = DisassembleInfo("EOR", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x56] = DisassembleInfo("LSR", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x58] = DisassembleInfo("CLI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x59] = DisassembleInfo("EOR", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0x5D] = DisassembleInfo("EOR", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x5E] = DisassembleInfo("LSR", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x60] = DisassembleInfo("RTS", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x61] = DisassembleInfo("ADC", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0x65] = DisassembleInfo("ADC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x66] = DisassembleInfo("ROR", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0x68] = DisassembleInfo("PLA", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x69] = DisassembleInfo("ADC", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0x6A] = DisassembleInfo("ROR", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x6D] = DisassembleInfo("ADC", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x6C] = DisassembleInfo("JMP", AddressingMode.Indirect)
        INSTRUCTION_TABLE[0x6E] = DisassembleInfo("ROR", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0x70] = DisassembleInfo("BVS", AddressingMode.Relative)
        INSTRUCTION_TABLE[0x71] = DisassembleInfo("ADC", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0x75] = DisassembleInfo("ADC", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x76] = DisassembleInfo("ROR", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0x78] = DisassembleInfo("SEI", AddressingMode.Implied)
        INSTRUCTION_TABLE[0x79] = DisassembleInfo("ADC", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0x7D] = DisassembleInfo("ADC", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0x7E] = DisassembleInfo("ROR", AddressingMode.AbsoluteX)
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
        INSTRUCTION_TABLE[0xC6] = DisassembleInfo("DEC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xC8] = DisassembleInfo("INY", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xC9] = DisassembleInfo("CMP", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xC0] = DisassembleInfo("CPY", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xCA] = DisassembleInfo("DEX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xCC] = DisassembleInfo("CPY", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xCD] = DisassembleInfo("CMP", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xCE] = DisassembleInfo("DEC", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xD1] = DisassembleInfo("CMP", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0xD5] = DisassembleInfo("CMP", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xD6] = DisassembleInfo("DEC", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xD8] = DisassembleInfo("CLD", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xD9] = DisassembleInfo("CMP", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0xD0] = DisassembleInfo("BNE", AddressingMode.Relative)
        INSTRUCTION_TABLE[0xDD] = DisassembleInfo("CMP", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xDE] = DisassembleInfo("DEC", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xE0] = DisassembleInfo("CPX", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xE1] = DisassembleInfo("SBC", AddressingMode.IndexedIndirectX)
        INSTRUCTION_TABLE[0xE4] = DisassembleInfo("CPX", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xE5] = DisassembleInfo("SBC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xE6] = DisassembleInfo("INC", AddressingMode.ZeroPage)
        INSTRUCTION_TABLE[0xE8] = DisassembleInfo("INX", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xE9] = DisassembleInfo("SBC", AddressingMode.Immediate)
        INSTRUCTION_TABLE[0xEA] = DisassembleInfo("NOP", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xEC] = DisassembleInfo("CPX", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xED] = DisassembleInfo("SBC", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xEE] = DisassembleInfo("INC", AddressingMode.Absolute)
        INSTRUCTION_TABLE[0xF0] = DisassembleInfo("BEQ", AddressingMode.Relative)
        INSTRUCTION_TABLE[0xF1] = DisassembleInfo("SBC", AddressingMode.IndirectIndexedY)
        INSTRUCTION_TABLE[0xF5] = DisassembleInfo("SBC", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xF6] = DisassembleInfo("INC", AddressingMode.ZeroPageX)
        INSTRUCTION_TABLE[0xF8] = DisassembleInfo("SED", AddressingMode.Implied)
        INSTRUCTION_TABLE[0xF9] = DisassembleInfo("SBC", AddressingMode.AbsoluteY)
        INSTRUCTION_TABLE[0xFD] = DisassembleInfo("SBC", AddressingMode.AbsoluteX)
        INSTRUCTION_TABLE[0xFE] = DisassembleInfo("INC", AddressingMode.AbsoluteX)
    }

    fun checkStatus() {
        // check whether disassembly should be printed
        printDisassembledCode = printDisassembledCode || registers.PC == startDisassemblerAt
    }

    @Throws(C64ExecutionException::class)
    fun disassemble(opcode: UByte): String {
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