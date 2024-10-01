import emulator.Emulation
import emulator.ppu.DebugParams
import emulator.ppu.LCDColor
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

val EMULATIONS: MutableMap<Int, Emulation> = mutableMapOf()

@JsExport
fun makeEmulator(rom: Uint8Array, draw: (x: Int, y: Int, color: Int) -> Unit): Int {
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
fun emulatorDrawnFrameCount(id: Int): Int {
    return EMULATIONS[id]!!.drawnFrameCount
}

@JsExport
fun emulatorOnJoypadInput(id: Int, key: String, pressed: Boolean) {
    val joypadHandlers = EMULATIONS[id]!!.getJoypadHandlers()
    val handler = when (key) {
        "Up" -> joypadHandlers::onUp
        "Down" -> joypadHandlers::onDown
        "Left" -> joypadHandlers::onLeft
        "Right" -> joypadHandlers::onRight
        "A" -> joypadHandlers::onA
        "B" -> joypadHandlers::onB
        "Start" -> joypadHandlers::onStart
        "Select" -> joypadHandlers::onSelect
        else -> throw RuntimeException()
    }
    handler(pressed)
}