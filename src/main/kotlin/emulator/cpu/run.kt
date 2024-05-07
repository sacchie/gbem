package emulator.cpu

import emulator.cpu.op.*

interface Memory {
    fun get8(addr: Int16): Int8

    fun get16(addr: Int16): Int16

    fun set8(addr: Int16, int8: Int8)

    fun set16(addr: Int16, int16: Int16)

    fun getIfForDebug(): Int8
}

interface Registers {
    fun getAf(): Int16
    fun setAf(x: Int16)
    fun getBc(): Int16
    fun setBc(x: Int16)
    fun getDe(): Int16
    fun setDe(x: Int16)
    fun getHl(): Int16
    fun setHl(x: Int16)
    fun getSp(): Int16
    fun setSp(x: Int16)

    fun getPc(): Int16
    fun setPc(x: Int16)

    fun getIme(): Boolean
    fun setIme(on: Boolean)


    // flag register
    fun isZeroOn(): Boolean
    fun setZero(on: Boolean)
    fun isSubtractionOn(): Boolean
    fun setSubtraction(on: Boolean)
    fun isHalfCarryOn(): Boolean
    fun setHalfCarry(on: Boolean)
    fun isCarryOn(): Boolean
    fun setCarry(on: Boolean)


    fun incCallDepthForDebug(diff: Int = 1)
}

fun Registers.updateHl(fn: (x: Int16) -> Int16) = setHl(fn(getHl()))
fun Registers.updateSp(fn: (x: Int16) -> Int16) = setSp(fn(getSp()))
fun Registers.incPc(diff: Int16 = 1) = setPc(diff + getPc())
fun Registers.getA() = getAf().hi()
fun Registers.setA(x: Int8) = setAf(int16FromHiAndLo(x, getAf().lo()))
fun Registers.getB() = getBc().hi()
fun Registers.setB(x: Int8) = setBc(int16FromHiAndLo(x, getBc().lo()))
fun Registers.getC() = getBc().lo()
fun Registers.setC(x: Int8) = setBc(int16FromHiAndLo(getBc().hi(), x))
fun Registers.getD() = getDe().hi()
fun Registers.setD(x: Int8) = setDe(int16FromHiAndLo(x, getDe().lo()))
fun Registers.getE() = getDe().lo()
fun Registers.setE(x: Int8) = setDe(int16FromHiAndLo(getDe().hi(), x))
fun Registers.getH() = getHl().hi()
fun Registers.setH(x: Int8) = setHl(int16FromHiAndLo(x, getHl().lo()))
fun Registers.getL() = getHl().lo()
fun Registers.setL(x: Int8) = setHl(int16FromHiAndLo(getHl().hi(), x))

interface Place<T> {
    fun get(): T
    fun set(x: T)

}

fun Registers.a() = object : Place<Int8> {
    override fun get(): Int8 = getA()
    override fun set(x: Int8) = setA(x)
}

fun Registers.r8(r: RegEnum8) = when (r) {
    RegEnum8.B -> object : Place<Int8> {
        override fun get(): Int8 = getB()
        override fun set(x: Int8) = setB(x)
    }
    RegEnum8.C -> object : Place<Int8> {
        override fun get(): Int8 = getC()
        override fun set(x: Int8) = setC(x)
    }
    RegEnum8.D -> object : Place<Int8> {
        override fun get(): Int8 = getD()
        override fun set(x: Int8) = setD(x)

    }
    RegEnum8.E -> object : Place<Int8> {
        override fun get(): Int8 = getE()
        override fun set(x: Int8) = setE(x)
    }
    RegEnum8.H -> object : Place<Int8> {
        override fun get(): Int8 = getH()
        override fun set(x: Int8) = setH(x)
    }
    RegEnum8.L -> object : Place<Int8> {
        override fun get(): Int8 = getL()
        override fun set(x: Int8) = setL(x)
    }
    RegEnum8.A -> a()
}

