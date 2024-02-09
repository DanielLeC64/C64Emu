package c64.emulation.cia

import c64.emulation.System.clock
import c64.emulation.System.cpu
import c64.emulation.System.keyboard
import c64.util.bcdToInt
import c64.util.toBcd
import c64.util.toBinary
import c64.util.toHex
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Emulation of the C64 Complex Interface Adapter (CIA 6526).
 *
 * @author Daniel Schulte 2017-2024
 */
class CIA {

    private data class TimeOfDayClock(
        var todTen: UByte = 0x00u,
        var todSec: UByte = 0x00u,
        var todMin: UByte = 0x00u,
        var todHrs: UByte = 0x00u
    )

    companion object {
        val CIA_ADDRESS_SPACE = 0xDC00..0xDCFF
        const val DATA_PORT_A: Int = 0x00
        const val DATA_PORT_B: Int = 0x01
        const val DATA_DIRECTION_A: Int = 0x02
        const val DATA_DIRECTION_B: Int = 0x03
        const val TIMER_A_LOW: Int = 0x04
        const val TIMER_A_HIGH: Int = 0x05
        const val TIMER_B_LOW: Int = 0x06
        const val TIMER_B_HIGH: Int = 0x07

        const val TODTEN: Int = 0x08  // Time of Day Clock Tenths of Seconds
        const val TODSEC: Int = 0x09  // Time of Day Clock Seconds
        const val TODMIN: Int = 0x0A  // Time of Day Clock Minutes
        const val TODHRS: Int = 0x0B  // Time of Day Clock Hours

        const val CIASDR: Int = 0x0C  // Serial Data Port
        const val CIAICR: Int = 0x0D  // Interrupt Control Register
        const val CIACRA: Int = 0x0E  //  Control Register A
        const val CIACRB: Int = 0x0F  //  Control Register B
    }

    private var craValue: UByte = 0x00u
    private var timerAIRQEnabled = false
    private var timerAEnabled = false
    private var timerARunMode = 0
    // starting value for timer A
    private var timerALatch: Int = 0x0000
    private var timerA: Int = 0x0000

    private var crbValue: UByte = 0x00u
    private var timerBIRQEnabled = false
    private var timerBEnabled = false
    private var timerBRunMode = 0
    // starting value for timer B
    private var timerBLatch: Int = 0x0000
    private var timerB: Int = 0x0000

    private var timeOfDayCycle: Long = 0
    private var timeOfDayTenCycle: Long = 0
    private var timeOfDayClock: TimeOfDayClock = TimeOfDayClock()
    private var timeOfDayClockStopped = true
    private var timeOfDaySaved: TimeOfDayClock? = null

    private var ciaIcrState: UByte = 0x00u

    private var dataPortA: UByte = 0x00u

    /**
     * Signals the next cycle
     */
    fun cycle() {
        cycleTimeOfDayClock()
        cycleTimerA()
        cycleTimerB()
    }

    private fun cycleTimeOfDayClock() {
        // check for stopped time of day clock
        if (timeOfDayClockStopped) {
            return
        }

        // adjust TOD
        timeOfDayCycle++
        timeOfDayTenCycle++
        if (timeOfDayCycle > clock.cyclesPerSecond) {
            timeOfDayCycle = 0
            timeOfDayTenCycle = 0
            timeOfDayClock.todTen = 0x00u
            val second = timeOfDayClock.todSec.bcdToInt()
            if (second == 59) {
                timeOfDayClock.todSec = 0x00u

                val minute = timeOfDayClock.todMin.bcdToInt()
                if (minute == 59) {
                    timeOfDayClock.todMin = 0x00u

                    val hour = timeOfDayClock.todHrs.bcdToInt()
                    var ampmFlag = timeOfDayClock.todHrs and 0b1000_0000u
                    when (hour) {
                        11 -> {
                            ampmFlag = ampmFlag xor 0b1000_0000u
                            timeOfDayClock.todHrs = (hour + 1).toUByte().toBcd() or ampmFlag
                        }
                        12 -> {
                            timeOfDayClock.todHrs = 0x01.toUByte() or ampmFlag
                        }
                        else -> {
                            timeOfDayClock.todHrs = (hour + 1).toUByte().toBcd() or ampmFlag
                        }
                    }
                } else {
                    timeOfDayClock.todMin = (minute + 1).toUByte().toBcd()
                }
            } else {
                timeOfDayClock.todSec = (second + 1).toUByte().toBcd()
            }
        }
        if (timeOfDayTenCycle > clock.cyclesPerTenthsSecond) {
            // adjust tenths seconds
            timeOfDayTenCycle = 0
            timeOfDayClock.todTen++
            if (timeOfDayClock.todTen > 0x09u)
            {
                timeOfDayClock.todTen = 0x00u
            }
        }
    }

