package emulator.cpu.op

import emulator.cpu.Int16
import emulator.cpu.Int8
import emulator.cpu.objToStringHex

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

abstract class Op {
    override fun toString() = objToStringHex(this)
}
class OpLdR8R8(val x: RegEnum8, val y: RegEnum8) : Op()
class OpLdR8D8(val r: RegEnum8, val d: Int8) : Op()
class OpLdR8HL(val r: RegEnum8) : Op()
class OpLdHLR8(val r: RegEnum8) : Op()
class OpLdHLD8(val d: Int8) : Op()
class OpLdABC : Op()
class OpLdADE : Op()
class OpLdAD16(val d: Int16) : Op()
class OpLdBCA : Op()
class OpLdDEA : Op()
class OpLdD16A(val d: Int16) : Op()
class OpLdFromIoPort(val d: Int8) : Op()
class OpLdToIoPort(val d: Int8) : Op()
class OpLdFromIoPortC : Op()
class OpLdToIoPortC : Op()
class OpLdiHLA : Op()
class OpLdiAHL : Op()
class OpLddHLA : Op()
class OpLddAHL : Op()
class OpLdR16D16(val r: RegEnum16, val d: Int16) : Op()
class OpLdD16SP(val d: Int16) : Op()
class OpLdSPHL : Op()
class OpPushR16(val r: RegEnum16) : Op()
class OpPopR16(val r: RegEnum16) : Op()

class OpAddAR8(val r: RegEnum8) : Op()
class OpAddAD8(val d: Int8) : Op()
class OpAddAHL : Op()
class OpAdcAR8(val r: RegEnum8) : Op()
class OpAdcAD8(val d: Int8) : Op()
class OpAdcAHL : Op()
class OpSubAR8(val r: RegEnum8) : Op()
class OpSubAD8(val d: Int8) : Op()
class OpSubAHL : Op()
class OpSbcAR8(val r: RegEnum8) : Op()
class OpSbcAD8(val d: Int8) : Op()
class OpSbcAHL : Op()
class OpAndAR8(val r: RegEnum8) : Op()
class OpAndAD8(val d: Int8) : Op()
class OpAndAHL : Op()
class OpXorAR8(val r: RegEnum8) : Op()
class OpXorAD8(val d: Int8) : Op()
class OpXorAHL : Op()
class OpOrAR8(val r: RegEnum8) : Op()
class OpOrAD8(val d: Int8) : Op()
class OpOrAHL : Op()
class OpCpAR8(val r: RegEnum8) : Op()
class OpCpAD8(val d: Int8) : Op()
class OpCpAHL : Op()
class OpIncR8(val r: RegEnum8) : Op()
class OpIncHL : Op()
class OpDecR8(val r: RegEnum8) : Op()
class OpDecHL : Op()
class OpDaa : Op()
class OpCpl : Op()
class OpAddHLR16(val r: RegEnum16) : Op()
class OpIncR16(val r: RegEnum16) : Op()
class OpDecR16(val r: RegEnum16) : Op()
class OpAddSpD8(val d: Int8) : Op()
class OpLdHLSpAndD8(val d: Int8) : Op()
class OpRlcA : Op()
class OpRlA : Op()
class OpRrcA : Op()
class OpRrA : Op()
class OpRlcR8(val r: RegEnum8) : Op()
class OpRlcHL : Op()
class OpRlR8(val r: RegEnum8) : Op()
class OpRlHL : Op()
class OpRrcR8(val r: RegEnum8) : Op()
class OpRrcHL : Op()
class OpRrR8(val r: RegEnum8) : Op()
class OpRrHL : Op()
class OpSlaR8(val r: RegEnum8) : Op()
class OpSlaHL : Op()
class OpSwapR8(val r: RegEnum8) : Op()
class OpSwapHL : Op()
class OpSraR8(val r: RegEnum8) : Op()
class OpSraHL : Op()
class OpSrlR8(val r: RegEnum8) : Op()
class OpSrlHL : Op()
class OpBitNR8(val n: Int, val r: RegEnum8) : Op()
class OpBitNHL(val n: Int) : Op()
class OpSetNR8(val n: Int, val r: RegEnum8) : Op()
class OpSetNHL(val n: Int) : Op()
class OpResNR8(val n: Int, val r: RegEnum8) : Op()
class OpResNHL(val n: Int) : Op()
class OpCcf : Op()
class OpScf : Op()
class OpNop : Op()
class OpHalt : Op()
class OpStop : Op()
class OpDi : Op()
class OpEi : Op()
class OpJpN16(val n: Int16) : Op()
class OpJpHl : Op()
class OpJpFNn(val f: ConditionalJumpFlag, val n: Int16) : Op()
class OpJrD8(val d: Int8) : Op()
class OpJrFD8(val f: ConditionalJumpFlag, val d: Int8) : Op()
class OpCallN16(val n: Int16) : Op()
class OpCallFN16(val f: ConditionalJumpFlag, val n: Int16) : Op()
class OpRet : Op()
class OpRetF(val f: ConditionalJumpFlag) : Op()
class OpRetI : Op()
class OpRstN8(val n: Num) : Op() {
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