fun Registers.r16(r: RegEnum16) = when (r) {
    RegEnum16.AF -> object : Place<Int16> {
        override fun get(): Int16 = getAf()
        override fun set(x: Int16) = setAf(x)
    }
    RegEnum16.BC -> object : Place<Int16> {
        override fun get(): Int16 = getBc()
        override fun set(x: Int16) = setBc(x)
    }
    RegEnum16.DE -> object : Place<Int16> {
        override fun get(): Int16 = getDe()
        override fun set(x: Int16) = setDe(x)
    }
    RegEnum16.HL -> object : Place<Int16> {
        override fun get(): Int16 = getHl()
        override fun set(x: Int16) = setHl(x)

    }
    RegEnum16.SP -> object : Place<Int16> {
        override fun get(): Int16 = getSp()
        override fun set(x: Int16) = setSp(x)
    }
}

fun Memory.asPlace8(addr: Int16) = object : Place<Int8> {
    override fun get(): Int8 = get8(addr)
    override fun set(x: Int8) = set8(addr, x)
}

fun opAddA(regs: Registers, d: Int8, cy: Int8 = 0) {
    val aOld = regs.getA()
    val aNew = aOld + d + cy
    val aSet = aNew % 0x100
    regs.setA(aSet)
    regs.setZero(aSet == 0)
    regs.setSubtraction(false)
    regs.setCarry(0x100 <= aNew)
    regs.setHalfCarry(aOld.and(0xF) + d.and(0xF) + cy > 0xF)
}

fun opSubA(regs: Registers, d: Int8, cy: Int8 = 0) {
    val aOld = regs.getA()
    val aNew = aOld - d - cy
    val aSet = (aNew + 0x100) % 0x100
    regs.setA(aSet)
    regs.setZero(aSet == 0)
    regs.setSubtraction(true)
    regs.setCarry(aNew < 0)
    regs.setHalfCarry(aOld.and(0xF) - d.and(0xF) - cy < 0)
}

fun opAndA(regs: Registers, d: Int8) {
    val aSet = regs.getA().and(d)
    regs.setA(aSet)
    regs.setZero(aSet == 0)
    regs.setSubtraction(false)
    regs.setCarry(false)
    regs.setHalfCarry(true);
}

fun opXorA(regs: Registers, d: Int8) {
    val aSet = regs.getA().xor(d)
    regs.setA(aSet)
    regs.setZero(aSet == 0)
    regs.setSubtraction(false)
    regs.setCarry(false)
    regs.setHalfCarry(false);
}

fun opOrA(regs: Registers, d: Int8) {
    val aSet = regs.getA().or(d)
    regs.setA(aSet)
    regs.setZero(aSet == 0)
    regs.setSubtraction(false)
    regs.setCarry(false)
    regs.setHalfCarry(false);
}

fun opCpA(regs: Registers, d: Int8) {
    val aOld = regs.getA()
    val aNew = aOld - d
    val aResult = (aNew + 0x100) % 0x100
    regs.setZero(aResult == 0)
    regs.setSubtraction(true)
    regs.setCarry(aNew < 0)
    regs.setHalfCarry(aOld.and(0xF) - d.and(0xF) < 0)
}

