package cpu.op

import cpu.Int16
import cpu.Int8
import cpu.Memory

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
            in 0x09..0x39 step 16 -> {
                val idx = opcode / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP)[idx]
                OpAddHLR16(reg)
            }
            in 0x0A..0x3A step 16 -> {
                val idx = opcode / 16
                when (idx) {
                    0 -> OpLdABC()
                    1 -> OpLdADE()
                    2 -> OpLdiAHL()
                    3 -> OpLddAHL()
                    else -> throw AssertionError()
                }
            }
            in 0x0B..0x3B step 16 -> {
                val idx = opcode / 16
                val reg = listOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP)[idx]
                OpDecR16(reg)
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
            0xC9 -> OpRet()
            0xD9 -> OpRetI()
            0xCD -> OpCallN16(memory.get16(address+1))

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
    } ?: run {
        throw RuntimeException("Invalid opcode: ${opcode.toString(16)}, addr: ${address.toString(16)}")
    }
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
