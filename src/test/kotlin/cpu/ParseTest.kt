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
    fun parseLdR8D8() {
        val cmd = parse(makeMemory(listOf(0b00_001_110, 0x12)), 0)
        assertThat(cmd).isInstanceOf(CommandLdR8D8::class.java)
        (cmd as CommandLdR8D8).let {
            assertThat(it.r).isEqualTo(Reg8.C)
            assertThat(it.d).isEqualTo(0x12)
        }
    }

    @Test
    fun parseLdR8HL() {
        val cmd = parse(makeMemory(listOf(0b01_001_110)), 0)
        assertThat(cmd).isInstanceOf(CommandLdR8HL::class.java)
        (cmd as CommandLdR8HL).let {
            assertThat(it.r).isEqualTo(Reg8.C)
        }
    }

    @Test
    fun parseLdHLR8() {
        val cmd = parse(makeMemory(listOf(0b01110_001)), 0)
        assertThat(cmd).isInstanceOf(CommandLdHLR8::class.java)
        (cmd as CommandLdHLR8).let {
            assertThat(it.r).isEqualTo(Reg8.C)
        }
    }

    @Test
    fun parseLdHLD8() {
        val cmd = parse(makeMemory(listOf(0b00110110, 0x12)), 0)
        assertThat(cmd).isInstanceOf(CommandLdHLD8::class.java)
        (cmd as CommandLdHLD8).let {
            assertThat(it.d).isEqualTo(0x12)
        }
    }

    @Test
    fun parseLdABC() {
        val cmd = parse(makeMemory(listOf(0b00001010)), 0)
        assertThat(cmd).isInstanceOf(CommandLdABC::class.java)
    }

    @Test
    fun parseLdADE() {
        val cmd = parse(makeMemory(listOf(0b00011010)), 0)
        assertThat(cmd).isInstanceOf(CommandLdADE::class.java)
    }

    @Test
    fun parseLdAD16() {
        val cmd = parse(makeMemory(listOf(0b11111010, 0x12, 0x34)), 0)
        assertThat(cmd).isInstanceOf(CommandLdAD16::class.java)
        (cmd as CommandLdAD16).let {
            assertThat(it.d).isEqualTo(0x3412)
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
