package cpu

typealias Int8 = Int

typealias Int16 = Int

fun Int16.hi(): Int8 = this.shr(8)

fun Int16.hi(hi: Int8): Int16 = (this and 0x000000FF) + hi.shl(8)

fun Int16.lo(): Int8 = (this and 0x000000FF)

fun Int16.lo(lo: Int8): Int16 = (this and 0x0000FF00) + lo

fun int16FromHiAndLo(hi: Int8, lo: Int8): Int16 = hi * 0xFF + lo

interface Memory {
    fun get8(addr: Int16): Int8

    fun set8(addr: Int16, int8: Int8)
}


enum class Reg16(val num: Int8) {
    BC(0),
    DE(1),
    HL(2),
    SP(3);

    companion object {
        fun fromNum(num: Int8): Reg16? = Reg16.values().find { it.num == num }
    }
}

enum class Reg8(val num: Int8) {
    B(0),
    C(1),
    D(2),
    E(3),
    H(4),
    L(5),
    A(7);

    companion object {
        fun fromNum(num: Int8): Reg8? = Reg8.values().find { it.num == num }
    }
}

// general-purpose register
interface GPR<T> {
    fun get(): T
    fun set(x: T)
}

// program counter
interface PC {
    fun get(): Int16
    fun inc(diff: Int16 = 1)
}

// flag register
interface Flag {
    fun isCarryOn(): Boolean
    fun setCarry(on: Boolean)
}

data class Registers(
    var pc: Int16,
    var a: Int8,
    var f: Int8,
    val map: MutableMap<Reg16, Int16>,
) {
    fun gpr16(reg16: Reg16): GPR<Int16> = object : GPR<Int16> {
        override fun get(): Int16 = map[reg16]!!
        override fun set(x: Int16) { map[reg16] = x }
    }

    fun gpr8(reg8: Reg8): GPR<Int8> = when (reg8) {
        Reg8.B -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.BC]!!.hi()
            override fun set(x: Int8) {
                map[Reg16.BC] = map[Reg16.BC]!!.hi(x)
            }
        }
        Reg8.C -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.BC]!!.lo()
            override fun set(x: Int8) {
                map[Reg16.BC] = map[Reg16.BC]!!.lo(x)
            }
        }
        Reg8.D -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.DE]!!.hi()
            override fun set(x: Int8) {
                map[Reg16.DE] = map[Reg16.DE]!!.hi(x)
            }
        }
        Reg8.E -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.DE]!!.lo()
            override fun set(x: Int8) {
                map[Reg16.DE] = map[Reg16.DE]!!.lo(x)
            }
        }
        Reg8.H -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.HL]!!.hi()
            override fun set(x: Int8) {
                map[Reg16.HL] = map[Reg16.HL]!!.hi(x)
            }
        }
        Reg8.L -> object : GPR<Int8> {
            override fun get(): Int8 = map[Reg16.HL]!!.lo()
            override fun set(x: Int8) {
                map[Reg16.HL] = map[Reg16.HL]!!.lo(x)
            }
        }
        Reg8.A -> object : GPR<Int8> {
            override fun get(): Int8 = a
            override fun set(x: Int8) {
                a = x
            }
        }
    }

    fun pc(): PC = object : PC {
        override fun get(): Int16 = pc
        override fun inc(diff: Int16) { pc += diff }
    }

    fun flag(): Flag = object : Flag {
        override fun isCarryOn(): Boolean = f.and(0b0001_0000) == 0b0001_0000

        override fun setCarry(on: Boolean) {
            f = if (on) f or 0b0001_0000 else f and 0b1110_1111
        }
    }
}
