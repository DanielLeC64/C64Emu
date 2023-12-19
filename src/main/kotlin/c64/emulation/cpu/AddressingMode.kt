package c64.emulation.cpu

/**
 * Enumeration with possible addressing modes for the MOS 6510/8500.
 *
 * @author Daniel Schulte 2017-1018
 */
enum class AddressingMode(val numByte: Int) {
    Implied(1), ZeroPage(2), ZeroPageX(2), ZeroPageY(2),
    Immediate(2), Relative(2), Indirect(3),
    Absolute(3), AbsoluteX(3), AbsoluteY(3),
    IndirectIndexedY(2), IndexedIndirectX(2),
    Accumulator(1)
}
