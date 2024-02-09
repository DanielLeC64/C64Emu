package c64.emulation

/**
 * General exception class for errors during emulation.
 *
 * @author Daniel Schulte 2017-2024
 */
class C64ExecutionException
internal constructor(message: String) : Exception(message)