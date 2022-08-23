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
        assertThat(cmd).isInstanceOf(OpLdR8R8::class.java)
        (cmd as OpLdR8R8).let {
            assertThat(it.x).isEqualTo(RegEnum8.C)
            assertThat(it.y).isEqualTo(RegEnum8.E)
        }
    }

    @Test
    fun parseLdR8D8() {
        val cmd = parse(makeMemory(listOf(0b00_001_110, 0x12)), 0)
        assertThat(cmd).isInstanceOf(OpLdR8D8::class.java)
        (cmd as OpLdR8D8).let {
            assertThat(it.r).isEqualTo(RegEnum8.C)
            assertThat(it.d).isEqualTo(0x12)
        }
    }

    @Test
    fun parseLdR8HL() {
        val cmd = parse(makeMemory(listOf(0b01_001_110)), 0)
        assertThat(cmd).isInstanceOf(OpLdR8HL::class.java)
        (cmd as OpLdR8HL).let {
            assertThat(it.r).isEqualTo(RegEnum8.C)
        }
    }

    @Test
    fun parseLdHLR8() {
        val cmd = parse(makeMemory(listOf(0b01110_001)), 0)
        assertThat(cmd).isInstanceOf(OpLdHLR8::class.java)
        (cmd as OpLdHLR8).let {
            assertThat(it.r).isEqualTo(RegEnum8.C)
        }
    }

    @Test
    fun parseLdHLD8() {
        val cmd = parse(makeMemory(listOf(0b00110110, 0x12)), 0)
        assertThat(cmd).isInstanceOf(OpLdHLD8::class.java)
        (cmd as OpLdHLD8).let {
            assertThat(it.d).isEqualTo(0x12)
        }
    }

    @Test
    fun parseLdABC() {
        val cmd = parse(makeMemory(listOf(0b00001010)), 0)
        assertThat(cmd).isInstanceOf(OpLdABC::class.java)
    }

    @Test
    fun parseLdADE() {
        val cmd = parse(makeMemory(listOf(0b00011010)), 0)
        assertThat(cmd).isInstanceOf(OpLdADE::class.java)
    }

    @Test
    fun parseLdAD16() {
        val cmd = parse(makeMemory(listOf(0b11111010, 0x12, 0x34)), 0)
        assertThat(cmd).isInstanceOf(OpLdAD16::class.java)
        (cmd as OpLdAD16).let {
            assertThat(it.d).isEqualTo(0x3412)
        }
    }

    @Test
    fun parseIncR16() {
        val cmd = parse(makeMemory(listOf(0b00_01_0011)), 0)
        assertThat(cmd).isInstanceOf(OpIncR16::class.java)
        (cmd as OpIncR16).let {
            assertThat(it.r).isEqualTo(RegEnum16.DE)
        }
    }
}
