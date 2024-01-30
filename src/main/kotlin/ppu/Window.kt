package ppu

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JFrame
import javax.swing.WindowConstants

// Ref. https://gbdev.io/pandocs/Tile_Data.html
val COLOR = mapOf(
    LCDColor.Black to Color(0x08, 0x18, 0x20),
    LCDColor.DarkGray to Color(0x34, 0x68, 0x56),
    LCDColor.LightGray to Color(0x88, 0xc0, 0x70),
    LCDColor.White to Color(0xe0, 0xf8, 0xd0),
)

class Window(private val width: Int, private  val height:Int, title: String, onKey: (pressed: Boolean) -> Unit) {
    private val canvas: Canvas
    private val graphics: Graphics
    private val image: Image
    private val imageBuffer: Graphics

    init {
        val frame = JFrame(title)
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.isResizable = true
        canvas = Canvas()
        canvas.setPreferredSize(Dimension(width, height))

        canvas.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                onKey(true)
            }

            override fun keyReleased(e: KeyEvent?) {
                onKey(false)
            }
        })

        frame.contentPane.add(canvas)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        graphics = canvas.getGraphics()
        image = canvas.createImage(width, height)
        imageBuffer = image.getGraphics()
    }

    fun draw(f: (buf: Graphics)-> Unit) {
        // imageBuffer.clearRect(0, 0, width, height)
        f(imageBuffer)
        graphics.drawImage(image, 0, 0, canvas)
    }
}
