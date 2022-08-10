package cpu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ParseTest {

    private fun makeMemory(vals: List<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
        override fun set8(addr: Int16, int8: Int8) { throw UnsupportedOperationException() }
    }

    @Test
    fun parseLdR8R8() {
        val cmd = parse(makeMemory(listOf(0b01_001_011)), 0)
        assertThat(cmd).isInstanceOf(CommandLdR8R8::class.java)
        (cmd as CommandLdR8R8).let {
            assertThat(it.x).isEqualTo(Reg8.C)
            assertThat(it.y).isEqualTo(Reg8.E)
        }
    }

    @Test
    fun parseIncR16() {
        val cmd = parse(makeMemory(listOf(0b00_01_0011)), 0)
        assertThat(cmd).isInstanceOf(CommandIncR16::class.java)
        (cmd as CommandIncR16).let {
            assertThat(it.r).isEqualTo(Reg16.DE)
        }
    }
}
