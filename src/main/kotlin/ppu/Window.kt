package ppu

import java.awt.Canvas
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import javax.swing.JFrame
import javax.swing.WindowConstants

class Window(private val width: Int, private  val height:Int, title: String) {
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

        frame.contentPane.add(canvas)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        graphics = canvas.getGraphics()
        image = canvas.createImage(width, height)
        imageBuffer = image.getGraphics()
    }

    fun draw(f: (buf: Graphics)-> Unit) {
        imageBuffer.clearRect(0, 0, width, height)
        f(imageBuffer)
        graphics.drawImage(image, 0, 0, canvas)
    }
}
