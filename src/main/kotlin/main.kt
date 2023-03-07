import ppu.*
import java.awt.Color
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
    val COLOR =
        listOf(Color(0x08, 0x18, 0x20), Color(0x34, 0x68, 0x56), Color(0x88, 0xc0, 0x70), Color(0xe0, 0xf8, 0xd0))

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
            drawViewport(memory) { x, y, colorId ->
                buf.color = COLOR[colorId]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
        }

        // draw background debug window
        backgroundDebugWindow.draw { buf ->
            drawBackgroundToScreen(memory) { x, y, colorId ->
                buf.color = COLOR[colorId]
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

private val DUMMY_DATA =
    listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)

class MockMemoryImpl : Memory {
    var counter = 0
    override fun get(addr: Address): Int8 {
        return when (addr) {
            ADDR_SCX -> (counter++ / 200) % 256
            ADDR_SCY -> (counter++ / 400) % 256
            ADDR_LCDC -> 0b10100011
            else -> DUMMY_DATA[addr % 16]
        }
    }
}
