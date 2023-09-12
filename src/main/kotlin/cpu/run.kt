package cpu

fun Registers.gpr8(r: RegEnum8): GPR<Int8> = when (r) {
    RegEnum8.B -> b()
    RegEnum8.C -> c()
    RegEnum8.D -> d()
    RegEnum8.E -> e()
    RegEnum8.H -> h()
    RegEnum8.L -> l()
    RegEnum8.A -> a()
}

fun Registers.gpr16(r: RegEnum16): GPR<Int16> = when (r) {
    RegEnum16.AF -> af()
    RegEnum16.BC -> bc()
    RegEnum16.DE -> de()
    RegEnum16.HL -> hl()
    RegEnum16.SP -> sp()
}

fun opAddA(regs: Registers, d: Int8, cy: Int8 = 0) {
    val aOld = regs.a().get()
    val aNew = aOld + d + cy
    val aSet = aNew % 0x100
    regs.a().set(aSet)
    regs.flag().setZero(aSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setCarry(0x100 <= aNew)
    regs.flag().setHalfCarry(aOld.and(0xF) + d.and(0xF) + cy > 0xF)
}

fun opSubA(regs: Registers, d: Int8, cy: Int8 = 0) {
    val aOld = regs.a().get()
    val aNew = aOld - d - cy
    val aSet = (aNew + 0x100) % 0x100
    regs.a().set(aSet)
    regs.flag().setZero(aSet == 0)
    regs.flag().setSubtraction(true)
    regs.flag().setCarry(aNew < 0)
    regs.flag().setHalfCarry(aOld.and(0xF) - d.and(0xF) - cy < 0)
}

fun opAndA(regs: Registers, d: Int8) {
    val aSet = regs.a().get().and(d)
    regs.a().set(aSet)
    regs.flag().setZero(aSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setCarry(false)
    regs.flag().setHalfCarry(true);
}

fun opXorA(regs: Registers, d: Int8) {
    val aSet = regs.a().get().xor(d)
    regs.a().set(aSet)
    regs.flag().setZero(aSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setCarry(false)
    regs.flag().setHalfCarry(false);
}

fun opOrA(regs: Registers, d: Int8) {
    val aSet = regs.a().get().or(d)
    regs.a().set(aSet)
    regs.flag().setZero(aSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setCarry(false)
    regs.flag().setHalfCarry(false);
}

fun opCpA(regs: Registers, d: Int8) {
    val aOld = regs.a().get()
    val aNew = aOld - d
    val aResult = (aNew + 0x100) % 0x100
    regs.flag().setZero(aResult == 0)
    regs.flag().setSubtraction(true)
    regs.flag().setCarry(aNew < 0)
    regs.flag().setHalfCarry(aOld.and(0xF) - d.and(0xF) < 0)
}

fun opInc(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit) {
    val oldVal = get()
    val setVal = (oldVal + 1) % 0x100
    set(setVal)
    regs.flag().setZero(setVal == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(oldVal.and(0xF) + 1.and(0xF) > 0xF)
}

fun opDec(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit) {
    val oldVal = get()
    val setVal = ((oldVal - 1) + 0x100) % 0x100
    set(setVal)
    regs.flag().setZero(setVal == 0)
    regs.flag().setSubtraction(true)
    regs.flag().setHalfCarry(oldVal.and(0xF) - 1.and(0xF) < 0)
}

fun opRlc(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit, forceZeroOff: Boolean) {
    val aOld = get()
    val aNew = aOld.shl(1) + aOld.shr(7)
    val aSet = aNew  % 0x100
    set(aSet)
    regs.flag().setZero(if (forceZeroOff) false else aSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(0xFF < aNew)
}

fun opRl(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit, forceZeroOff: Boolean) {
    val vOld = get()
    val vNew = vOld.shl(1) + if (regs.flag().isCarryOn()) 1 else 0
    val vSet = vNew % 0x100
    set(vSet)
    regs.flag().setZero(if (forceZeroOff) false else vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(0xFF < vNew)
}

fun opRrc(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit, forceZeroOff: Boolean) {
    val vOld = get()
    val vNew = vOld.shr(1) + vOld.shl(7).and(0xFF)
    val vSet = vNew  % 0x100
    set(vSet)
    regs.flag().setZero(if (forceZeroOff) false else vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(vOld.and(1) == 1)
}

fun opRr(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit, forceZeroOff: Boolean) {
    val vOld = get()
    val vNew = vOld.shr(1) + (if (regs.flag().isCarryOn()) 1 else 0).shl(7)
    val vSet = vNew % 0x100
    set(vSet)
    regs.flag().setZero(if (forceZeroOff) false else vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(vOld.and(1) == 1)
}

fun opSwap(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit) {
    val vOld = get()
    val vOldHigh = vOld.shr(4)
    val vOldLow = vOld.and(0xF)
    val vSet = vOldLow.shl(4).or(vOldHigh)
    set(vSet)
    regs.flag().setZero(vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(false)
}

fun opSra(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit) {
    val vOld = get()
    val vSet = vOld.shr(1) + vOld.and(0b10000000)
    set(vSet)
    regs.flag().setZero(vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(vOld.and(1) == 1)
}

fun opSrl(regs: Registers, get: () -> Int8, set: (d: Int8) -> Unit) {
    val vOld = get()
    val vSet = vOld.shr(1)
    set(vSet)
    regs.flag().setZero(vSet == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(false)
    regs.flag().setCarry(vOld.and(1) == 1)
}

fun opBitN(n: Int, regs: Registers, get: () -> Int8) {
    val vOld = get()
    val vNew = vOld.and(1.shl(n))
    regs.flag().setZero((vNew % 0x100) == 0)
    regs.flag().setSubtraction(false)
    regs.flag().setHalfCarry(true)
}

fun opSetN(n: Int, get: () -> Int8,  set: (d: Int8) -> Unit) {
    set(get().or(1.shl(n)))
}

fun opResN(n: Int, get: () -> Int8,  set: (d: Int8) -> Unit) {
    set(get().and(1.shl(n).inv()))
}

fun opCall(regs: Registers, memory: Memory, n: Int16) {
    regs.sp().set(regs.sp().get() - 2)
    memory.set16(regs.sp().get(), regs.pc().get() + 3)
    regs.pc().set(n)
    regs.callDepthForDebug += 1
}

fun opRet(regs: Registers, memory: Memory) {
    val pc = memory.get16(regs.sp().get())
    regs.pc().set(pc)
    regs.sp().set(regs.sp().get() + 2)
    regs.callDepthForDebug -= 1
}

fun runIfConditionSatisfied(regs: Registers, f: ConditionalJumpFlag, thenDo: () -> Unit, elseDo: () -> Unit) {
    if (f == ConditionalJumpFlag.NZ && !regs.flag().isZeroOn()
        || f == ConditionalJumpFlag.Z && regs.flag().isZeroOn()
        || f == ConditionalJumpFlag.NC && !regs.flag().isCarryOn()
        || f == ConditionalJumpFlag.C && !regs.flag().isCarryOn()) {
        thenDo()
    } else {
        elseDo()
    }
}

fun Op.run(regs: Registers, memory: Memory) {
    when (this) {
        is OpLdR8R8 -> {
            val yVal = regs.gpr8(y).get()
            regs.gpr8(x).set(yVal)
            regs.pc().inc()
        }

        is OpLdR8D8 -> {
            regs.gpr8(r).set(d)
            regs.pc().inc(2)
        }

        is OpLdR8HL -> {
            regs.gpr8(r).set(memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpLdHLR8 -> {
            memory.set8(regs.hl().get(), regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpLdHLD8 -> {
            memory.set8(regs.hl().get(), d)
            regs.pc().inc(2)
        }

        is OpLdABC -> {
            regs.a().set(memory.get8(regs.bc().get()))
            regs.pc().inc()
        }

        is OpLdADE -> {
            regs.a().set(memory.get8(regs.de().get()))
            regs.pc().inc()
        }

        is OpLdAD16 -> {
            regs.a().set(memory.get8(d))
            regs.pc().inc(3)
        }

        is OpLdBCA -> {
            memory.set8(regs.bc().get(), regs.a().get())
            regs.pc().inc()
        }

        is OpLdDEA -> {
            memory.set8(regs.de().get(), regs.a().get())
            regs.pc().inc()
        }

        is OpLdD16A -> {
            memory.set8(d, regs.a().get())
            regs.pc().inc(3)
        }

        is OpLdFromIoPort -> {
            regs.a().set(memory.get8(0xFF00 + d))
            regs.pc().inc(2)
        }

        is OpLdToIoPort -> {
            // TODO
            // memory.set8(0xFF00 + d, regs.a().get())
            regs.pc().inc(2)
        }

        is OpLdFromIoPortC -> {
            regs.a().set(memory.get8(0xFF00 + regs.c().get()))
            regs.pc().inc()
        }

        is OpLdToIoPortC -> {
            memory.set8(0xFF00 + regs.c().get(), regs.a().get())
            regs.pc().inc()
        }

        is OpLdiHLA -> {
            memory.set8(regs.hl().get(), regs.a().get())
            regs.hl().update { (it + 1) % 0x10000 }
            regs.pc().inc()
        }

        is OpLdiAHL -> {
            regs.a().set(memory.get8(regs.hl().get()))
            regs.hl().update { (it + 1) % 0x10000 }
            regs.pc().inc()
        }

        is OpLddHLA -> {
            memory.set8(regs.hl().get(), regs.a().get())
            regs.hl().update { if (it == 0) 0xFFFF else it - 1 }
            regs.pc().inc()
        }

        is OpLddAHL -> {
            regs.a().set(memory.get8(regs.hl().get()))
            regs.hl().update { if (it == 0) 0xFFFF else it - 1 }
            regs.pc().inc()
        }

        is OpLdR16D16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.SP)
            regs.gpr16(r).set(d)
            regs.pc().inc(3)
        }

        is OpLdD16SP -> {
            memory.set16(d, regs.sp().get())
            regs.pc().inc(3)
        }

        is OpLdSPHL -> {
            regs.sp().set(regs.hl().get())
            regs.pc().inc()
        }

        is OpPushR16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.AF)
            regs.sp().update { if (it < 2) it + 0x10000 - 2 else it - 2 }
            memory.set16(regs.sp().get(), regs.gpr16(r).get())
            regs.pc().inc()
        }

        is OpPopR16 -> {
            assert(r == RegEnum16.BC || r == RegEnum16.DE || r == RegEnum16.HL || r == RegEnum16.AF)
            regs.gpr16(r).set(memory.get16(regs.sp().get()))
            regs.sp().update { (it + 2) % 0x10000 }
            regs.pc().inc()
        }

        is OpAddAR8 -> {
            opAddA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpAddAD8 -> {
            opAddA(regs, d)
            regs.pc().inc(2)
        }

        is OpAddAHL -> {
            opAddA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpAdcAR8 -> {
            opAddA(regs, regs.gpr8(r).get(), if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc()
        }

        is OpAdcAD8 -> {
            opAddA(regs, d, if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc(2)
        }

        is OpAdcAHL -> {
            opAddA(regs, memory.get8(regs.hl().get()), if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc()
        }

        is OpSubAR8 -> {
            opSubA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpSubAD8 -> {
            opSubA(regs, d)
            regs.pc().inc(2)
        }

        is OpSubAHL -> {
            opSubA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpSbcAR8 -> {
            opSubA(regs, regs.gpr8(r).get(), if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc()
        }

        is OpSbcAD8 -> {
            opSubA(regs, d, if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc(2)
        }

        is OpSbcAHL -> {
            opSubA(regs, memory.get8(regs.hl().get()), if (regs.flag().isCarryOn()) 1 else 0)
            regs.pc().inc()
        }

        is OpAndAR8 -> {
            opAndA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpAndAD8 -> {
            opAndA(regs, d)
            regs.pc().inc(2)
        }

        is OpAndAHL -> {
            opAndA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpXorAR8 -> {
            opXorA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpXorAD8 -> {
            opXorA(regs, d)
            regs.pc().inc(2)
        }

        is OpXorAHL -> {
            opXorA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpOrAR8 -> {
            opOrA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpOrAD8 -> {
            opOrA(regs, d)
            regs.pc().inc(2)
        }

        is OpOrAHL -> {
            opOrA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpCpAR8 -> {
            opCpA(regs, regs.gpr8(r).get())
            regs.pc().inc()
        }

        is OpCpAD8 -> {
            opCpA(regs, d)
            regs.pc().inc(2)
        }

        is OpCpAHL -> {
            opCpA(regs, memory.get8(regs.hl().get()))
            regs.pc().inc()
        }

        is OpIncR8 -> {
            opInc(regs, { regs.gpr8(r).get() }, { d -> regs.gpr8(r).set(d) })
            regs.pc().inc()
        }

        is OpIncHL -> {
            opInc(regs, { memory.get8(regs.hl().get()) }, { d -> memory.set8(regs.hl().get(), d) })
            regs.pc().inc()
        }

        is OpDecR8 -> {
            opDec(regs, { regs.gpr8(r).get() }, { d -> regs.gpr8(r).set(d) })
            regs.pc().inc()
        }

        is OpDecHL -> {
            opDec(regs, { memory.get8(regs.hl().get()) }, { d -> memory.set8(regs.hl().get(), d) })
            regs.pc().inc()
        }

        is OpDaa -> {
            var corr = 0
                .or(if (regs.flag().isHalfCarryOn()) 0x06 else 0x00)
                .or(if (regs.flag().isCarryOn()) 0x60 else 0x00)

            val aOld = regs.a().get()
            val aNew = if (regs.flag().isSubtractionOn()) {
                aOld - corr
            } else {
                corr = corr
                    .or(if (aOld.and(0x0F) > 0x09) 0x06 else 0x00)
                    .or(if (aOld > 0x99) 0x60 else 0x00)
                aOld + corr
            }

            regs.flag().setSubtraction(false)
            regs.flag().setZero(aNew.and(0xFF) == 0)
            regs.flag().setCarry(corr.and(0x60) != 0)
            regs.a().set(aNew.and(0xFF))
            regs.pc().inc()
        }

        is OpCpl -> {
            regs.a().set(regs.a().get().xor(0xFF).and(0xFF))
            regs.flag().setSubtraction(true)
            regs.flag().setHalfCarry(true)
            regs.pc().inc()
        }

        is OpAddHLR16 -> {
            val hlOld = regs.hl().get()
            val hlNew = hlOld + regs.gpr16(r).get()
            val hlSet = hlNew % 0x10000
            regs.hl().set(hlSet)
            regs.flag().setSubtraction(false)
            regs.flag().setCarry(0x10000 <= hlNew)
            regs.flag().setHalfCarry(0xFFF < hlOld.and(0xFFF) + regs.gpr16(r).get().and(0xFFF))
            regs.pc().inc()
        }

        is OpIncR16 -> {
            val reg16 = regs.gpr16(r)
            reg16.set((reg16.get() + 1) % 0x10000)
            regs.pc().inc()
        }

        is OpDecR16 -> {
            val reg16 = regs.gpr16(r)
            reg16.set(((reg16.get() - 1) + 0x10000) % 0x10000)
            regs.pc().inc()
        }

        is OpAddSpD8 -> {
            val spOld = regs.sp().get()
            val spNew = if (d < 0x80) spOld + d else spOld - (0x100 - d)
            val spSet = (spNew + 0x10000) % 0x10000
            regs.sp().set(spSet)
            regs.flag().setZero(false)
            regs.flag().setSubtraction(false)
            regs.flag().setHalfCarry(0xF < (spOld.and(0xF) + d.and(0xF)))
            regs.flag().setCarry(0xFF < (spOld.and(0xFF) + d.and(0xFF)))
            regs.pc().inc(2)
        }

        is OpLdHLSpAndD8 -> {
            val spVal = regs.sp().get()
            val hlNew = if (d < 0x80) spVal + d else spVal - (0x100 - d)
            val hlSet = (hlNew + 0x10000) % 0x10000
            regs.hl().set(hlSet)
            regs.flag().setZero(false)
            regs.flag().setSubtraction(false)
            regs.flag().setHalfCarry(0xF < (spVal.and(0xF) + d.and(0xF)))
            regs.flag().setCarry(0xFF < (spVal.and(0xFF) + d.and(0xFF)))
            regs.pc().inc(2)
        }

        is OpRlcA -> {
            opRlc(regs, {regs.a().get()}, {regs.a().set(it)}, true)
            regs.pc().inc()
        }

        is OpRlA -> {
            opRl(regs, {regs.a().get()}, {regs.a().set(it)}, true)
            regs.pc().inc()
        }

        is OpRrcA -> {
            opRrc(regs, {regs.a().get()}, {regs.a().set(it)}, true)
            regs.pc().inc()
        }

        is OpRrA -> {
            opRr(regs, {regs.a().get()}, {regs.a().set(it)}, true)
            regs.pc().inc()
        }

        is OpRlcR8 -> {
            opRlc(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)}, false)
            regs.pc().inc(2)
        }

        is OpRlcHL -> {
            val addr = regs.hl().get()
            opRlc(regs, {memory.get8(addr)}, {memory.set8(addr, it)}, false)
            regs.pc().inc(2)
        }

        is OpRlR8 -> {
            opRl(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)}, false)
            regs.pc().inc(2)
        }

        is OpRlHL -> {
            val addr = regs.hl().get()
            opRl(regs, {memory.get8(addr)}, {memory.set8(addr, it)}, false)
            regs.pc().inc(2)
        }

        is OpRrcR8 -> {
            opRrc(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)}, false)
            regs.pc().inc(2)
        }

        is OpRrcHL -> {
            val addr = regs.hl().get()
            opRrc(regs, {memory.get8(addr)}, {memory.set8(addr, it)}, false)
            regs.pc().inc(2)
        }

        is OpRrR8 -> {
            opRr(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)}, false)
            regs.pc().inc(2)
        }

        is OpRrHL -> {
            val addr = regs.hl().get()
            opRr(regs, {memory.get8(addr)}, {memory.set8(addr, it)}, false)
            regs.pc().inc(2)
        }

        is OpSlaR8 -> {
            val reg = regs.gpr8(r)
            val vOld = reg.get()
            val vNew = vOld.shl(1)
            val vSet = vNew % 0x100
            reg.set(vSet)
            regs.flag().setZero(vSet == 0)
            regs.flag().setSubtraction(false)
            regs.flag().setHalfCarry(false)
            regs.flag().setCarry(0xFF < vNew)
            regs.pc().inc(2)
        }

        is OpSlaHL -> {
            val vOld = memory.get8(regs.hl().get())
            val vNew = vOld.shl(1)
            val vSet = vNew % 0x100
            memory.set8(regs.hl().get(), vSet)
            regs.flag().setZero(vSet == 0)
            regs.flag().setSubtraction(false)
            regs.flag().setHalfCarry(false)
            regs.flag().setCarry(0xFF < vNew)
            regs.pc().inc(2)
        }

        is OpSwapR8 -> {
            opSwap(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)})
            regs.pc().inc(2)
        }

        is OpSwapHL -> {
            opSwap(regs, {memory.get8(regs.hl().get())}, {memory.set8(regs.hl().get(), it)})
            regs.pc().inc(2)
        }

        is OpSraR8 -> {
            opSra(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)})
            regs.pc().inc(2)
        }

        is OpSraHL -> {
            opSra(regs, {memory.get8(regs.hl().get())}, {memory.set8(regs.hl().get(), it)})
            regs.pc().inc(2)
        }

        is OpSrlR8 -> {
            opSrl(regs, {regs.gpr8(r).get()}, {regs.gpr8(r).set(it)})
            regs.pc().inc(2)
        }

        is OpSrlHL -> {
            opSrl(regs, {memory.get8(regs.hl().get())}, {memory.set8(regs.hl().get(), it)})
            regs.pc().inc(2)
        }

        is OpBitNR8 -> {
            opBitN(n, regs, {regs.gpr8(r).get()})
            regs.pc().inc(2)
        }

        is OpBitNHL -> {
            opBitN(n, regs, {memory.get8(regs.hl().get())})
            regs.pc().inc(2)
        }

        is OpSetNR8 -> {
            opSetN(n, {regs.gpr8(r).get()},  {regs.gpr8(r).set(it)})
            regs.pc().inc(2)
        }

        is OpSetNHL -> {
            opSetN(n, {memory.get8(regs.hl().get())}, {memory.set8(regs.hl().get(), it)})
            regs.pc().inc(2)
        }

        is OpResNR8 -> {
            opResN(n, {regs.gpr8(r).get()},  {regs.gpr8(r).set(it)})
            regs.pc().inc(2)
        }

        is OpResNHL -> {
            opResN(n, {memory.get8(regs.hl().get())}, {memory.set8(regs.hl().get(), it)})
            regs.pc().inc(2)
        }

        is OpCcf -> {
            regs.flag().setCarry(!regs.flag().isCarryOn())
            regs.flag().setHalfCarry(false)
            regs.flag().setSubtraction(false)
            regs.pc().inc()
        }

        is OpScf -> {
            regs.flag().setCarry(true)
            regs.flag().setHalfCarry(false)
            regs.flag().setSubtraction(false)
            regs.pc().inc()
        }

        is OpNop -> {
            regs.pc().inc()
        }

        is OpHalt -> {
            regs.pc().inc()
            throw UnsupportedOperationException()
        }

        is OpStop -> {
            regs.pc().inc(2)
            throw UnsupportedOperationException()
        }

        is OpDi -> {
            // TODO
            // memory.set8(0xFFFF, 0)
            regs.pc().inc()
        }

        is OpEi -> {
            memory.set8(0xFFFF, 0xFF)
            regs.pc().inc()
        }

        is OpJpN16 -> {
            regs.pc().set(n)
        }

        is OpJpHl -> {
            regs.pc().set(regs.hl().get())
        }

        is OpJpFNn -> {
            runIfConditionSatisfied(regs, f, {
                regs.pc().set(n)
            }) {
                regs.pc().inc(3)
            }
        }

        is OpJrD8 -> {
            regs.pc().set(2 + regs.pc().get() + (d.xor(0x80) - 0x80))
        }

        is OpJrFD8 -> {
            runIfConditionSatisfied(regs, f, {
                regs.pc().set(2 + regs.pc().get() + (d.xor(0x80) - 0x80))
            }) {
                regs.pc().inc(2)
            }
        }

        is OpCallN16 -> {
            opCall(regs, memory, n)
        }

        is OpCallFN16 -> {
            runIfConditionSatisfied(regs, f, { opCall(regs, memory, n) }) {
                regs.pc().inc(3)
            }
        }

        is OpRet -> {
            opRet(regs, memory)
        }

        is OpRetF -> {
            runIfConditionSatisfied(regs, f, { opRet(regs, memory) }) {
                regs.pc().inc()
            }
        }

        is OpRetI -> {
            throw UnsupportedOperationException()
        }

        is OpRstN8 -> {
            opCall(regs, memory, n.v)
        }
    }
}