    private fun cycleTimerA() {
        if (timerAEnabled) {
            //logger.info {"counting cycle down to $timerA"}
            timerA--
            if (timerA <= 0) {
                // timer has reached 0
                // --> set CIAICR: bit 0 (timer a count down to 0), bit 7 (IRQ)
                ciaIcrState = ciaIcrState or 0b1000_0001u
                // reset timer to latch
                timerA = timerALatch
                if (timerARunMode == 8) {
                    // timer is in one-shot mode ==> stop timer
                    timerAEnabled = false
                }
                if (timerAIRQEnabled) {
                    // signal Timer A interrupt
                    cpu.signalTimerIRQ()
                }
            }
        }
    }

    private fun cycleTimerB() {
        if (timerBEnabled) {
            //logger.info {"counting cycle down to $timerB"}
            timerB--
            if (timerB <= 0) {
                // timer has reached 0
                // --> set CIAICR: bit 1 (timer b count down to 0), bit 7 (IRQ)
                ciaIcrState = ciaIcrState or 0b1000_0010u
                // reset timer to latch
                timerB = timerBLatch
                if (timerBRunMode == 8) {
                    // timer is in one-shot mode ==> stop timer
                    timerBEnabled = false
                }
                if (timerBIRQEnabled) {
                    // signal Timer B interrupt
                    cpu.signalTimerIRQ()
                }
            }
        }
    }

    /**
     * Fetches a single byte from the given CIA address.
     */
    fun fetch(address: Int): UByte {
        return when (address and 0x000F) {
            DATA_PORT_A -> {
                dataPortA
            }
            DATA_PORT_B -> {
                keyboard.getDataPortB(dataPortA)
            }
            TIMER_A_LOW -> {
                return timerA.toUByte()
            }
            TIMER_A_HIGH -> {
                return ((timerA and 0xFF00) shr 8).toUByte()
            }
            TIMER_B_LOW -> {
                return timerB.toUByte()
            }
            TIMER_B_HIGH -> {
                return ((timerB and 0xFF00) shr 8).toUByte()
            }
            TODTEN -> {
                // unfreeze TOD clock
                timeOfDaySaved = null
                return timeOfDayClock.todTen
            }
            TODSEC -> {
                return timeOfDaySaved?.todSec ?: timeOfDayClock.todSec
            }
            TODMIN -> {
                return timeOfDaySaved?.todMin ?: timeOfDayClock.todMin
            }
            TODHRS -> {
                // freeze whole clock will TODTEN will be read
                timeOfDaySaved = timeOfDayClock.copy()
                return timeOfDaySaved?.todHrs ?: timeOfDayClock.todHrs
            }
            CIAICR -> {
                val result = ciaIcrState
                ciaIcrState = 0x00u
                result
            }
            CIACRA -> {
                return craValue
            }
            CIACRB -> {
                return crbValue
            }
            else -> {
                // todo - implementation
                logger.info { "missing IMPL for CIA1:read ${address.toHex()}" }
                0x00.toUByte()
            }
        }
    }

