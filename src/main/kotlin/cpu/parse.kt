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

enum class ConditionalJumpFlag {
    NZ, Z, NC, C
}

interface Op
data class OpLdR8R8(val x: RegEnum8, val y: RegEnum8) : Op
data class OpLdR8D8(val r: RegEnum8, val d: Int8) : Op
data class OpLdR8HL(val r: RegEnum8) : Op
data class OpLdHLR8(val r: RegEnum8) : Op
data class OpLdHLD8(val d: Int8) : Op
class OpLdABC : Op
class OpLdADE : Op
data class OpLdAD16(val d: Int16) : Op
class OpLdBCA : Op
class OpLdDEA : Op
data class OpLdD16A(val d: Int16) : Op
data class OpLdFromIoPort(val d: Int8) : Op
data class OpLdToIoPort(val d: Int8) : Op
class OpLdFromIoPortC : Op
class OpLdToIoPortC : Op
class OpLdiHLA : Op
class OpLdiAHL : Op
class OpLddHLA : Op
class OpLddAHL : Op
data class OpLdR16D16(val r: RegEnum16, val d: Int16) : Op
data class OpLdD16SP(val d: Int16) : Op
class OpLdSPHL : Op
data class OpPushR16(val r: RegEnum16) : Op
data class OpPopR16(val r: RegEnum16) : Op

data class OpAddAR8(val r: RegEnum8) : Op
data class OpAddAD8(val d: Int8) : Op
class OpAddAHL : Op
data class OpAdcAR8(val r: RegEnum8) : Op
data class OpAdcAD8(val d: Int8) : Op
class OpAdcAHL : Op
data class OpSubAR8(val r: RegEnum8) : Op
data class OpSubAD8(val d: Int8) : Op
class OpSubAHL : Op
data class OpSbcAR8(val r: RegEnum8) : Op
data class OpSbcAD8(val d: Int8) : Op
class OpSbcAHL : Op
data class OpAndAR8(val r: RegEnum8) : Op
data class OpAndAD8(val d: Int8) : Op
class OpAndAHL : Op
data class OpXorAR8(val r: RegEnum8) : Op
data class OpXorAD8(val d: Int8) : Op
class OpXorAHL : Op
data class OpOrAR8(val r: RegEnum8) : Op
data class OpOrAD8(val d: Int8) : Op
class OpOrAHL : Op
data class OpCpAR8(val r: RegEnum8) : Op
data class OpCpAD8(val d: Int8) : Op
class OpCpAHL : Op
data class OpIncR8(val r: RegEnum8) : Op
class OpIncHL : Op
data class OpDecR8(val r: RegEnum8) : Op
class OpDecHL : Op
class OpDaa : Op
class OpCpl : Op
data class OpAddHLR16(val r: RegEnum16) : Op
data class OpIncR16(val r: RegEnum16) : Op
data class OpDecR16(val r: RegEnum16) : Op
data class OpAddSpD8(val d: Int8) : Op
data class OpLdHLSpAndD8(val d: Int8) : Op
class OpRlcA : Op
class OpRlA : Op
class OpRrcA : Op
class OpRrA : Op
data class OpRlcR8(val r: RegEnum8) : Op
class OpRlcHL : Op
data class OpRlR8(val r: RegEnum8) : Op
class OpRlHL : Op
data class OpRrcR8(val r: RegEnum8) : Op
class OpRrcHL : Op
data class OpRrR8(val r: RegEnum8) : Op
class OpRrHL : Op
data class OpSlaR8(val r: RegEnum8) : Op
class OpSlaHL : Op
data class OpSwapR8(val r: RegEnum8) : Op
class OpSwapHL : Op
data class OpSraR8(val r: RegEnum8) : Op
class OpSraHL : Op
data class OpSrlR8(val r: RegEnum8) : Op
class OpSrlHL : Op
data class OpBitNR8(val n: Int, val r: RegEnum8) : Op
class OpBitNHL(val n: Int) : Op
data class OpSetNR8(val n: Int, val r: RegEnum8) : Op
class OpSetNHL(val n: Int) : Op
data class OpResNR8(val n: Int, val r: RegEnum8) : Op
data class OpResNHL(val n: Int) : Op
class OpCcf : Op
class OpScf : Op
class OpNop : Op
class OpHalt : Op
class OpStop : Op
class OpDi : Op
class OpEi : Op
data class OpJpN16(val n: Int16) : Op
class OpJpHl : Op
data class OpJpFNn(val f: ConditionalJumpFlag, val n: Int16) : Op
data class OpJrD8(val d: Int8) : Op
data class OpJrFD8(val f: ConditionalJumpFlag, val d: Int8) : Op
data class OpCallN16(val n: Int16) : Op
data class OpCallFN16(val f: ConditionalJumpFlag, val n: Int16) : Op
class OpRet : Op
data class OpRetF(val f: ConditionalJumpFlag) : Op
class OpRetI : Op
data class OpRstN8(val n: Num) : Op {
    enum class Num(val v: Int16) {
        N_00(0x00),
        N_08(0x08),
        N_10(0x10),
        N_18(0x18),
        N_20(0x20),
        N_28(0x28),
        N_30(0x30),
        N_38(0x38);
        companion object {
            fun of(v: Int16): Num = Num.values().find { it.v == v }!!
        }
    }
}

