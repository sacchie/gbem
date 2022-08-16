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

interface Command
data class CommandLdR8R8(val x: RegEnum8, val y: RegEnum8): Command
data class CommandLdR8D8(val r: RegEnum8, val d: Int8): Command
data class CommandLdR8HL(val r: RegEnum8): Command
data class CommandLdHLR8(val r: RegEnum8): Command
data class CommandLdHLD8(val d: Int8): Command
class CommandLdABC: Command
class CommandLdADE: Command
data class CommandLdAD16(val d: Int16): Command
data class CommandIncR16(val r: RegEnum16): Command


fun parse(memory: Memory, address: Int16): Command {
    val opcode = memory.get8(address)
    if (opcode.and(0b11_000_000) == 0b01_000_000) {
        // LD r8 HL/LD r8 r8
        val x = RegEnum8.fromNum(opcode.and(0b00_111_000).shr(3))
        val yNum = opcode.and(0b00_000_111)
        if (x != null && yNum == 0b0000_0110) {
            return CommandLdR8HL(x)
        }

        val y = RegEnum8.fromNum(yNum)
        if (x != null && y != null) {
            return CommandLdR8R8(x, y)
        }
    }
    if (opcode.and(0b11_000_111) == 0b00_000_110) {
        // LD r8 d8
        val r = RegEnum8.fromNum(opcode.and(0b00_111_000).shr(3))
        val d = memory.get8(address + 1)
        if (r != null) {
            return CommandLdR8D8(r, d)
        }
    }
    if (opcode.and(0b11111_000) == 0b01110_000) {
        // LD HL r8
        val r = RegEnum8.fromNum(opcode.and(0b00000_111))
        if (r != null) {
            return CommandLdHLR8(r)
        }
    }
    if (opcode == 0b0011_0110) {
        // LD HL d8
        val d = memory.get8(address + 1)
        return CommandLdHLD8(d)
    }
    if (opcode == 0b0000_1010) {
        return CommandLdABC()
    }
    if (opcode == 0b0001_1010) {
        return CommandLdADE()
    }
    if (opcode == 0b1111_1010) {
        val lo = memory.get8(address + 1)
        val hi = memory.get8(address + 2)
        return CommandLdAD16(int16FromHiAndLo(hi, lo))
    }
    if (opcode.and(0b11_00_1111) == 0b00_00_0011) {
        // INC r16
        val r = RegEnum16.fromNum(opcode.and(0b00_11_0000).shr(4))!!
        return CommandIncR16(r)
    }
    throw IllegalArgumentException(opcode.toString())
}
