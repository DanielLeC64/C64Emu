package c64.emulation.cpu

import c64.emulation.C64ExecutionException
import c64.emulation.System.cia
import c64.emulation.System.clock
import c64.emulation.System.memory
import c64.emulation.System.registers
import c64.emulation.System.vic
import c64.emulation.cpu.instructionset.*
import c64.emulation.debugger.Debugger
import c64.emulation.disassemble.Disassembly
import c64.emulation.memory.Memory
import c64.util.toHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

// alias for "Instruction" functions
typealias Instruction = () -> Unit

@ExperimentalUnsignedTypes
typealias InstructionWithArgAndResult = (value: UByte) -> UByte

@ExperimentalUnsignedTypes
typealias InstructionWithArg = (value: UByte) -> Unit

/**
 * Emulator for CPU MOS 6510/8500.
 *
 * @author Daniel Schulte 2017-2023
 */
@ExperimentalUnsignedTypes
class CPU {

    private data class OpCodeInfo(val instruction: Instruction)
    private data class OpCodeInfoWithArg(
        val instruction: InstructionWithArg,
        val addressingMode: AddressingMode,
        val cycles: Int
    )

    private data class OpCodeInfoWithArgAndResult(
        val instruction: InstructionWithArgAndResult,
        val addressingMode: AddressingMode,
        val cycles: Int
    )

    companion object {
        const val NMI_VECTOR: Int = 0xFFFA
        const val RESET_VECTOR: Int = 0xFFFC
        const val IRQ_BRK_VECTOR: Int = 0xFFFE
        // table with all instructions methods indexed with their opcode
        private val INSTRUCTION_TABLE = arrayOfNulls<Any>(0x100)
    }

    internal var disassembly: Disassembly = Disassembly()
    internal var debugger: Debugger = Debugger(disassembly)

    // currently executed opcode
    internal var currentOpcode: UByte = 0x00u

    private var numRegisteredOps = 0

    // 0 none, 1=IRQ, 2=NMI, 3=RESET, 4=BRK
    private var irqTypeSignaled = 0

    fun initialize() {
        logger.info { "init CPU 6510/8500" }

        // ***************
        // functional_test: end 0x3469, timeout 96_241_300

        // debugging settings
        disassembly.startDisassemblerAt = 0x0000
        //debugger.breakpoint = 0xEA87 // main irq entry point (0xEA31)
        //debugger.breakpoint = 0
        //debugger.waitForCycle = 2_130_000

        // initialize instructions table
        val instructions = arrayOf(
            ::IncrementsDecrements, ::RegisterTransfers, ::LoadStore, ::JumpsCalls,
            ::Arithmetic, ::Logical, ::Branch, ::Stack, ::StatusFlags, ::Shift, ::System
        )
        instructions.forEach { it() }
        logger.debug { "$numRegisteredOps opCodes registered" }
    }

