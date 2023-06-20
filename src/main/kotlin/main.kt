import ppu.*
import java.awt.Color
import java.lang.IllegalArgumentException
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

fun loop(maxIterations: Int) {
    //  state = memory & registers

    repeat(maxIterations) {
        //  CPUがstateを更新

        //  PPUがstate.memory.VRAM領域を見て画面を更新
        //  APUがstate.memory.AUDIO領域を見て音を出す
        //  割り込みがあったらコールバックを実行
    }
}

fun main(args: Array<String>) {
//    loop(10)

    // Create Background Debug Window
    val width = 256
    val height = 256
    val zoom = 5
    // Ref. https://gbdev.io/pandocs/Tile_Data.html
    val COLOR = mapOf(
        LCDColor.Black to Color(0x08, 0x18, 0x20),
        LCDColor.DarkGray to Color(0x34, 0x68, 0x56),
        LCDColor.LightGray to Color(0x88, 0xc0, 0x70),
        LCDColor.White to Color(0xe0, 0xf8, 0xd0),
    )

    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")
    val backgroundDebugWindow = Window(zoom * width, zoom * height, "gbem background debug")

    val memory = MockMemoryImpl()

    // Create Monitor Window
    val monitorWindow = Window(500, 500, "gbem monitor")

    var prev = Instant.now()
    while (true) {
        val curr = Instant.now()

        // draw main window
        mainWindow.draw { buf ->
            drawViewport(memory) { x, y, color ->
                buf.color = COLOR[color]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
        }

        // draw background debug window
        backgroundDebugWindow.draw { buf ->
            drawBackgroundToScreen(memory) { x, y, color ->
                buf.color = COLOR[color]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
            val left = memory.get(ADDR_SCX)
            val top = memory.get(ADDR_SCY)
            val right = (left + VIEWPORT_W) % SCREEN_W
            val bottom = (top + VIEWPORT_H) % SCREEN_H
            buf.color = Color.RED
            // lines from left to right
            if (left < right) {
                buf.fillRect(left * zoom, top * zoom, (right - left) * zoom, zoom)
                buf.fillRect(left * zoom, bottom * zoom, (right - left) * zoom, zoom)
            } else {
                buf.fillRect(0, top * zoom, right * zoom, zoom)
                buf.fillRect(left * zoom, top * zoom, (SCREEN_W - left) * zoom, zoom)
                buf.fillRect(0, bottom * zoom, right * zoom, zoom)
                buf.fillRect(left * zoom, bottom * zoom, (SCREEN_W - left) * zoom, zoom)
            }
            // lines from top to bottom
            if (top < bottom) {
                buf.fillRect(left * zoom, top * zoom, zoom, (bottom - top) * zoom)
                buf.fillRect(right * zoom, top * zoom, zoom, (bottom - top) * zoom)
            } else {
                buf.fillRect(left * zoom, 0, zoom, bottom * zoom)
                buf.fillRect(left * zoom, top * zoom, zoom, (SCREEN_H - top) * zoom)
                buf.fillRect(right * zoom, 0, zoom, bottom * zoom)
                buf.fillRect(right * zoom, top * zoom, zoom, (SCREEN_H - top) * zoom)
            }
        }

        // draw monitor window
        monitorWindow.draw {
            val delta = Duration.between(prev, curr)
            val fps = (1000.0 / delta.toMillis()).roundToInt()
            it.color = Color(0, 0, 0)
            it.drawString("${fps} fps", 0, 50)
            it.drawString("SCX=${memory.get(ADDR_SCX)}", 0, 100)
            it.drawString("SCY=${memory.get(ADDR_SCY)}", 0, 150)
        }
        prev = curr
    }
    println("Emulation finished")
}

private val DUMMY_TILE_DATA =
    listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)

class MockMemoryImpl : Memory {
    var counter = 0
    override fun get(addr: Address): Int8 {
        return when (addr) {
            ADDR_SCX -> (counter++ / 200) % 256
            ADDR_SCY -> (counter++ / 400) % 256
            ADDR_WX -> 10
            ADDR_WY -> 100
            // 7	LCD and PPU enable	0=Off, 1=On
            // 6	Window tile map area	0=9800-9BFF, 1=9C00-9FFF
            // 5	Window enable	0=Off, 1=On
            // 4	BG and Window tile data area	0=8800-97FF, 1=8000-8FFF
            // 3	BG tile map area	0=9800-9BFF, 1=9C00-9FFF
            // 2	OBJ size	0=8x8, 1=8x16
            // 1	OBJ enable	0=Off, 1=On
            // 0	BG and Window enable/priority	0=Off, 1=On
            ADDR_LCDC -> 0b10100011
            ADDR_BGP -> 0b00_01_10_11
            ADDR_OBP0 -> 0b00_01_10_11
            ADDR_OBP1 -> 0b00_01_10_11
            in 0x8000..0x8FFF -> DUMMY_TILE_DATA[addr % 16] // BG and Window tile data area (LCDC4=1)
            in 0x8800..0x97FF -> DUMMY_TILE_DATA[addr % 16] // BG and Window tile data area (LCDC4=0)
            in 0x9800..0x9BFF -> 0 // BG/Window tile map data (LCDC3/6=0)
            in 0x9C00..0x9FFF -> 0 // BG/Window tile map data (LCDC3/6=1)
            in 0xFE00..0xFE9F -> dummyOAM(addr) // OAM (4byte * 40)
            else -> throw IllegalArgumentException("Address = $addr")
        }
    }

    private fun dummyOAM(addr: Address): Int8 {
        val i = (addr - 0xFE00) / 4
        val x = i * 8 * 2
        val y = i * 8
        // Bit7   BG and Window over OBJ (0=No, 1=BG and Window colors 1-3 over the OBJ)
        // Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
        // Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
        // Bit4   Palette number  **Non CGB Mode Only** (0=OBP0, 1=OBP1)
        // Bit3   Tile VRAM-Bank  **CGB Mode Only**     (0=Bank 0, 1=Bank 1)
        // Bit2-0 Palette number  **CGB Mode Only**     (OBP0-7)
        return listOf(y, x, 0, 0b00000000 + 0b10000000 * (i % 2))[addr % 4]
    }
}
