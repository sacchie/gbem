package cpu

typealias Int8 = Int

typealias Int16 = Int

fun Int16.hi(): Int8 = this.shr(8)

fun Int16.lo(): Int8 = (this and 0x000000FF)

fun int16FromHiAndLo(hi: Int8, lo: Int8): Int16 = hi.shl(8) + lo
