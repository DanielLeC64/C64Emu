package c64.emulation.cpu.instructionset

import c64.emulation.System.cpu
import c64.emulation.System.memory
import c64.emulation.System.registers

/**
 * Class collecting all "Load / Store" instructions.
 *
 * @author Daniel Schulte 2017-2019
 */
@ExperimentalUnsignedTypes
class LoadStore {
    
    init {
        cpu.registerInstruction(0x81, ::opSTA)
        cpu.registerInstruction(0x84, ::opSTY)
        cpu.registerInstruction(0x85, ::opSTA)
        cpu.registerInstruction(0x86, ::opSTX)
        cpu.registerInstruction(0x8C, ::opSTY)
        cpu.registerInstruction(0x8D, ::opSTA)
        cpu.registerInstruction(0x8E, ::opSTX)
        cpu.registerInstruction(0x91, ::opSTA)
        cpu.registerInstruction(0x94, ::opSTY)
        cpu.registerInstruction(0x95, ::opSTA)
        cpu.registerInstruction(0x96, ::opSTX)
        cpu.registerInstruction(0x99, ::opSTA)
        cpu.registerInstruction(0x9D, ::opSTA)
        cpu.registerInstruction(0xA0, ::opLDY)
        cpu.registerInstruction(0xA1, ::opLDA)
        cpu.registerInstruction(0xA2, ::opLDX)
        cpu.registerInstruction(0xA4, ::opLDY)
        cpu.registerInstruction(0xA5, ::opLDA)
        cpu.registerInstruction(0xA6, ::opLDX)
        cpu.registerInstruction(0xA9, ::opLDA)
        cpu.registerInstruction(0xAC, ::opLDY)
        cpu.registerInstruction(0xAD, ::opLDA)
        cpu.registerInstruction(0xAE, ::opLDX)
        cpu.registerInstruction(0xB1, ::opLDA)
        cpu.registerInstruction(0xB4, ::opLDY)
        cpu.registerInstruction(0xB5, ::opLDA)
        cpu.registerInstruction(0xB6, ::opLDX)
        cpu.registerInstruction(0xB9, ::opLDA)
        cpu.registerInstruction(0xBC, ::opLDY)
        cpu.registerInstruction(0xBD, ::opLDA)
        cpu.registerInstruction(0xBE, ::opLDX)
    }
    
