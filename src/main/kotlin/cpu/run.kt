package cpu

fun Command.run(regs: Registers, memory: Memory) {
    when (this) {
        is CommandLdR8R8 -> {
            val yVal = regs.gpr8(y).get()
            regs.gpr8(x).set(yVal)
            regs.pc().inc()
        }
        is CommandLdR8D8 -> {
            regs.gpr8(r).set(d)
            regs.pc().inc(2)
        }
        is CommandLdR8HL -> {
            val memVal = memory.get8(regs.gpr16(Reg16.HL).get())
            regs.gpr8(r).set(memVal)
            regs.pc().inc()
        }
        is CommandLdHLR8 -> {
            memory.set8(regs.gpr16(Reg16.HL).get(), regs.gpr8(r).get())
            regs.pc().inc()
        }
        is CommandLdHLD8 -> {
            memory.set8(regs.gpr16(Reg16.HL).get(), d)
            regs.pc().inc(2)
        }
        is CommandLdABC -> {
            regs.gpr8(Reg8.A).set(memory.get8(regs.gpr16(Reg16.BC).get()))
            regs.pc().inc()
        }
        is CommandLdADE -> {
            regs.gpr8(Reg8.A).set(memory.get8(regs.gpr16(Reg16.DE).get()))
            regs.pc().inc()
        }
        is CommandLdAD16 -> {
            regs.gpr8(Reg8.A).set(memory.get8(d))
            regs.pc().inc(3)
        }
        is CommandIncR16 -> {
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