    /**
     * Registers a given instruction with the given opCode.
     */
    internal fun registerInstruction(opCode: Int, instruction: Instruction) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfo(instruction)
        numRegisteredOps++
    }

    /**
     * Registers a given instruction (with args but without result) with the given opCode, addressingMode and cycles.
     */
    internal fun registerInstruction(opCode: Int, instruction: InstructionWithArg, addressingMode: AddressingMode,
                                     cycles: Int) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfoWithArg(instruction, addressingMode, cycles)
        numRegisteredOps++
    }

    /**
     * Registers a given instruction (with args and result) with the given opCode, addressingMode and cycles.
     */
    internal fun registerInstructionWithResult(opCode: Int, instruction: InstructionWithArgAndResult,
                                               addressingMode: AddressingMode, cycles: Int) {
        // check for duplicate entries
        if (INSTRUCTION_TABLE[opCode] != null) {
            throw IllegalArgumentException("Duplicate registration of opcode <${opCode.toUByte().toHex()}> !")
        }
        INSTRUCTION_TABLE[opCode] = OpCodeInfoWithArgAndResult(instruction, addressingMode, cycles)
        numRegisteredOps++
    }

    fun reset() {
        registers.reset()
        // hardwired processor port data direction
        memory.store(Memory.CPU_PORT_DATA_DIRECTION_ADDR, 0x27u)
        // hardwired processor port
        memory.store(Memory.CPU_PORT_ADDR, 0x37u)
        registers.PC = memory.fetchWord(RESET_VECTOR)
    }

    fun runMachine() {
        clock.start(::runCycles)
    }

    /**
     * Runs the machine for the given amount of cycles.
     * @param cycles numer of cycles to execute
     * @return number of additional executed cycles
     */
    private fun runCycles(cycles: Long): Long {
        try {
            var cyclesExecuted: Long = 0
            do {
                val lastMachineCycleCount = registers.cycles
                // check debugging status, maybe print registers
                debugger.checkStatus()
                // check disassembly status
                disassembly.checkStatus()
                // fetch byte from memory
                currentOpcode = memory.fetchWithPC()
                // decode and run opcode
                decodeAndRunOpCode(currentOpcode)

                // do the VIC stuff
                vic.refresh()

                //  IRQ handling
                handleInterrupt()

                var opCodeCycleCount = registers.cycles - lastMachineCycleCount
                cyclesExecuted += opCodeCycleCount
                while (opCodeCycleCount > 0) {
                    cia.cycle()
                    opCodeCycleCount--
                }
            }
            while (cyclesExecuted < cycles)
            return cyclesExecuted - cycles

        } catch (ex: C64ExecutionException) {
            logger.error { ex.message }
            return 0
        }
    }

    fun signalTimerAIRQ() {
        irqTypeSignaled = 1
    }

    private fun handleInterrupt() {
        if (irqTypeSignaled != 0) {
            // https://www.retro-programming.de/programming/nachschlagewerk/interrupts/
            if (!registers.I) {
                // 1.Sobald ein Interrupt auftritt, unterbricht die CPU das aktuelle Programm, nachdem der aktive Befehl verarbeitet wurde.
                // Handelt es sich bei der Quelle um einen RESET, dann geht es direkt bei Punkt 6 weiter.
                if (irqTypeSignaled != 3) {

                    // 2.Die Adresse des nächsten Befehls wird auf dem Stack abgelegt, erst das MSB, dann LSB.
                    // Denkt dran, dass der Stack, von oben nach unten gefüllt wird und somit die Adresse wieder im gewohnten LSB/MSB-Format im Speicher liegt.
                    memory.pushWordToStack(registers.PC)

                    // 3.Handelt es sich beim Auslöser um den BRK-Befehl, dann wird das B-Flag gesetzt.
                    // 4.Danach landet das Statusregister (inkl. ggf. gesetzem B-Flag) ebenfalls auf dem Stack.
                    if (irqTypeSignaled == 4) {
                        // set B-Flag
                        memory.pushToStack(registers.getProcessorStatus() or 0b0001_0000u)
                    } else {
                        memory.pushToStack(registers.getProcessorStatus() and 0b1110_1111u)
                    }

                    // 5.Jetzt werden noch weitere Interrupts durch Setzen des I-Flags verhindert.
                    registers.I = true
                }

                // 6.Die CPU springt zu der laut Hardware-Vektoren zugehörigen Speicherstelle.
                // Abhängig von der jeweiligen Interruptquelle gibt es unterschiedliche Einsprungadressen.
                if (irqTypeSignaled == 1 || irqTypeSignaled == 4) {
                    // IRQ/BRK
                    registers.PC = memory.fetchWord(IRQ_BRK_VECTOR)
                } else if (irqTypeSignaled == 2) {
                    // NMI
                    registers.PC = memory.fetchWord(NMI_VECTOR)
                } else if (irqTypeSignaled == 3) {
                    // RESET
                    registers.PC = memory.fetchWord(RESET_VECTOR)
                }

                // Bei einem RESET ist hier Schluß, schließlich startet der Rechner alles neu.
                // 7.Trifft der Prozessor auf ein RTI (ReTurn from Interrupt), dann wird zunächst das Statusregister vom Stack geholt.
                // Da das I-Flag erst nach der Ablage auf dem Stack gesetzt wurde, wird es so auch automatisch wieder gelöscht.
                // Anschließend wird die Adresse des nächsten Befehls (s. Punkt 2) vom Stack geholt und das Programm dort fortgesetzt.
            }

            irqTypeSignaled = 0
        }
    }

    @Throws(C64ExecutionException::class)
    private fun decodeAndRunOpCode(opcode: UByte) {
        val opCodeInfo = INSTRUCTION_TABLE[opcode.toInt()]
        if (opCodeInfo != null) {
            // print disassembled code
            if (logger.isDebugEnabled && disassembly.printDisassembledCode) {
                logger.debug(disassembly.disassemble(opcode))
            }
            debugger.handleConsoleDebugging()
            if (opCodeInfo is OpCodeInfo) {
                opCodeInfo.instruction.invoke()
            } else if (opCodeInfo is OpCodeInfoWithArg) {
                runOpcodeWithFetch(opCodeInfo)
            } else if (opCodeInfo is OpCodeInfoWithArgAndResult) {
                runOpcodeWithFetchStore(opCodeInfo)
            }
        } else {
            // reset PC to get the correct register output
            registers.PC--
            throw C64ExecutionException(
                "CPU jam! Found unknown op-code <${opcode.toHex()}>\n" +
                        "${registers.printRegisters()}\n" +
                        memory.printMemoryLineWithAddress(registers.PC)
            )
        }
    }

    private fun runOpcodeWithFetch(opCodeInfo: OpCodeInfoWithArg) {
        val value: UByte
        // fetch value from memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage -> {
                value = memory.fetchZeroPageWithPC()
            }
            AddressingMode.ZeroPageX -> {
                value = memory.fetchZeroPageXWithPC()
            }
            AddressingMode.Accumulator -> {
                value = registers.A
            }
            AddressingMode.Immediate -> {
                value = memory.fetchWithPC()
            }
            AddressingMode.Absolute -> {
                value = memory.fetchAbsoluteWithPC()
            }
            AddressingMode.AbsoluteX -> {
                value = memory.fetchAbsoluteXWithPC()
            }
            AddressingMode.AbsoluteY -> {
                value = memory.fetchAbsoluteYWithPC()
            }
            AddressingMode.IndexedIndirectX -> {
                value = memory.fetchIndexedIndirectXWithPC()
            }
            AddressingMode.IndirectIndexedY -> {
                value = memory.fetchIndirectIndexedYWithPC()
            }
            else -> {
                value = 0u
            }
        }
        opCodeInfo.instruction.invoke(value)
        // increment cycles
        registers.cycles += opCodeInfo.cycles
    }

    private fun runOpcodeWithFetchStore(opCodeInfo: OpCodeInfoWithArgAndResult) {
        var value: UByte = 0u
        var addr = -1
        // fetch value from memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage -> {
                addr = memory.fetchZeroPageAddressWithPC()
            }
            AddressingMode.ZeroPageX -> {
                addr = memory.fetchZeroPageXAddressWithPC()
            }
            AddressingMode.Accumulator -> {
                value = registers.A
            }
            AddressingMode.Absolute -> {
                addr = memory.fetchWordWithPC()
            }
            AddressingMode.AbsoluteX -> {
                addr = memory.fetchWordWithPC() + registers.X.toInt()
            }
            AddressingMode.AbsoluteY -> {
                addr = memory.fetchWordWithPC() + registers.Y.toInt()
            }
            else -> {
                addr = 0
                value = 0u
            }
        }
        if (addr != -1) {
            value = memory.fetch(addr)
        }
        value = opCodeInfo.instruction.invoke(value)
        // increment cycles
        registers.cycles += opCodeInfo.cycles
        // write back value to memory
        when (opCodeInfo.addressingMode) {
            AddressingMode.ZeroPage,
            AddressingMode.ZeroPageX,
            AddressingMode.Absolute,
            AddressingMode.AbsoluteX,
            AddressingMode.AbsoluteY -> {
                memory.store(addr, value)
            }
            AddressingMode.Accumulator -> {
                registers.A = value
            }
            else -> {

            }
        }
    }
}