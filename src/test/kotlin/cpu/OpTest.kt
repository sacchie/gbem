package cpu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OpTest {
    private fun makeMemory(vals: MutableList<Int8>) = object : Memory {
        override fun get8(addr: Int16): Int8 = vals[addr]
        override fun get16(addr: Int16): Int16 {
            TODO("Not yet implemented")
        }

        override fun set8(addr: Int16, int8: Int8) { vals[addr] = int8 }
        override fun set16(addr: Int16, int16: Int16) {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun commandLdR8R8_lo_bit() {
        val regs = Registers(de = 0x0002)
        OpLdR8R8(RegEnum8.C, RegEnum8.E).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.c().get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdR8R8_hi_bit() {
        val regs = Registers(de = 0x0002)
        OpLdR8R8(RegEnum8.B, RegEnum8.E).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.b().get()).isEqualTo(0x02)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdR8D8() {
        val regs = Registers()
        OpLdR8D8(RegEnum8.B, 0x10).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.b().get()).isEqualTo(0x10)
        assertThat(regs.pc().get()).isEqualTo(2)
    }

    @Test
    fun commandLdR8HL() {
        val regs = Registers()
        OpLdR8HL(RegEnum8.B).run(regs, makeMemory(mutableListOf(0x12)))
        assertThat(regs.b().get()).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdHLR8() {
        val memory = makeMemory(mutableListOf(0x00))
        val regs = Registers(bc = 0x1234)
        OpLdHLR8(RegEnum8.B).run(regs, memory)
        assertThat(memory.get8(0)).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdHLD8() {
        val memory = makeMemory(mutableListOf(0x00))
        val regs = Registers()
        OpLdHLD8(0x12).run(regs, memory)
        assertThat(memory.get8(0)).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(2)
    }

    @Test
    fun commandLdABC() {
        val memory = makeMemory(mutableListOf(0x12))
        val regs = Registers()
        OpLdABC().run(regs, memory)
        assertThat(regs.a().get()).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdADE() {
        val memory = makeMemory(mutableListOf(0x12))
        val regs = Registers()
        OpLdADE().run(regs, memory)
        assertThat(regs.a().get()).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(1)
    }

    @Test
    fun commandLdAD16() {
        val memory = makeMemory(mutableListOf(0x12))
        val regs = Registers()
        OpLdAD16(0x0000).run(regs, memory)
        assertThat(regs.a().get()).isEqualTo(0x12)
        assertThat(regs.pc().get()).isEqualTo(3)
    }

    @Test
    fun commandInc() {
        val regs = Registers()
        OpIncR16(RegEnum16.BC).run(regs, makeMemory(mutableListOf()))
        assertThat(regs.bc().get()).isEqualTo(0x0001)
    }
}
