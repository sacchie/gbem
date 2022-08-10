package cpu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandTest {
    private fun makeMemory(vals: MutableList<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
        override fun set8(addr: Int16, int8: Int8) { vals[addr] = int8 }
    }

    @Test
    fun commandLdR8R8_lo_bit() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x0000, Reg16.DE to 0x0002))
        CommandLdR8R8(Reg8.C, Reg8.E).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.gpr8(Reg8.C).get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdR8R8_hi_bit() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x0000, Reg16.DE to 0x0002))
        CommandLdR8R8(Reg8.B, Reg8.E).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdR8D8() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x0000))
        CommandLdR8D8(Reg8.B, 0x10).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x10)
        assertThat(regs.pc().get()).isEqualTo(2)
    }

    @Test
    fun commandLdR8HL() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x0000, Reg16.HL to 0x0000))
        CommandLdR8HL(Reg8.B).run(regs, makeMemory(mutableListOf(0x12)))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdHLR8() {
        val memory = makeMemory(mutableListOf(0x00))
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x1234, Reg16.HL to 0x0000))
        CommandLdHLR8(Reg8.B).run(regs, memory)
        assertThat(memory.get8(0)).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdHLD8() {
        val memory = makeMemory(mutableListOf(0x00))
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.HL to 0x0000))
        CommandLdHLD8(0x12).run(regs, memory)
        assertThat(memory.get8(0)).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(2)
    }

    @Test
    fun commandInc() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0x0000))
        CommandIncR16(Reg16.BC).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.gpr16(Reg16.BC).get()).isEqualTo(0x0001)
    }

    @Test
    fun commandInc_carry() {
        val regs = Registers(
            pc = 0, a = 0, f = 0, map = mutableMapOf(Reg16.BC to 0xFFFF))
        CommandIncR16(Reg16.BC).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x0000)
        assertThat(regs.flag().isCarryOn()).isTrue
    }
}
