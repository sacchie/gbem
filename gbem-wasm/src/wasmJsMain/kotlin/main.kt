import emulator.Emulation
import emulator.ppu.DebugParams
import emulator.ppu.LCDColor
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

val EMULATIONS: MutableMap<Int, Emulation> = mutableMapOf()

@JsExport
fun newEmulator(rom: Uint8Array, draw: (x: Int, y: Int, color: Int) -> Unit): Int {
    val ba = ByteArray(rom.length)
    for (i in ba.indices) {
        ba[i] = rom[i]
    }

    val id = EMULATIONS.size
    EMULATIONS[id] = object : Emulation(ba) {
        override fun startDrawingScanLine(
            ly: Int,
            ppuDebugParams: DebugParams,
            drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit
        ) {
            drawScanLine { x, y, color -> draw(x, y, color.ordinal) }
        }
    }
    return id
}

@JsExport
fun emulatorStep(id: Int) {
    EMULATIONS[id]?.run(10000)
}

@JsExport
fun getDrawnFrameCount(id: Int): Int {
    return EMULATIONS[id]!!.drawnFrameCount
}
