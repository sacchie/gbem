package cpu

enum class RegEnum16(val num: Int8) {
    BC(0),
    DE(1),
    HL(2),
    SP(3),
    AF(3);

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
data class OpPushR16(val r: RegEnum16): Op
data class OpPopR16(val r: RegEnum16): Op

data class OpAddAR8(val r: RegEnum8): Op
data class OpAddAD8(val d: Int8): Op
class OpAddAHL: Op
data class OpAdcAR8(val r: RegEnum8): Op
data class OpAdcAD8(val d: Int8): Op
class OpAdcAHL: Op
data class OpSubAR8(val r: RegEnum8): Op
data class OpSubAD8(val d: Int8): Op
class OpSubAHL: Op
data class OpSbcAR8(val r: RegEnum8): Op
data class OpSbcAD8(val d: Int8): Op
class OpSbcAHL: Op
data class OpAndAR8(val r: RegEnum8): Op
data class OpAndAD8(val d: Int8): Op
class OpAndAHL: Op
data class OpXorAR8(val r: RegEnum8): Op
data class OpXorAD8(val d: Int8): Op
class OpXorAHL: Op
data class OpOrAR8(val r: RegEnum8): Op
data class OpOrAD8(val d: Int8): Op
class OpOrAHL: Op
data class OpCpAR8(val r: RegEnum8): Op
data class OpCpAD8(val d: Int8): Op
class OpCpAHL: Op
data class OpIncR8(val r: RegEnum8): Op
class OpIncHL : Op
data class OpDecR8(val r: RegEnum8): Op
class OpDecHL : Op
class OpDaa: Op
class OpCpl: Op
data class OpAddHLR16(val r: RegEnum16): Op
data class OpIncR16(val r: RegEnum16): Op
data class OpDecR16(val r: RegEnum16): Op
data class OpAddSpD8(val d: Int8): Op
data class OpLdSpAndD8(val d: Int8): Op
class OpRlcA : Op
class OpRlA : Op
class OpRrcA : Op
class OpRrA : Op
data class OpRlcR8(val r: RegEnum8) : Op
class OpRlcHL : Op
data class OpRlR8(val r: RegEnum8): Op
class OpRlHL : Op
data class OpRrcR8(val r: RegEnum8) : Op
class OpRrcHL : Op
data class OpRrR8(val r: RegEnum8): Op
class OpRrHL : Op
data class OpSlaR8(val r: RegEnum8) : Op
class OpSlaHL : Op
data class OpSwapR8(val r: RegEnum8) : Op
class OpSwapHL: Op
data class OpSraR8(val r: RegEnum8) : Op
class OpSraHL : Op
data class OpSrlR8(val r: RegEnum8) : Op
class OpSrlHL : Op
data class OpBitNR8(val n: Int, val r: RegEnum8) : Op
class OpBitNHL(val n: Int) : Op
data class OpSetNR8(val n: Int, val r: RegEnum8) : Op
class OpSetNHL(val n: Int) : Op
data class OpResNR8(val n: Int, val r: RegEnum8) : Op
class OpResNHL(val n: Int) : Op
class OpCcf: Op
class OpScf: Op
class OpNop: Op
class OpHalt: Op
class OpStop: Op
class OpDi: Op
class OpEi: Op

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
