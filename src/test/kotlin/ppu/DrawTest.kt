package ppu

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ppu.LCDColor.*

internal class DrawTest {
    @Test
    fun testDrawBackgroundTileToScreen() {
        val set = mutableSetOf<Triple<Int, Int, LCDColor>>()
        val dummyTileData =
            listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)
        drawBackgroundTileToScreen(object : Memory {
            override fun get(addr: Address): Int8 {
                return when (addr) {
                    in 0x8010..0x801F -> dummyTileData[addr % 16]
                    ADDR_BGP -> 0b00_01_10_11
                    else -> error("unexpected")
                }
            }
        }, 5, 5, 1, true) { x, y, color ->
            set.add(
                Triple(
                    x,
                    y,
                    color
                )
            )
        }
        assertThat(set).hasSize(64)
        listOf(
            // @formatter:off
            Black, LightGray, White, White, White, White, LightGray, Black,
            Black, White, Black, Black, Black, Black, White, Black,
            Black, White, Black, Black, Black, Black, White, Black,
            Black, White, Black, Black, Black, Black, White, Black,
            Black, White, DarkGray, White, White, White, White, Black,
            Black, DarkGray, DarkGray, DarkGray, White, DarkGray, White, Black,
            Black, White, DarkGray, White, DarkGray, White, LightGray, Black,
            Black, LightGray, White, White, White, LightGray, Black, Black,
            // @formatter:on
        ).forEachIndexed { i, color ->
            assertThat(set).contains(Triple(40 + i % 8, 40 + i / 8, color))
        }
    }

    @Test
    fun testDrawScanlineInViewport() {
        val set = mutableSetOf<Triple<Int, Int, LCDColor>>()
        val dummyTileData =
            listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)
        drawScanlineInViewport(object : Memory {
            override fun get(addr: Address): Int8 {
                return when (addr) {
                    in 0x8000..0x800F -> dummyTileData[addr % 16]
                    in 0x9800 until (0x9800 + 20) -> 0 // Fake Tile Map
                    ADDR_LCDC -> 0b00010001
                    ADDR_BGP -> 0b11_10_01_00
                    ADDR_SCX -> 0
                    ADDR_SCY -> 0
                    else -> error("unexpected")
                }
            }
        }, 0) { x, y, color ->
            set.add(
                Triple(
                    x,
                    y,
                    color
                )
            )
        }
        assertThat(set).hasSize(160)
        List(160/8) {_ -> listOf(White, DarkGray, Black, Black, Black, Black, DarkGray, White)}.flatten().forEachIndexed { i, color ->
            assertThat(set).contains(Triple(i, 0, color))
        }
    }
}
