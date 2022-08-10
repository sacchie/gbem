package cpu

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ParseTest {

    private fun makeMemory(vals: List<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
    }

    @Test
    fun `op_LD`() {
        val cmd = parse(makeMemory(listOf(0b01_001_011)), 0)
        assert(cmd is CommandLdRR)
        assertEquals(Reg8.C, (cmd as CommandLdRR).x)
        assertEquals(Reg8.E, (cmd as CommandLdRR).y)
    }

    @Test
    fun `op_INC`() {
        val cmd = parse(makeMemory(listOf(0b00_01_0011)), 0)
        assert(cmd is CommandInc)
        assertEquals(Reg16.DE, (cmd as CommandInc).r)
    }
}