    /**
     * Stores a single byte at the given CIA address.
     */
    fun store(address: Int, byte: UByte) {
        // use only bit 0-4, mask out all higher bits
        when (address and 0x000F) {
            DATA_PORT_A -> {
                // todo: check DATA_DIRECTION_A before writing
                // set keyboard matrix column
                dataPortA = byte
            }
            DATA_PORT_B -> {
                logger.info { "missing IMPL for DATA_PORT_B:write ${byte.toHex()} (${byte.toBinary()})" }
            }
            DATA_DIRECTION_A -> {
                logger.info { "missing IMPL for DATA_DIRECTION_A:write ${byte.toHex()}" }
            }
            DATA_DIRECTION_B -> {
                logger.info { "missing IMPL for DATA_DIRECTION_B:write ${byte.toHex()}" }
            }
            TIMER_A_LOW -> {
                timerALatch = (timerALatch and 0xFF00) + byte.toInt()
            }
            TIMER_A_HIGH -> {
                timerALatch = (timerALatch and 0x00FF) + (byte.toInt() shl 8)
            }
            TIMER_B_LOW -> {
                timerBLatch = (timerBLatch and 0xFF00) + byte.toInt()
            }
            TIMER_B_HIGH -> {
                timerBLatch = (timerBLatch and 0x00FF) + (byte.toInt() shl 8)
            }
            TODTEN -> {
                // TODO: check for CRB-Bit7=1 --> set alarm
                timeOfDayClock.todTen = byte and 0b0000_1111u
                timeOfDayClockStopped = false
            }
            TODSEC -> {
                // TODO: check for CRB-Bit7=1 --> set alarm
                timeOfDayClock.todSec = byte and 0b0111_1111u
            }
            TODMIN -> {
                // TODO: check for CRB-Bit7=1 --> set alarm
                timeOfDayClock.todMin = byte and 0b0111_1111u
            }
            TODHRS -> {
                // TODO: check for CRB-Bit7=1 --> set alarm
                timeOfDayClockStopped = true
                timeOfDayClock.todHrs = byte
            }
            CIASDR -> {
                logger.info { "missing IMPL for CIASDR:write ${byte.toHex()}" }
            }
            CIAICR -> {
                writeCIAICR(byte)
            }
            CIACRA -> {
                writeCIACRA(byte)
            }
            CIACRB -> {
                writeCIACRB(byte)
            }
        }
    }

    private fun writeCIAICR(byte: UByte) {
        // logger.info {"write to CIAICR register: ${byte.toBinary()}"}
        // BIT 7: set or clear bits (0=clear bits, 1=set bits)
        val setMode = byte.toInt() and 0b1000_0000 == 0b1000_0000
        // BIT 0: enable / disable Timer A interrupt
        if (byte.toInt() and 0b0000_0001 == 0b0000_0001) {
            timerAIRQEnabled = setMode
            logger.info {"timerAIRQEnabled: $timerAIRQEnabled"}
        }
        // BIT 1: enable / disable Timer B interrupt
        else if (byte.toInt() and 0b0000_0010 == 0b0000_0010) {
            timerBIRQEnabled = setMode
            logger.info {"timerBIRQEnabled: $timerBIRQEnabled"}
        }
        // BIT 5-6 unused
        // check for missing implementation
        if (byte.toInt() and 0b0001_1100 > 0) {
            // todo: implementation
            logger.warn { "not handled BITS for CIAICR register: ${(byte and 0b0001_1100u).toBinary()}" }
        }
    }

    private fun writeCIACRA(byte: UByte) {
        // logger.info { "write to CIACRA register: ${byte.toBinary()}" }
        craValue = byte

        // BIT 0: start/stop Timer A
        timerAEnabled = byte.toInt() and 0b0000_0001 == 0b0000_0001
        logger.info {"timerAEnabled: $timerAEnabled"}
        // BIT 3: Timer A run mode: 1=one-shot, 0=continous
        timerARunMode = byte.toInt() and 0b0000_1000
        logger.info { "timerARunMode: $timerARunMode" }
        // BIT 4: load latch Timer A
        if (byte.toInt() and 0b0001_0000 == 0b0001_0000) {
            timerA = timerALatch
        }
        // check for missing implementation
        if (byte.toInt() and 0b1110_0110 > 0) {
            // todo: implementation
            logger.warn { "not handled BITS for write CIACRA register: ${(byte and 0b1110_0110u).toBinary()}" }
        }
    }

    private fun writeCIACRB(byte: UByte) {
        // logger.info { "write to CIACRB register: ${byte.toBinary()}" }
        crbValue = byte

        // BIT 0: start/stop Timer B
        timerBEnabled = byte.toInt() and 0b0000_0001 == 0b0000_0001
        logger.info { "timerBEnabled: $timerBEnabled" }
        // BIT 3: Timer B run mode: 1=one-shot, 0=continous
        timerBRunMode = byte.toInt() and 0b0000_1000
        logger.info { "timerBRunMode: $timerBRunMode" }
        // BIT 4: load latch Timer B
        if (byte.toInt() and 0b0001_0000 == 0b0001_0000) {
            timerB = timerBLatch
        }
        // check for missing implementation
        if (byte.toInt() and 0b1110_0110 > 0) {
            // todo: implementation
            logger.warn { "not handled BITS for write CIACRB register: ${(byte and 0b1110_0110u).toBinary()}" }
        }
    }
}