fun opInc(regs: Registers, place: Place<Int8>) {
    val oldVal = place.get()
    val setVal = (oldVal + 1) % 0x100
    place.set(setVal)
    regs.setZero(setVal == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(oldVal.and(0xF) + 1.and(0xF) > 0xF)
}

fun opDec(regs: Registers, place: Place<Int8>) {
    val oldVal = place.get()
    val setVal = ((oldVal - 1) + 0x100) % 0x100
    place.set(setVal)
    regs.setZero(setVal == 0)
    regs.setSubtraction(true)
    regs.setHalfCarry(oldVal.and(0xF) - 1.and(0xF) < 0)
}

fun opRlc(regs: Registers, place: Place<Int8>, forceZeroOff: Boolean) {
    val aOld = place.get()
    val aNew = aOld.shl(1) + aOld.shr(7)
    val aSet = aNew % 0x100
    place.set(aSet)
    regs.setZero(if (forceZeroOff) false else aSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(0xFF < aNew)
}

fun opRl(regs: Registers, place: Place<Int8>, forceZeroOff: Boolean) {
    val vOld = place.get()
    val vNew = vOld.shl(1) + if (regs.isCarryOn()) 1 else 0
    val vSet = vNew % 0x100
    place.set(vSet)
    regs.setZero(if (forceZeroOff) false else vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(0xFF < vNew)
}

fun opRrc(regs: Registers, place: Place<Int8>, forceZeroOff: Boolean) {
    val vOld = place.get()
    val vNew = vOld.shr(1) + vOld.shl(7).and(0xFF)
    val vSet = vNew % 0x100
    place.set(vSet)
    regs.setZero(if (forceZeroOff) false else vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(vOld.and(1) == 1)
}

fun opRr(regs: Registers, place: Place<Int8>, forceZeroOff: Boolean) {
    val vOld = place.get()
    val vNew = vOld.shr(1) + (if (regs.isCarryOn()) 1 else 0).shl(7)
    val vSet = vNew % 0x100
    place.set(vSet)
    regs.setZero(if (forceZeroOff) false else vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(vOld.and(1) == 1)
}

fun opSwap(regs: Registers, place: Place<Int8>) {
    val vOld = place.get()
    val vOldHigh = vOld.shr(4)
    val vOldLow = vOld.and(0xF)
    val vSet = vOldLow.shl(4).or(vOldHigh)
    place.set(vSet)
    regs.setZero(vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(false)
}

fun opSra(regs: Registers, place: Place<Int8>) {
    val vOld = place.get()
    val vSet = vOld.shr(1) + vOld.and(0b10000000)
    place.set(vSet)
    regs.setZero(vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(vOld.and(1) == 1)
}

fun opSrl(regs: Registers, place: Place<Int8>) {
    val vOld = place.get()
    val vSet = vOld.shr(1)
    place.set(vSet)
    regs.setZero(vSet == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(false)
    regs.setCarry(vOld.and(1) == 1)
}

fun opBitN(n: Int, regs: Registers, get: () -> Int8) {
    val vOld = get()
    val vNew = vOld.and(1.shl(n))
    regs.setZero((vNew % 0x100) == 0)
    regs.setSubtraction(false)
    regs.setHalfCarry(true)
}

fun opSetN(n: Int, place: Place<Int8>) {
    place.set(place.get().or(1.shl(n)))
}

fun opResN(n: Int, place: Place<Int8>) {
    place.set(place.get().and(1.shl(n).inv()))
}

fun opCall(regs: Registers, memory: Memory, n: Int16) {
    regs.setSp(regs.getSp() - 2)
    memory.set16(regs.getSp(), regs.getPc() + 3)
    regs.setPc(n)
    regs.incCallDepthForDebug()
}

fun opRst(regs: Registers, memory: Memory, n: OpRstN8.Num) {
    regs.setSp(regs.getSp() - 2)
    memory.set16(regs.getSp(), regs.getPc() + 1)
    regs.setPc(n.v)
    regs.incCallDepthForDebug()
}

fun opRet(regs: Registers, memory: Memory) {
    val pc = memory.get16(regs.getSp())
    regs.setPc(pc)
    regs.setSp(regs.getSp() + 2)
    regs.incCallDepthForDebug(-1)
}

fun runIfConditionSatisfied(regs: Registers, f: ConditionalJumpFlag, thenDo: () -> Unit, elseDo: () -> Unit) {
    if (f == ConditionalJumpFlag.NZ && !regs.isZeroOn()
        || f == ConditionalJumpFlag.Z && regs.isZeroOn()
        || f == ConditionalJumpFlag.NC && !regs.isCarryOn()
        || f == ConditionalJumpFlag.C && regs.isCarryOn()
    ) {
        thenDo()
    } else {
        elseDo()
    }
}

interface HaltState {
    fun setHalted(b: Boolean)
    fun getHalted(): Boolean
}

fun handleInterrupts(memory: Memory, regs: Registers, haltState: HaltState) {
    fun getIE() = memory.get8(0xFFFF)

    //    fun getIF() = memory.get8(0xFF0F)
    fun getIF() = memory.getIfForDebug() // FIXME 不要になったら消す
    fun setIF(v: Int8) = memory.set8(0xFF0F, v)

    if (0 < getIE().and(0b1) && 0 < getIF().and(0b1)) {
        if (haltState.getHalted()) {
            regs.incPc()
            haltState.setHalted(false)
        }

        if (regs.getIme()) {
            setIF(getIF().and(0b11111110))
            regs.setSp(regs.getSp() - 2)
            memory.set16(regs.getSp(), regs.getPc())
            regs.setPc(0x40)
            regs.setIme(false)
            haltState.setHalted(false)
        }
    } else if (0 < getIE().and(0b10) && 0 < getIF().and(0b10)) {
        if (haltState.getHalted()) {
            regs.incPc()
            haltState.setHalted(false)
        }

        if (regs.getIme()) {
            setIF(getIF().and(0b11111101))
            regs.setSp(regs.getSp() - 2)
            memory.set16(regs.getSp(), regs.getPc())
            regs.setPc(0x48)
            regs.setIme(false)
            haltState.setHalted(false)
        }
    } else if (0 < getIE().and(0b100) && 0 < getIF().and(0b100)) {
        if (haltState.getHalted()) {
            regs.incPc()
            haltState.setHalted(false)
        }

        if (regs.getIme()) {
            setIF(getIF().and(0b11111011))
            regs.setSp(regs.getSp() - 2)
            memory.set16(regs.getSp(), regs.getPc())
            regs.setPc(0x50)
            regs.setIme(false)
            haltState.setHalted(false)
        }
    }
}

fun Op.run(regs: Registers, memory: Memory, haltState: HaltState) {
    when (this) {
        is OpLdR8R8 -> {
            val yVal = regs.r8(y).get()
            regs.r8(x).set(yVal)
            regs.incPc()
        }

        is OpLdR8D8 -> {
            regs.r8(r).set(d)
            regs.incPc(2)
        }

        is OpLdR8HL -> {
            regs.r8(r).set(memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpLdHLR8 -> {
            memory.set8(regs.getHl(), regs.r8(r).get())
            regs.incPc()
        }

        is OpLdHLD8 -> {
            memory.set8(regs.getHl(), d)
            regs.incPc(2)
        }

        is OpLdABC -> {
            regs.setA(memory.get8(regs.getBc()))
            regs.incPc()
        }

        is OpLdADE -> {
            regs.setA(memory.get8(regs.getDe()))
            regs.incPc()
        }

        is OpLdAD16 -> {
            regs.setA(memory.get8(d))
            regs.incPc(3)
        }

        is OpLdBCA -> {
            memory.set8(regs.getBc(), regs.getA())
            regs.incPc()
        }

        is OpLdDEA -> {
            memory.set8(regs.getDe(), regs.getA())
            regs.incPc()
        }

        is OpLdD16A -> {
            memory.set8(d, regs.getA())
            regs.incPc(3)
        }

        is OpLdFromIoPort -> {
            regs.setA(memory.get8(0xFF00 + d))
            regs.incPc(2)
        }

        is OpLdToIoPort -> {
            memory.set8(0xFF00 + d, regs.getA())
            regs.incPc(2)
        }

        is OpLdFromIoPortC -> {
            regs.setA(memory.get8(0xFF00 + regs.getC()))
            regs.incPc()
        }

        is OpLdToIoPortC -> {
            memory.set8(0xFF00 + regs.getC(), regs.getA())
            regs.incPc()
        }

        is OpLdiHLA -> {
            memory.set8(regs.getHl(), regs.getA())
            regs.updateHl { (it + 1) % 0x10000 }
            regs.incPc()
        }

        is OpLdiAHL -> {
            regs.setA(memory.get8(regs.getHl()))
            regs.updateHl { (it + 1) % 0x10000 }
            regs.incPc()
        }

        is OpLddHLA -> {
            memory.set8(regs.getHl(), regs.getA())
            regs.updateHl { if (it == 0) 0xFFFF else it - 1 }
            regs.incPc()
        }

        is OpLddAHL -> {
            regs.setA(memory.get8(regs.getHl()))
            regs.updateHl { if (it == 0) 0xFFFF else it - 1 }
            regs.incPc()
        }

        is OpLdR16D16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.SP)
            regs.r16(r).set(d)
            regs.incPc(3)
        }

        is OpLdD16SP -> {
            memory.set16(d, regs.getSp())
            regs.incPc(3)
        }

        is OpLdSPHL -> {
            regs.setSp(regs.getHl())
            regs.incPc()
        }

        is OpPushR16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.AF)
            regs.updateSp { if (it < 2) it + 0x10000 - 2 else it - 2 }
            memory.set16(regs.getSp(), regs.r16(r).get())
            regs.incPc()
        }

        is OpPopR16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.AF)
            val popValue = memory.get16(regs.getSp())
            val setValue = if (r == RegEnum16.AF) popValue and 0xFFF0 else popValue
            regs.r16(r).set(setValue)
            regs.updateSp { (it + 2) % 0x10000 }
            regs.incPc()
        }

        is OpAddAR8 -> {
            opAddA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpAddAD8 -> {
            opAddA(regs, d)
            regs.incPc(2)
        }

        is OpAddAHL -> {
            opAddA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpAdcAR8 -> {
            opAddA(regs, regs.r8(r).get(), if (regs.isCarryOn()) 1 else 0)
            regs.incPc()
        }

        is OpAdcAD8 -> {
            opAddA(regs, d, if (regs.isCarryOn()) 1 else 0)
            regs.incPc(2)
        }

        is OpAdcAHL -> {
            opAddA(regs, memory.get8(regs.getHl()), if (regs.isCarryOn()) 1 else 0)
            regs.incPc()
        }

        is OpSubAR8 -> {
            opSubA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpSubAD8 -> {
            opSubA(regs, d)
            regs.incPc(2)
        }

        is OpSubAHL -> {
            opSubA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpSbcAR8 -> {
            opSubA(regs, regs.r8(r).get(), if (regs.isCarryOn()) 1 else 0)
            regs.incPc()
        }

        is OpSbcAD8 -> {
            opSubA(regs, d, if (regs.isCarryOn()) 1 else 0)
            regs.incPc(2)
        }

        is OpSbcAHL -> {
            opSubA(regs, memory.get8(regs.getHl()), if (regs.isCarryOn()) 1 else 0)
            regs.incPc()
        }

        is OpAndAR8 -> {
            opAndA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpAndAD8 -> {
            opAndA(regs, d)
            regs.incPc(2)
        }

        is OpAndAHL -> {
            opAndA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpXorAR8 -> {
            opXorA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpXorAD8 -> {
            opXorA(regs, d)
            regs.incPc(2)
        }

        is OpXorAHL -> {
            opXorA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpOrAR8 -> {
            opOrA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpOrAD8 -> {
            opOrA(regs, d)
            regs.incPc(2)
        }

        is OpOrAHL -> {
            opOrA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpCpAR8 -> {
            opCpA(regs, regs.r8(r).get())
            regs.incPc()
        }

        is OpCpAD8 -> {
            opCpA(regs, d)
            regs.incPc(2)
        }

        is OpCpAHL -> {
            opCpA(regs, memory.get8(regs.getHl()))
            regs.incPc()
        }

        is OpIncR8 -> {
            opInc(regs, regs.r8(r))
            regs.incPc()
        }

        is OpIncHL -> {
            opInc(regs, memory.asPlace8(regs.getHl()))
            regs.incPc()
        }

        is OpDecR8 -> {
            opDec(regs, regs.r8(r))
            regs.incPc()
        }

        is OpDecHL -> {
            opDec(regs, memory.asPlace8(regs.getHl()))
            regs.incPc()
        }

        is OpDaa -> {
            var corr = 0
                .or(if (regs.isHalfCarryOn()) 0x06 else 0x00)
                .or(if (regs.isCarryOn()) 0x60 else 0x00)

            val aOld = regs.getA()
            val aNew = if (regs.isSubtractionOn()) {
                aOld - corr
            } else {
                corr = corr
                    .or(if (aOld.and(0x0F) > 0x09) 0x06 else 0x00)
                    .or(if (aOld > 0x99) 0x60 else 0x00)
                aOld + corr
            }

            regs.setHalfCarry(false)
            regs.setZero(aNew.and(0xFF) == 0)
            regs.setCarry(corr.and(0x60) != 0)
            regs.setA(aNew.and(0xFF))
            regs.incPc()
        }

        is OpCpl -> {
            regs.setA(regs.getA().inv().and(0xFF))
            regs.setSubtraction(true)
            regs.setHalfCarry(true)
            regs.incPc()
        }

        is OpAddHLR16 -> {
            val hlOld = regs.getHl()
            val hlNew = hlOld + regs.r16(r).get()
            val hlSet = hlNew % 0x10000
            regs.setHl(hlSet)
            regs.setSubtraction(false)
            regs.setCarry(0x10000 <= hlNew)
            regs.setHalfCarry(0xFFF < hlOld.and(0xFFF) + regs.r16(r).get().and(0xFFF))
            regs.incPc()
        }

        is OpIncR16 -> {
            assert(arrayOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP).contains(r))
            val reg16 = regs.r16(r)
            reg16.set((reg16.get() + 1) % 0x10000)
            regs.incPc()
        }

        is OpDecR16 -> {
            assert(arrayOf(RegEnum16.BC, RegEnum16.DE, RegEnum16.HL, RegEnum16.SP).contains(r))
            val reg16 = regs.r16(r)
            reg16.set(((reg16.get() - 1) + 0x10000) % 0x10000)
            regs.incPc()
        }

        is OpAddSpD8 -> {
            val spOld = regs.getSp()
            val spNew = if (d < 0x80) spOld + d else spOld - (0x100 - d)
            val spSet = (spNew + 0x10000) % 0x10000
            regs.setSp(spSet)
            regs.setZero(false)
            regs.setSubtraction(false)
            regs.setHalfCarry(0xF < (spOld.and(0xF) + d.and(0xF)))
            regs.setCarry(0xFF < (spOld.and(0xFF) + d.and(0xFF)))
            regs.incPc(2)
        }

        is OpLdHLSpAndD8 -> {
            val spVal = regs.getSp()
            val hlNew = if (d < 0x80) spVal + d else spVal - (0x100 - d)
            val hlSet = (hlNew + 0x10000) % 0x10000
            regs.setHl(hlSet)
            regs.setZero(false)
            regs.setSubtraction(false)
            regs.setHalfCarry(0xF < (spVal.and(0xF) + d.and(0xF)))
            regs.setCarry(0xFF < (spVal.and(0xFF) + d.and(0xFF)))
            regs.incPc(2)
        }

        is OpRlcA -> {
            opRlc(regs, regs.a(), true)
            regs.incPc()
        }

        is OpRlA -> {
            opRl(regs, regs.a(), true)
            regs.incPc()
        }

        is OpRrcA -> {
            opRrc(regs, regs.a(), true)
            regs.incPc()
        }

        is OpRrA -> {
            opRr(regs, regs.a(), true)
            regs.incPc()
        }

        is OpRlcR8 -> {
            opRlc(regs, regs.r8(r), false)
            regs.incPc(2)
        }

        is OpRlcHL -> {
            val addr = regs.getHl()
            opRlc(regs, memory.asPlace8(addr), false)
            regs.incPc(2)
        }

        is OpRlR8 -> {
            opRl(regs, regs.r8(r), false)
            regs.incPc(2)
        }

        is OpRlHL -> {
            val addr = regs.getHl()
            opRl(regs, memory.asPlace8(addr), false)
            regs.incPc(2)
        }

        is OpRrcR8 -> {
            opRrc(regs, regs.r8(r), false)
            regs.incPc(2)
        }

        is OpRrcHL -> {
            val addr = regs.getHl()
            opRrc(regs, memory.asPlace8(addr), false)
            regs.incPc(2)
        }

        is OpRrR8 -> {
            opRr(regs, regs.r8(r), false)
            regs.incPc(2)
        }

        is OpRrHL -> {
            val addr = regs.getHl()
            opRr(regs, memory.asPlace8(addr), false)
            regs.incPc(2)
        }

        is OpSlaR8 -> {
            val reg = regs.r8(r)
            val vOld = reg.get()
            val vNew = vOld.shl(1)
            val vSet = vNew % 0x100
            reg.set(vSet)
            regs.setZero(vSet == 0)
            regs.setSubtraction(false)
            regs.setHalfCarry(false)
            regs.setCarry(0xFF < vNew)
            regs.incPc(2)
        }

        is OpSlaHL -> {
            val vOld = memory.get8(regs.getHl())
            val vNew = vOld.shl(1)
            val vSet = vNew % 0x100
            memory.set8(regs.getHl(), vSet)
            regs.setZero(vSet == 0)
            regs.setSubtraction(false)
            regs.setHalfCarry(false)
            regs.setCarry(0xFF < vNew)
            regs.incPc(2)
        }

        is OpSwapR8 -> {
            opSwap(regs, regs.r8(r))
            regs.incPc(2)
        }

        is OpSwapHL -> {
            opSwap(regs, memory.asPlace8(regs.getHl()))
            regs.incPc(2)
        }

        is OpSraR8 -> {
            opSra(regs, regs.r8(r))
            regs.incPc(2)
        }

        is OpSraHL -> {
            opSra(regs, memory.asPlace8(regs.getHl()))
            regs.incPc(2)
        }

        is OpSrlR8 -> {
            opSrl(regs, regs.r8(r))
            regs.incPc(2)
        }

        is OpSrlHL -> {
            opSrl(regs, memory.asPlace8(regs.getHl()))
            regs.incPc(2)
        }

        is OpBitNR8 -> {
            opBitN(n, regs, { regs.r8(r).get() })
            regs.incPc(2)
        }

        is OpBitNHL -> {
            opBitN(n, regs, { memory.get8(regs.getHl()) })
            regs.incPc(2)
        }

        is OpSetNR8 -> {
            opSetN(n, regs.r8(r))
            regs.incPc(2)
        }

        is OpSetNHL -> {
            opSetN(n, memory.asPlace8(regs.getHl()))
            regs.incPc(2)
        }

        is OpResNR8 -> {
            opResN(n, regs.r8(r))
            regs.incPc(2)
        }

        is OpResNHL -> {
            opResN(n, memory.asPlace8(regs.getHl()))
            regs.incPc(2)
        }

        is OpCcf -> {
            regs.setCarry(!regs.isCarryOn())
            regs.setHalfCarry(false)
            regs.setSubtraction(false)
            regs.incPc()
        }

        is OpScf -> {
            regs.setCarry(true)
            regs.setHalfCarry(false)
            regs.setSubtraction(false)
            regs.incPc()
        }

        is OpNop -> {
            regs.incPc()
        }

        is OpHalt -> {
            haltState.setHalted(true)
        }

        is OpStop -> {
            regs.incPc(2)
            throw UnsupportedOperationException()
        }

        is OpDi -> {
            regs.setIme(false)
            regs.incPc()
        }

        is OpEi -> {
            regs.setIme(true)
            regs.incPc()
        }

        is OpJpN16 -> {
            regs.setPc(n)
        }

        is OpJpHl -> {
            regs.setPc(regs.getHl())
        }

        is OpJpFNn -> {
            runIfConditionSatisfied(regs, f, {
                regs.setPc(n)
            }) {
                regs.incPc(3)
            }
        }

        is OpJrD8 -> {
            regs.setPc(2 + regs.getPc() + (d.xor(0x80) - 0x80))
        }

        is OpJrFD8 -> {
            runIfConditionSatisfied(regs, f, {
                regs.setPc(2 + regs.getPc() + (d.xor(0x80) - 0x80))
            }) {
                regs.incPc(2)
            }
        }

        is OpCallN16 -> {
            opCall(regs, memory, n)
        }

        is OpCallFN16 -> {
            runIfConditionSatisfied(regs, f, { opCall(regs, memory, n) }) {
                regs.incPc(3)
            }
        }

        is OpRet -> {
            opRet(regs, memory)
        }

        is OpRetF -> {
            runIfConditionSatisfied(regs, f, { opRet(regs, memory) }) {
                regs.incPc()
            }
        }

        is OpRetI -> {
            regs.setIme(true)
            opRet(regs, memory)
        }

        is OpRstN8 -> {
            opRst(regs, memory, n)
        }
    }
}

fun Op.getCycleCount() = when (this) {
    is OpLdR8R8 -> 4
    else -> 8
}
