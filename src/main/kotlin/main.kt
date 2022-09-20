import ppu.*
import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import java.time.Duration
import java.time.Instant
import javax.swing.JFrame
import javax.swing.WindowConstants
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
    val ZOOM = 5
    val mainWindow = Window(ZOOM * WIDTH, ZOOM * HEIGHT, "gbem")

    // Create Monitor Window
    val monitorWindow = Window(500, 500, "gbem monitor")

    var prev = Instant.now()
    while (true) {
        val curr = Instant.now()

        // render main window
        mainWindow.draw { render(MockMemoryImpl(), it, ZOOM) }

        // render monitor window
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
