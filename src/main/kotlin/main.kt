import emulator.Emulation
import emulator.ppu.DebugParams
import emulator.ppu.LCDColor


fun main(args: Array<String>) {
    val zoom = 3
    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")

    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()

    val emu = object : Emulation(romByteArray) {
        override fun startDrawingScanLine(ly: Int, ppuDebugParams: DebugParams, drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit) {
            mainWindow.draw { buf ->
                buf.clearRect(0, zoom * ly, zoom * 160, zoom * 1)
                drawScanLine { x, y, color ->
                    buf.color = COLOR[color]
                    buf.fillRect(x * zoom, y * zoom, zoom, zoom)
                }
            }
            mainWindow.updateTitle(ppuDebugParams.toString())
        }
    }

    mainWindow.bindJoypadHandlers(emu.getJoypadHandlers(), emu::toggleDrawBackground, emu::toggleDrawWindow, emu::toggleDrawSprites)

    emu.run()

    println("Emulation finished")
}
