package cpu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class CommandTest {
    private fun makeMemory(vals: List<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
    }

    @Test
    fun `cmd_LD_lo_bit`() {
        val regs = Registers(
            pc = 0,
            a = 0,
            f = 0,
            map = mutableMapOf(Reg16.BC to 0x0000, Reg16.DE to 0x0002))
        CommandLdR8R8(Reg8.C, Reg8.E).run(regs, makeMemory(listOf()))
        assertThat(regs.gpr8(Reg8.C).get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun `cmd_LD_hi_bit`() {
        val regs = Registers(
            pc = 0,
            a = 0,
            f = 0,
            map = mutableMapOf(Reg16.BC to 0x0000, Reg16.DE to 0x0002))
        CommandLdR8R8(Reg8.B, Reg8.E).run(regs, makeMemory(listOf()))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun `cmd_INC`() {
        val regs = Registers(
            pc = 0,
            a = 0,
            f = 0,
            map = mutableMapOf(Reg16.BC to 0x0000))
        CommandIncR16(Reg16.BC).run(regs, makeMemory(listOf()))
        assertThat(regs.gpr16(Reg16.BC).get()).isEqualTo(0x0001)
    }

    @Test
    fun `cmd_INC_carry`() {
        val regs = Registers(
            pc = 0,
            a = 0,
            f = 0,
            map = mutableMapOf(Reg16.BC to 0xFFFF))
        CommandIncR16(Reg16.BC).run(regs, makeMemory(listOf()))
        assertThat(regs.gpr8(Reg8.B).get()).isEqualTo(0x0000)
        assertThat(regs.flag().isCarryOn()).isTrue
    }
}