    /**
     * Load accumulator with memory
     */
    private fun opLDA() {
        when (cpu.currentOpcode.toInt()) {
            0xA1 -> {
                // addressing mode: (indirect,x)
                // cycles: 6
                registers.A = memory.fetchIndexedIndirectXWithPC()
                registers.cycles += 6
            }
            0xA5 -> {
                // addressing mode: zeropage
                // cycles: 3
                registers.A = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0xA9 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.A = memory.fetchWithPC()
                registers.cycles += 2
            }
            0xAD -> {
                // addressing mode: absolute
                // cycles: 4
                registers.A = memory.fetchAbsoluteWithPC()
                registers.cycles += 4
            }
            0xB1 -> {
                // addressing mode: (indirect), y
                // cycles: 5 (+1 page boundary cross)
                registers.A = memory.fetchIndirectIndexedYWithPC()
                registers.cycles += 5
            }
            0xB5 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                registers.A = memory.fetchZeroPageXWithPC()
                registers.cycles += 4
            }
            0xB9 -> {
                // addressing mode: absolute, y
                // cycles: 4* (+1 page boundary cross)
                registers.A = memory.fetchAbsoluteYWithPC()
                registers.cycles += 4
            }
            0xBD -> {
                // addressing mode: absolute, x
                // cycles: 4* (+1 page boundary cross)
                registers.A = memory.fetchAbsoluteXWithPC()
                registers.cycles += 4
            }
        }
        registers.setZeroFlagFromValue(registers.A)
        registers.setNegativeFlagFromValue(registers.A)
    }

    /**
     * Load index X with memory
     */
    private fun opLDX() {
        when (cpu.currentOpcode.toInt()) {
            0xA2 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.X = memory.fetchWithPC()
                registers.cycles += 2
            }
            0xA6 -> {
                // addressing mode: zeropage
                // cycles: 3
                registers.X = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0xAE -> {
                // addressing mode: absolute
                // cycles: 4
                registers.X = memory.fetchAbsoluteWithPC()
                registers.cycles += 4
            }
            0xB6 -> {
                // addressing mode: zeropage, y
                // cycles: 4
                registers.X = memory.fetchZeroPageYWithPC()
                registers.cycles += 4
            }
            0xBE -> {
                // addressing mode: absolute, y
                // cycles: 4* (+1 page boundary cross)
                registers.X = memory.fetchAbsoluteYWithPC()
                registers.cycles += 4
            }
        }
        registers.setZeroFlagFromValue(registers.X)
        registers.setNegativeFlagFromValue(registers.X)
    }

    /**
     * Load index Y with memory
     */
    private fun opLDY() {
        when (cpu.currentOpcode.toInt()) {
            0xA0 -> {
                // addressing mode: immediate
                // cycles: 2
                registers.Y = memory.fetchWithPC()
                registers.cycles += 2
            }
            0xA4 -> {
                // addressing mode: zeropage
                // cycles: 3
                registers.Y = memory.fetchZeroPageWithPC()
                registers.cycles += 3
            }
            0xAC -> {
                // addressing mode: absolute
                // cycles: 4
                registers.Y = memory.fetchAbsoluteWithPC()
                registers.cycles += 4
            }
            0xBC -> {
                // addressing mode: absolute, x
                // cycles: 4* (+1 page boundary cross)
                registers.Y = memory.fetchAbsoluteXWithPC()
                registers.cycles += 4
            }
            0xB4 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                registers.Y = memory.fetchZeroPageXWithPC()
                registers.cycles += 4
            }
        }
        registers.setZeroFlagFromValue(registers.Y)
        registers.setNegativeFlagFromValue(registers.Y)
    }

    /**
     * Store Accu
     */
    private fun opSTA() {
        when (cpu.currentOpcode.toInt()) {
            0x81 -> {
                // addressing mode: (indirect,x)
                // cycles: 6
                val addr = memory.fetchIndexedIndirectXAddressWithPC()
                memory.store(addr, registers.A)
                registers.cycles += 6
            }
            0x85 -> {
                // addressing mode: zeropage
                // cycles: 3
                memory.store(memory.fetchWithPC().toInt(), registers.A)
                registers.cycles += 3
            }
            0x8D -> {
                // addressing mode: absolute
                // cycles: 4
                memory.store(memory.fetchWordWithPC(), registers.A)
                registers.cycles += 4
            }
            0x91 -> {
                // addressing mode: (indirect), y
                // cycles: 6
                val addr = memory.fetchIndirectIndexedYAddressWithPC()
                memory.store(addr + registers.Y.toInt(), registers.A)
                registers.cycles += 6
            }
            0x95 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                memory.store(memory.fetchZeroPageXAddressWithPC(), registers.A)
                registers.cycles += 4
            }
            0x99 -> {
                // addressing mode: absolute, y
                // cycles: 5
                memory.store(memory.fetchWordWithPC() + registers.Y.toInt(), registers.A)
                registers.cycles += 5
            }
            0x9D -> {
                // addressing mode: absolute, x
                // cycles: 5
                memory.store(memory.fetchWordWithPC() + registers.X.toInt(), registers.A)
                registers.cycles += 5
            }
        }
    }

    /**
     * Store X Register
     */
    private fun opSTX() {
        when (cpu.currentOpcode.toInt()) {
            0x86 -> {
                // addressing mode: zeropage
                // cycles: 3
                memory.store(memory.fetchWithPC().toInt(), registers.X)
                registers.cycles += 3
            }
            0x8E -> {
                // addressing mode: absolute
                // cycles: 4
                memory.store(memory.fetchWordWithPC(), registers.X)
                registers.cycles += 4
            }
            0x96 -> {
                // addressing mode: zeropage, y
                // cycles: 4
                memory.store(memory.fetchZeroPageYAddressWithPC(), registers.X)
                registers.cycles += 4
            }
        }
    }

    /**
     * Store Y Register
     */
    private fun opSTY() {
        when (cpu.currentOpcode.toInt()) {
            0x84 -> {
                // addressing mode: zeropage
                // cycles: 3
                memory.store(memory.fetchWithPC().toInt(), registers.Y)
                registers.cycles += 3
            }
            0x8C -> {
                // addressing mode: absolute
                // cycles: 4
                memory.store(memory.fetchWordWithPC(), registers.Y)
                registers.cycles += 4
            }
            0x94 -> {
                // addressing mode: zeropage, x
                // cycles: 4
                memory.store(memory.fetchZeroPageXAddressWithPC(), registers.Y)
                registers.cycles += 4
            }
        }
    }

}