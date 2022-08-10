package cpu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StepTest {

    private fun makeMemory(vals: List<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr.toInt()]
    }

    @Test
    fun `op_LD`() {
        val OPCODE: Int8 = 0b01_001_011

        val regValues = RegisterValues(
            pc = 0,
            reg8 = mutableMapOf(Reg8.E to 0x02)
        )

        step(makeMemory(listOf(OPCODE)), Registers(regValues))

        assertEquals(0x02, regValues.reg8[Reg8.C]!!)
        assertEquals(1, regValues.pc)
    }
}
