@file: JvmName("ThreeMath")

package info.laht.acco.math

import kotlin.math.*
import kotlin.random.Random

val LN2 = ln(2.0)
const val DEG2RAD = (PI / 180.0)
const val RAD2DEG = (180.0 / PI)

const val TWO_PI = (PI * 2)

private val lut by lazy {
    Array(256) { i ->
        if (i < 16) "0" else i.toString(16)
    }
}

fun generateUUID(): String {

    val d0 = (Random.nextDouble() * 0xffffffff).toInt() or 0
    val d1 = (Random.nextDouble() * 0xffffffff).toInt() or 0
    val d2 = (Random.nextDouble() * 0xffffffff).toInt() or 0
    val d3 = (Random.nextDouble() * 0xffffffff).toInt() or 0

    val uuid = lut[d0 and 0xff] + lut[d0 shr 8 and 0xff] + lut[d0 shr 16 and 0xff] + lut[d0 shr 24 and 0xff] + '-' +
            lut[d1 and 0xff] + lut[d1 shr 8 and 0xff] + '-' + lut[d1 shr 16 and 0x0f or 0x40] + lut[d1 shr 24 and 0xff] + '-' +
            lut[d2 and 0x3f or 0x80] + lut[d2 shr 8 and 0xff] + '-' + lut[d2 shr 16 and 0xff] + lut[d2 shr 24 and 0xff] +
            lut[d3 and 0xff] + lut[d3 shr 8 and 0xff] + lut[d3 shr 16 and 0xff] + lut[d3 shr 24 and 0xff]

    return uuid.toUpperCase()
}

fun clamp(value: Int, min: Int, max: Int): Int {
    return max(min, min(max, value))
}

fun clamp(value: Double, min: Number, max: Number): Double {
    return max(min.toDouble(), min(max.toDouble(), value))
}

fun mapLinear(x: Double, a1: Double, a2: Double, b1: Double, b2: Double): Double {
    return b1 + (x - a1) * (b2 - b1) / (a2 - a1)
}

fun lerp(x: Double, y: Double, t: Double): Double {
    return (1 - t) * x + t * y
}

fun smoothStep(x: Double, min: Double, max: Double): Double {
    if (x <= min) {
        return 0.0
    }
    if (x >= max) {
        return 1.0
    }

    return (x - min) / (max - min).let {
        it * it * (3 - 2 * it)
    }

}

fun isPowerOfTwo(value: Int): Boolean {
    return (value and (value - 1)) == 0 && value != 0
}

fun ceilPowerOfTwo(value: Double): Double {
    return 2.0.pow(ceil(ln(value) / LN2))
}

fun floorPowerOfTwo(value: Double): Double {
    return 2.0.pow(floor(ln(value) / LN2))
}
