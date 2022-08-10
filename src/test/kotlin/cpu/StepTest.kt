package cpu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class StepTest {
    @Test
    fun `op_LD`() {
        val OPCODE: Int8 = 0b01_001_011

        val values = RegisterValues(
            pc = 0,
            reg8 = mutableMapOf(Reg8.E to 0x02)
        )
        val memory = object : Memory {
            override fun get8(addr: Int16): Int8 {
                if (addr == 0.toShort()) {
                    return OPCODE
                }
                throw RuntimeException()
            }
        }

        step(memory, Registers(values))

        assertEquals(0x02, values.reg8[Reg8.C]!!, )
        assertEquals(1, values.pc)
    }
}
