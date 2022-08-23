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
    RegEnum16.BC -> bc()
    RegEnum16.DE -> de()
    RegEnum16.HL -> hl()
    RegEnum16.SP -> sp()
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
        is OpIncR16 -> {
            val reg16 = regs.gpr16(r)
            val hi = reg16.get().hi()
            val lo = reg16.get().lo()

            if (lo != 0xFF) {
                reg16.set(int16FromHiAndLo(hi, lo + 1))
            } else if (hi != 0xFF) {
                reg16.set(int16FromHiAndLo(hi + 1, 0))
            } else {
                reg16.set(int16FromHiAndLo(0, 0))
                regs.flag().setCarry(true)
            }
            regs.pc().inc()
        }
    }
}