fun parseHorizontally(opcode: Int8, range: IntProgression, whenNonHl: (RegEnum8) -> Op, whenHl: () -> Op): Op? {
    val index = range.indexOf(opcode)
    if (index == -1) {
        return null
    }
    if (index == 6) {
        return whenHl()
    }
    return whenNonHl(RegEnum8.fromNum(index)!!)
}

fun parse(memory: Memory, address: Int16): Op {
    val opcode = memory.get8(address)

    // @formatter:off
    // 0x00 - 0x3F
    return run {
        when (opcode) {
            0x00 -> OpNop()
            0x10 -> OpStop()
            0x20 -> OpJrFD8(ConditionalJumpFlag.NZ, memory.get8(address+1))
            0x30 -> OpJrFD8(ConditionalJumpFlag.NC, memory.get8(address+1))
            0x07 -> OpRlcA()
            0x17 -> OpRlA()
            0x27 -> OpDaa()
            0x37 -> OpScf()
            0x08 -> OpLdD16SP(memory.get16(address+1))
            0x18 -> OpJrD8(memory.get8(address+1))
            0x28 -> OpJrFD8(ConditionalJumpFlag.Z, memory.get8(address+1))
            0x38 -> OpJrFD8(ConditionalJumpFlag.C, memory.get8(address+1))
            0x0F -> OpRrcA()
            0x1F -> OpRrA()
            0x2F -> OpCpl()
            0x3F -> OpCcf()
            in 0x01..0x31 step 16 -> {
                val idx = opcode / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP)[idx]
                OpLdR16D16(reg, memory.get16(address + 1))
            }
            in 0x02..0x32 step 16 -> {
                val idx = opcode / 16
                when (idx) {
                    0 -> OpLdBCA()
                    1 -> OpLdDEA()
                    2 -> OpLdiHLA()
                    3 -> OpLddHLA()
                    else -> throw AssertionError()
                }
            }
            in 0x03..0x33 step 16 -> {
                val idx = opcode / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP)[idx]
                OpIncR16(reg)
            }
            else -> null
        }
    } ?:
    parseHorizontally(opcode, 0x04..0x3C step 8, { OpIncR8(it) }, { OpIncHL() }) ?:
    parseHorizontally(opcode, 0x05..0x3D step 8, { OpDecR8(it) }, { OpDecHL() }) ?:
    parseHorizontally(opcode, 0x06..0x3E step 8, { OpLdR8D8(it, memory.get8(address + 1)) }, { OpLdHLD8(memory.get8(address + 1)) }) ?:
    run {
        // 0x40 - 0x7F
        when (opcode) {
            in 0x40..0x7F -> {
                if (opcode in 0x70..0x77) {
                    if (opcode == 0x76) {
                        return@run OpHalt()
                    }
                    val r = RegEnum8.fromNum(opcode.and(0b111))!!
                    return@run OpLdHLR8(r)
                }

                val x = RegEnum8.fromNum(opcode.and(0b111_000).shr(3))!!
                val yNum = opcode.and(0b000_111)
                if (yNum == 0b110) {
                    return@run OpLdR8HL(x)
                }

                val y = RegEnum8.fromNum(yNum)!!
                return@run OpLdR8R8(x, y)
            }
            else -> null
        }
    } ?:
    // 0x80 - 0xBF
    parseHorizontally(opcode, 0x80..0x87, { OpAddAR8(it) }) { OpAddAHL() } ?:
    parseHorizontally(opcode, 0x88..0x8F, { OpAdcAR8(it) }) { OpAdcAHL() } ?:
    parseHorizontally(opcode, 0x90..0x97, { OpSubAR8(it) }) { OpSubAHL() } ?:
    parseHorizontally(opcode, 0x98..0x9F, { OpSbcAR8(it) }) { OpSbcAHL() } ?:
    parseHorizontally(opcode, 0xA0..0xA7, { OpAndAR8(it) }) { OpAndAHL() } ?:
    parseHorizontally(opcode, 0xA8..0xAF, { OpXorAR8(it) }) { OpXorAHL() } ?:
    parseHorizontally(opcode, 0xB0..0xB7, { OpOrAR8(it) }) { OpOrAHL() } ?:
    parseHorizontally(opcode, 0xB8..0xBF, { OpCpAR8(it) }) { OpCpAHL() } ?:
    // 0xC0 - 0xFF
    run {
        when (opcode) {
            in 0xC0..0xD8 step 8 -> {
                val idx = (opcode - 0xC0) / 8
                val f = listOf(ConditionalJumpFlag.NZ, ConditionalJumpFlag.Z, ConditionalJumpFlag.NC, ConditionalJumpFlag.C)[idx]
                OpRetF(f)
            }
            in 0xC2..0xDA step 8 -> {
                val idx = (opcode - 0xC2) / 8
                val f = listOf(ConditionalJumpFlag.NZ, ConditionalJumpFlag.Z, ConditionalJumpFlag.NC, ConditionalJumpFlag.C)[idx]
                OpJpFNn(f, memory.get16(address+1))
            }
            in 0xC4..0xDC step 8 -> {
                val idx = (opcode - 0xC4) / 8
                val f = listOf(ConditionalJumpFlag.NZ, ConditionalJumpFlag.Z, ConditionalJumpFlag.NC, ConditionalJumpFlag.C)[idx]
                OpCallFN16(f, memory.get16(address+1))
            }
            in 0xC7 .. 0xFF step 8 -> {
                val d = (opcode - 0xC7)
                OpRstN8(OpRstN8.Num.of(d))
            }
            in 0xC1..0xF1 step 16 -> {
                val idx = (opcode - 0xC1) / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.AF)[idx]
                OpPopR16(reg)
            }
            in 0xC5..0xF5 step 16 -> {
                val idx = (opcode - 0xC5) / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.AF)[idx]
                OpPushR16(reg)
            }
            0xC3 -> OpJpN16(memory.get16(address+1))
            0xCC -> OpCallN16(memory.get16(address+1))

            0xC6 -> OpAddAD8(memory.get8(address+1))
            0xCE -> OpAdcAD8(memory.get8(address+1))
            0xD6 -> OpSubAD8(memory.get8(address+1))
            0xDE -> OpSbcAD8(memory.get8(address+1))
            0xE6 -> OpAndAD8(memory.get8(address+1))
            0xEE -> OpXorAD8(memory.get8(address+1))
            0xF6 -> OpOrAD8(memory.get8(address+1))
            0xFE -> OpCpAD8(memory.get8(address+1))

            0xE0 -> OpLdToIoPort(memory.get8(address+1))
            0xE2 -> OpLdToIoPortC()

            0xE8 -> OpAddSpD8(memory.get8(address+1))
            0xE9 -> OpJpHl()

            0xF0 -> OpLdFromIoPort(memory.get8(address+1))
            0xF2 -> OpLdFromIoPortC()

            0xF3 -> OpDi()
            0xFB -> OpEi()

            0xF8 -> OpLdHLSpAndD8(memory.get8(address+1))
            0xF9 -> OpLdSPHL()

            0xEA -> OpLdD16A(memory.get16(address+1))
            0xFA -> OpLdAD16(memory.get16(address+1))

            0xCB -> parsePrefixed(memory.get8(address + 1))

            else -> null
        }
    }!!
}

