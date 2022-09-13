import ppu.HEIGHT
import ppu.MockMemoryImpl
import ppu.WIDTH
import ppu.render
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

// TODO refactor window class
fun main(args: Array<String>) {
//    loop(10)

    // Create Main Window
    val ZOOM = 5
    val windowWidth = ZOOM * WIDTH
    val windowHeight = ZOOM * HEIGHT

    val mainFrame = JFrame("gbem")
    mainFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    mainFrame.isResizable = false
    val mainCanvas = Canvas()
    mainCanvas.setPreferredSize(Dimension(windowWidth, windowHeight))

    mainFrame.contentPane.add(mainCanvas)
    mainFrame.pack()
    mainFrame.setLocationRelativeTo(null)
    mainFrame.isVisible = true

    val gMain = mainCanvas.getGraphics()
    val mainBuffer = mainCanvas.createImage(windowWidth, windowHeight)
    val mainGraphicsBuffer = mainBuffer.getGraphics()

    // Create Monitor Window
    val monitorWidth = 500
    val monitorHeight = 500
    val monitorFrame = JFrame("gbem monitor")
    monitorFrame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    monitorFrame.isResizable = false
    val monitorCanvas = Canvas()
    monitorCanvas.setPreferredSize(Dimension(monitorWidth, monitorHeight))

    monitorFrame.contentPane.add(monitorCanvas)
    monitorFrame.pack()
    monitorFrame.setLocationRelativeTo(null)
    monitorFrame.isVisible = true

    val gMonitor = monitorCanvas.getGraphics()
    val monitorBuffer = monitorCanvas.createImage(monitorWidth, monitorHeight)
    val monitorGraphicsBuffer = monitorBuffer.getGraphics()

    var prev = Instant.now()
    while (true) {
        val curr = Instant.now()
        // render main window
        mainGraphicsBuffer.clearRect(0, 0, windowWidth, windowHeight)
        render(MockMemoryImpl(), mainGraphicsBuffer, ZOOM)
        gMain.drawImage(mainBuffer, 0, 0, mainCanvas)

        // render monitor window
        val delta = Duration.between(prev, curr)
        val fps = (1000.0 / delta.toMillis()).roundToInt()
        monitorGraphicsBuffer.clearRect(0, 0, windowWidth, windowHeight)
        monitorGraphicsBuffer.color = Color(0,0,0)
        monitorGraphicsBuffer.drawString("${fps} fps", 0,50)
        gMonitor.drawImage(monitorBuffer, 0, 0, monitorCanvas)

        prev = curr
    }
    println("Emulation finished")
}
