package c64.emulation.cpu

import c64.util.toBitString
import c64.util.toUnprefixedHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * This class handles all MOS 6510/8500 processor registers.
 * Additional this class contains a cycle counter.
 *
 * @author Daniel Schulte 2017-2018
 */
@ExperimentalUnsignedTypes
class Registers {
    companion object {
        const val DEBUG_MACHINE_STATE_HEADER: String = " PC   AC XR YR SP   NV-BDIZC  CY"
    }

    // program counter
    // todo - add setter method to check 16bit range
    internal var PC: Int = 0x0000
    // accu
    internal var A: UByte = 0x00u
    // X register
    internal var X: UByte = 0x00u
    // Y register
    internal var Y: UByte = 0x00u
    // stack pointer
    internal var SP: UByte = 0xFFu

    // Flags NV-BDIZC
    // negative flag
    internal var N: Boolean = false
    // overflow flag
    internal var V: Boolean = false
    // break flag
    internal var B: Boolean = false
    // decimal flag
    internal var D: Boolean = false
    // interrupt flag
    internal var I: Boolean = false
    // zero flag
    internal var Z: Boolean = false
    // carry flag
    internal var C: Boolean = false

    // cycle counter
    internal var cycles: Long = 0

    internal fun reset() {
        A = 0x00u
        X = 0x00u
        Y = 0x00u
        SP = 0xFFu
        PC = 0x0000
        N = false
        V = false
        B = false
        D = false
        I = false
        Z = false
        C = false
    }

    internal fun getProcessorStatus(): UByte {
        var result = 0
        if (N) result += 128 // bit 7 = N
        if (V) result += 64  // bit 6 = V
        result += 32         // unused bit 5 always set
        // Break bit always 1
        //if (B) result += 16  // bit 4 = B
        result += 16
        if (D) result += 8   // bit 3 = D
        if (I) result += 4   // bit 2 = I
        if (Z) result += 2   // bit 1 = Z
        if (C) result += 1   // bit 0 = C
        return result.toUByte()
    }

    internal fun setProcessorStatus(status: UByte) {
        N = (status.toInt() and 128 == 128)
        V = (status.toInt() and 64 == 64)

        /**
         * TODO: behaviour of B
         * see:http://forum.6502.org/viewtopic.php?f=8&t=3111#p35579
         * The emulators just follow the behavior of a real 6502 or any of its hardware successors.
         * The unused bit returns a 1 when read, because it is not present in hardware and reading an open circuit
         * simply returns a logic high state. The same is true for the break bit, as it is not an existing flag bit
         * register but a forced low to an otherwise open circuit. The bit is forced low only when the processor flag
         * bits are pushed onto the stack during either an IRQ or a NMI. So the break bit would be better defined to
         * signal "pushed by software" (BRK or PHP only).
         */
        // B = (status.toInt() and 16 == 16)
        B = true
        D = (status.toInt() and 8 == 8)
        I = (status.toInt() and 4 == 4)
        Z = (status.toInt() and 2 == 2)
        C = (status.toInt() and 1 == 1)
    }

    internal fun setZeroFlagFromValue(byte: UByte) {
        Z = byte.toInt() == 0x00
    }

    internal fun setNegativeFlagFromValue(byte: UByte) {
        N = byte > 0x7Fu
    }

    internal fun setOverflowFlagFromSignedValue(signedValue: Int) {
        // check for overflow of possible signed values in 2er complement (-128..127)
        V = signedValue > 127 || signedValue < -128
    }

    internal fun printRegisters(): String {
        return DEBUG_MACHINE_STATE_HEADER + "\n" + printRegisterValues()
    }

    private fun printRegisterValues(): String {
        return "${PC.toUnprefixedHex()}  ${A.toUnprefixedHex()} ${X.toUnprefixedHex()} " +
                "${Y.toUnprefixedHex()} ${SP.toUnprefixedHex()}   " +
                "${N.toBitString()}${V.toBitString()}-${B.toBitString()}${D.toBitString()}${I.toBitString()}" +
                "${Z.toBitString()}${C.toBitString()}  $cycles"
    }
}