private fun parsePrefixed(int8: Int8): Op {
    return parseHorizontally(int8, 0x00..0x07, { OpRlcR8(it) }) { OpRlcHL() } ?:
    parseHorizontally(int8, 0x08..0x0F, { OpRrcR8(it) }) { OpRrcHL() } ?:
    parseHorizontally(int8, 0x10..0x17, { OpRlR8(it) }) { OpRlHL() } ?:
    parseHorizontally(int8, 0x18..0x1F, { OpRrR8(it) }) { OpRrHL() } ?:
    parseHorizontally(int8, 0x20..0x27, { OpSlaR8(it) }) { OpSlaHL() } ?:
    parseHorizontally(int8, 0x28..0x2F, { OpSraR8(it) }) { OpSraHL() } ?:
    parseHorizontally(int8, 0x30..0x37, { OpSwapR8(it) }) { OpSwapHL() } ?:
    parseHorizontally(int8, 0x38..0x3F, { OpSrlR8(it) }) { OpSrlHL() } ?:
    run {
        when (int8) {
            in 0x40..0x7F -> {
                val n = (int8 - 0x40) / 8
                val start = 0x40 + n * 8
                val end = start + 7
                parseHorizontally(int8, start..end, {OpBitNR8(n, it)}, {OpBitNHL(n)})
            }
            in 0x80..0xBF -> {
                val n = (int8 - 0x80) / 8
                val start = 0x80 + n * 8
                val end = start + 7
                parseHorizontally(int8, start..end, {OpResNR8(n, it)}, {OpResNHL(n)})
            }
            in 0xC0..0xFF -> {
                val n = (int8 - 0xC0) / 8
                val start = 0xC0 + n * 8
                val end = start + 7
                parseHorizontally(int8, start..end, {OpSetNR8(n, it)}, {OpSetNHL(n)})
            }
            else -> throw AssertionError()
        }
    }!!
}
