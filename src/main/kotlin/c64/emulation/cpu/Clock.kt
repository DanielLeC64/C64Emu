package c64.emulation.cpu


/**
 * Clock - helper to get the right timing handled. It runs with a resolution of 1 millisecond.
 *
 * @author Daniel Schulte 2017-2021
 */
class Clock {

    companion object {
        // PAL clock speed: 985248 Hz
        const val PAL_CLOCK = 985248
        const val PAL_CYCLES_PER_MILLI = PAL_CLOCK / 1000
    }

    // use PAL as default for the clock
    val clock = PAL_CLOCK
    val cyclesPerSecond = PAL_CLOCK
    val cyclesPerTenthsSecond = (PAL_CLOCK / 10).toLong()
    val cyclesPerMilli = PAL_CYCLES_PER_MILLI

    fun start(callback: (Long) -> Long) {
        var lastMilliSecond = System.currentTimeMillis()
        // clock main loop
        while (true) {
            // get current millisecond
            val currentMilliSecond = System.currentTimeMillis()
            // check whether milliseconds counted up
            if (currentMilliSecond > lastMilliSecond) {
                callback.invoke((currentMilliSecond - lastMilliSecond) * cyclesPerMilli)
                lastMilliSecond = currentMilliSecond
            } else {
                // same millisecond, sleep 1 millisecond instead
                Thread.sleep(1)
            }
        }
    }

    // OLD CODE - TRY TO RUN WITH A RESOLUTION OF MICROSECONDS
    /*fun start(callback: (Long) -> Long) {
        var lastMicroSecond = System.nanoTime() / 1_000
        // clock main loop
        while (true) {
            // get current microsecond
            val currentMicroSecond = System.nanoTime() / 1_000
            // check whether microseconds counted up
            if (currentMicroSecond > lastMicroSecond) {
                // run callback method for n cycles and add the additional cycles to lastMicroSecond
                lastMicroSecond = currentMicroSecond + callback.invoke(currentMicroSecond - lastMicroSecond)
            }
        }
    }*/
}