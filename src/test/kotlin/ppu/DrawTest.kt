package ppu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DrawTest {
    @Test
    fun testDrawBackgroundTileToScreen() {
        val set = mutableSetOf<Triple<Int, Int, Int2>>()
        val dummyData =
            listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)
        drawBackgroundTileToScreen(object : Memory {
            override fun get(addr: Address): Int8 {
                return when (addr) {
                    in 0x8010..0x801F -> dummyData[addr % 16]
                    else -> error("unexpected")
                }
            }
        }, 5, 5, 1, true) { x, y, colorId ->
            set.add(
                Triple(
                    x,
                    y,
                    colorId
                )
            )
        }
        assertThat(set).hasSize(64)
        listOf(
            // @formatter:off
            0b00, 0b10, 0b11, 0b11, 0b11, 0b11, 0b10, 0b00,
            0b00, 0b11, 0b00, 0b00, 0b00, 0b00, 0b11, 0b00,
            0b00, 0b11, 0b00, 0b00, 0b00, 0b00, 0b11, 0b00,
            0b00, 0b11, 0b00, 0b00, 0b00, 0b00, 0b11, 0b00,
            0b00, 0b11, 0b01, 0b11, 0b11, 0b11, 0b11, 0b00,
            0b00, 0b01, 0b01, 0b01, 0b11, 0b01, 0b11, 0b00,
            0b00, 0b11, 0b01, 0b11, 0b01, 0b11, 0b10, 0b00,
            0b00, 0b10, 0b11, 0b11, 0b11, 0b10, 0b00, 0b00,
            // @formatter:on
        ).forEachIndexed { i, b ->
            assertThat(set).contains(Triple(40 + i % 8, 40 + i / 8, b))
        }
    }
}
