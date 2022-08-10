package cpu

import kotlin.experimental.and

typealias Int8 = Byte
typealias Int16 = Short

fun Int8.exists() = this != 0.toByte()

fun Int8.shr(bitCount: Int) = this.toInt().shr(bitCount).toByte()

interface Memory {
    fun get8(addr: Int16): Int8
}

interface PC {
    fun get(): Int16
    fun inc()
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

data class RegisterValues(
    var pc: Int16,
    val reg8: MutableMap<Reg8, Int8>
)

class Registers(val values: RegisterValues) {
    val pc = object : PC {
        override fun get(): Int16 = values.pc
        override fun inc() { values.pc++ }
    }

    fun set(reg8: Reg8, int8: Int8) {
        values.reg8[reg8] = int8
    }

    fun get(reg8: Reg8): Int8 = values.reg8[reg8]!!
}

fun step(memory: Memory, registers: Registers) {
    val address: Int16 = registers.pc.get()
    val opcode = memory.get8(address)
    if (opcode.and(0b01_000_000).exists()) {
        // LD x y
        val x = Reg8.fromNum(opcode.and(0b00_111_000).shr(3))
        val y = Reg8.fromNum(opcode.and(0b00_000_111))
        if (x != null && y != null) {
            registers.set(x, registers.get(y))
            registers.pc.inc()
        }
    }
}
