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

    // Create Main Window
    val width = 256
    val height = 256
    val zoom = 5
    // Ref. https://gbdev.io/pandocs/Tile_Data.html
    val COLOR =
        listOf(Color(0x08, 0x18, 0x20), Color(0x34, 0x68, 0x56), Color(0x88, 0xc0, 0x70), Color(0xe0, 0xf8, 0xd0))

    val mainWindow = Window(zoom * width, zoom * height, "gbem")

    val memory = MockMemoryImpl()

    // Create Monitor Window
    val monitorWindow = Window(500, 500, "gbem monitor")

    var prev = Instant.now()
    while (true) {
        val curr = Instant.now()

        // draw main window
        mainWindow.draw { buf ->
            drawScreen(memory) { x, y, colorId ->
                buf.color = COLOR[colorId]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
        }

        // draw monitor window
        monitorWindow.draw {
            val delta = Duration.between(prev, curr)
            val fps = (1000.0 / delta.toMillis()).roundToInt()
            it.color = Color(0, 0, 0)
            it.drawString("${fps} fps", 0, 50)
        }
        prev = curr
    }
    println("Emulation finished")
}
