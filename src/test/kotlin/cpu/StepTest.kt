package cpu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.sign

internal class StepTest {

    private fun makeMemory(vals: List<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
    }

    @Test
    fun `op_LD`() {
        val OPCODE: Int8 = 0b01_001_011

        val regs = Registers(
            pc = 0,
            a = 0,
            f = 0,
            map = mutableMapOf(
                Reg16.BC to 0x0000,
                Reg16.DE to 0x0002))

        step(regs, makeMemory(listOf(OPCODE)))

        assertEquals(0x02, regs.gpr8(Reg8.C).get())
        assertEquals(1, regs.pc().get())
    }
}
