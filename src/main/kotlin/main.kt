import emulator.Emulation
import emulator.ppu.DebugParams
import emulator.ppu.LCDColor
import emulator.ppu.OamData
import java.awt.Color


fun main(args: Array<String>) {
    val zoom = 3
    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")

    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()

    val emu = object : Emulation(romByteArray) {
        override fun startDrawingScanLine(ly: Int, ppuDebugParams: DebugParams, drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit) {
            mainWindow.draw { buf ->
                buf.color = Color.BLUE
                buf.fillRect(0, zoom * ly, zoom * 160, zoom * 1)
                drawScanLine { x, y, color ->
                    buf.color = COLOR[color]
                    buf.fillRect(x * zoom, y * zoom, zoom, zoom)
                }
            }
            // FIXME: WSLでこれをするとIntelliJごとフリーズする
            // mainWindow.updateTitle(ppuDebugParams.toString())
        }
    }

    mainWindow.bindJoypadHandlers(emu.getJoypadHandlers(), emu.getDebugHandlers())

    emu.run()

    println("Emulation finished")
}
