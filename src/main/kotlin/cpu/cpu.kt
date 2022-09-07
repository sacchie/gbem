package cpu

typealias Int8 = Int

typealias Int16 = Int

fun Int16.hi(): Int8 = this.shr(8)

fun Int16.lo(): Int8 = (this and 0x000000FF)

fun int16FromHiAndLo(hi: Int8, lo: Int8): Int16 = hi.shl(8) + lo

interface Memory {
    fun get8(addr: Int16): Int8

    fun get16(addr: Int16): Int16

    fun set8(addr: Int16, int8: Int8)

    fun set16(addr: Int16, int16: Int16)
}

// general-purpose register
interface GPR<T> {
    fun get(): T
    fun set(x: T)

    fun update(fn: (x: T) -> T) { set(fn(get())) }
}

// program counter
interface PC {
    fun get(): Int16
    fun inc(diff: Int16 = 1)
}

// flag register
interface Flag {
    fun setZero(on: Boolean)
    fun setSubtraction(on: Boolean)
    fun setHalfCarry(on: Boolean)
    fun isCarryOn(): Boolean
    fun setCarry(on: Boolean)
}

data class Registers(
    private var pc: Int16 = 0,
    private var af: Int16 = 0,
    private var bc: Int16 = 0,
    private var de: Int16 = 0,
    private var hl: Int16 = 0,
    private var sp: Int16 = 0,
) {
    fun af(): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = af
        override fun set(x: Int16) {
            af = x
        }
    }

    fun bc(): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = bc
        override fun set(x: Int16) {
            bc = x
        }
    }

    fun de(): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = de
        override fun set(x: Int16) {
            de = x
        }
    }

    fun hl(): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = hl
        override fun set(x: Int16) {
            hl = x
        }
    }

    fun sp(): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = sp
        override fun set(x: Int16) {
            sp = x
        }
    }

    fun a(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = af.hi()
        override fun set(x: Int16) {
            af = int16FromHiAndLo(x, af.lo())
        }
    }

    fun b(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = bc.hi()
        override fun set(x: Int8) {
            bc = int16FromHiAndLo(x, bc.lo())
        }
    }

    fun c(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = bc.lo()
        override fun set(x: Int8) {
            bc = int16FromHiAndLo(bc.hi(), x)
        }
    }

    fun d(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = de.hi()
        override fun set(x: Int8) {
            de = int16FromHiAndLo(x, de.lo())
        }
    }

    fun e(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = de.lo()
        override fun set(x: Int8) {
            de = int16FromHiAndLo(de.hi(), x)
        }
    }

    fun h(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = hl.hi()
        override fun set(x: Int8) {
            hl = int16FromHiAndLo(x, hl.lo())
        }
    }

    fun l(): GPR<Int8> = object : GPR<Int8> {
        override fun get(): Int8 = hl.lo()
        override fun set(x: Int8) {
            hl = int16FromHiAndLo(hl.hi(), x)
        }
    }

    fun pc(): PC = object : PC {
        override fun get(): Int16 = pc
        override fun inc(diff: Int16) {
            pc += diff
        }
    }

    fun flag(): Flag = object : Flag {
        override fun isCarryOn(): Boolean = af.lo().and(0b0001_0000) == 0b0001_0000
        override fun setCarry(on: Boolean) {
            val f = af.lo()
            af = int16FromHiAndLo(af.hi(), if (on) f or 0b0001_0000 else f and 0b1110_1111)
        }
    }
}
