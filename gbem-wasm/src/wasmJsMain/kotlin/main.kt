import emulator.Emulation
import emulator.ppu.DebugParams
import emulator.ppu.LCDColor
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

@JsExport
fun emulate(rom: Uint8Array, draw: (x: Int, y: Int, color: Int) -> Unit) {
    val ba = ByteArray(rom.length)
    for (i in ba.indices) {
        ba[i] = rom[i]
    }

    val emu = object : Emulation(ba) {
        override fun startDrawingScanLine(
            ly: Int,
            ppuDebugParams: DebugParams,
            drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit
        ) {
            drawScanLine { x, y, color -> draw(x, y, color.ordinal) }
        }
    }
    emu.run(100)
}
