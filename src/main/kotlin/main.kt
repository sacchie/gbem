import emulator.Emulation
import ppu.*


fun main(args: Array<String>) {
    val zoom = 3
    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")

    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()

    val emu = object : Emulation(romByteArray) {
        override fun startDrawingScanLine(drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit) {
            mainWindow.draw { buf ->
                drawScanLine { x, y, color ->
                    buf.color = COLOR[color]
                    buf.fillRect(x * zoom, y * zoom, zoom, zoom)
                }
            }
        }
    }

    mainWindow.bindHandlers(emu.getHandlers())

    emu.run()

    println("Emulation finished")
}
