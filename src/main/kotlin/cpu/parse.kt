package cpu

enum class RegEnum16(val num: Int8) {
    BC(0),
    DE(1),
    HL(2),
    SP(3);

    companion object {
        fun fromNum(num: Int8): RegEnum16? = RegEnum16.values().find { it.num == num }
    }
}

enum class RegEnum8(val num: Int8) {
    B(0),
    C(1),
    D(2),
    E(3),
    H(4),
    L(5),
    A(7);

    companion object {
        fun fromNum(num: Int8): RegEnum8? = RegEnum8.values().find { it.num == num }
    }
}

interface Op
data class OpLdR8R8(val x: RegEnum8, val y: RegEnum8): Op
data class OpLdR8D8(val r: RegEnum8, val d: Int8): Op
data class OpLdR8HL(val r: RegEnum8): Op
data class OpLdHLR8(val r: RegEnum8): Op
data class OpLdHLD8(val d: Int8): Op
class OpLdABC: Op
class OpLdADE: Op
data class OpLdAD16(val d: Int16): Op
class OpLdBCA: Op
class OpLdDEA: Op
data class OpLdD16A(val d: Int16): Op
data class OpLdFromIoPort(val d: Int8): Op
data class OpLdToIoPort(val d: Int8): Op
class OpLdFromIoPortC: Op
class OpLdToIoPortC: Op
class OpLdiHLA: Op
class OpLdiAHL: Op
class OpLddHLA: Op
class OpLddAHL: Op
data class OpLdR16D16(val r: RegEnum16, val d: Int16): Op
data class OpLdD16SP(val d: Int16): Op
class OpLdSPHL: Op
data class OpIncR16(val r: RegEnum16): Op


fun parse(memory: Memory, address: Int16): Op {
    val opcode = memory.get8(address)
    if (opcode.and(0b11_000_000) == 0b01_000_000) {
        // LD r8 HL/LD r8 r8
        val x = RegEnum8.fromNum(opcode.and(0b00_111_000).shr(3))
        val yNum = opcode.and(0b00_000_111)
        if (x != null && yNum == 0b0000_0110) {
            return OpLdR8HL(x)
        }

        val y = RegEnum8.fromNum(yNum)
        if (x != null && y != null) {
            return OpLdR8R8(x, y)
        }
    }
    if (opcode.and(0b11_000_111) == 0b00_000_110) {
        // LD r8 d8
        val r = RegEnum8.fromNum(opcode.and(0b00_111_000).shr(3))
        val d = memory.get8(address + 1)
        if (r != null) {
            return OpLdR8D8(r, d)
        }
    }
    if (opcode.and(0b11111_000) == 0b01110_000) {
        // LD HL r8
        val r = RegEnum8.fromNum(opcode.and(0b00000_111))
        if (r != null) {
            return OpLdHLR8(r)
        }
    }
    if (opcode == 0b0011_0110) {
        // LD HL d8
        val d = memory.get8(address + 1)
        return OpLdHLD8(d)
    }
    if (opcode == 0b0000_1010) {
        return OpLdABC()
    }
    if (opcode == 0b0001_1010) {
        return OpLdADE()
    }
    if (opcode == 0b1111_1010) {
        val lo = memory.get8(address + 1)
        val hi = memory.get8(address + 2)
        return OpLdAD16(int16FromHiAndLo(hi, lo))
    }
    if (opcode.and(0b11_00_1111) == 0b00_00_0011) {
        // INC r16
        val r = RegEnum16.fromNum(opcode.and(0b00_11_0000).shr(4))!!
        return OpIncR16(r)
    }
    throw IllegalArgumentException(opcode.toString())
}
