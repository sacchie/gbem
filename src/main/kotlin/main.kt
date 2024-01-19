import cpu.*
import ppu.COLOR
import ppu.Window
import ppu.drawViewport

const val ADDR_TIMA = 0xFF05
const val ADDR_TMA = 0xFF06
const val ADDR_TAC = 0xFF07
const val ADDR_IF = 0xFF0F


fun loop(maxIterations: Int, state: State, drawMainWindow: () -> Unit) {
    state.register.pc = 0x100

    val registers = state.registers()
    val memory = state.memory()
    val timer = state.timer()

    repeat(maxIterations) {
//        if (pc == 0x38) {
//            throw RuntimeException("0x0038 reached")
//        }
        /* 01-special.sを通した時のチェックポイント
        if (pc == 0xc7d2) {
            // 01-specialのforever:突入を検知するためだけのコード
            // そのうち消す必要がある
            throw RuntimeException("0xc7d2 reached")
        }
        if (pc == 0xc1f9) {
            // check_crc_に到達した
            listOf(0xFF80, 0xFF81, 0xFF82, 0xFF83).forEach{
                System.err.println("${it.toString(16)}: ${memory.get8(it).toString(16)}")
            }
            throw RuntimeException("check_crc_ reached")
        }
        */

        handleInterrupts(memory, registers, state.state())

        if (!state.halted) {
            val pc = registers.getPc()
            val op = cpu.op.parse(memory, pc)
            System.err.println(
                "${(if (state.register.callDepthForDebug >= 0) "*" else "-").repeat(Math.abs(state.register.callDepthForDebug))} 0x${
                    pc.toString(
                        16
                    )
                }: $op"
            )
            //  CPUがstateを更新
            op.run(registers, memory, state.state())
        }

        // Timer
        val CYCLE_COUNT = 8 // FIXME とりあえず平均値近くで設定しているだけ（2-byte instructionを想定）
        if (timer.tick(CYCLE_COUNT)) {
            memory.set8(ADDR_IF, memory.get8(ADDR_IF) or 0b100)
        }

        //  PPUがstate.memory.VRAM領域を見て画面を更新
        if (it % 10000 == 0) {
            drawMainWindow()
        }

        //  APUがstate.memory.AUDIO領域を見て音を出す
        //  割り込みがあったらコールバックを実行
    }
}


fun main(args: Array<String>) {
    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()
    val state = State(romByteArray)

    val memory = state.memory()
    val cartridgeType = memory.getCartridgeType()
    val romSize = memory.getRomSize()
    val ramSize = memory.getRamSize()

    System.err.println(
        "cartridgeType: 0x${cartridgeType.toString(16)}, romSize: 0x${romSize.toString(16)}, ramSize: 0x${
            ramSize.toString(
                16
            )
        })"
    )

    // Create Background Debug Window
    val width = 256
    val height = 256
    val zoom = 3

    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")
//    val backgroundDebugWindow = Window(zoom * width, zoom * height, "gbem background debug")

    // Create Monitor Window
//    val monitorWindow = Window(500, 500, "gbem monitor")

    loop(Int.MAX_VALUE, state) {
        mainWindow.draw { buf ->
            drawViewport(memory) { x, y, color ->
                buf.color = COLOR[color]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
        }
    }

//    var prev = Instant.now()
//    while (true) {
//        val curr = Instant.now()

//
//    // draw background debug window
//    backgroundDebugWindow.draw { buf ->
//        drawBackgroundToScreen(memory) { x, y, color ->
//            buf.color = COLOR[color]
//            buf.fillRect(x * zoom, y * zoom, zoom, zoom)
//        }
//        val left = memory.get(ADDR_SCX)
//        val top = memory.get(ADDR_SCY)
//        val right = (left + VIEWPORT_W) % SCREEN_W
//        val bottom = (top + VIEWPORT_H) % SCREEN_H
//        buf.color = Color.RED
//        // lines from left to right
//        if (left < right) {
//            buf.fillRect(left * zoom, top * zoom, (right - left) * zoom, zoom)
//            buf.fillRect(left * zoom, bottom * zoom, (right - left) * zoom, zoom)
//        } else {
//            buf.fillRect(0, top * zoom, right * zoom, zoom)
//            buf.fillRect(left * zoom, top * zoom, (SCREEN_W - left) * zoom, zoom)
//            buf.fillRect(0, bottom * zoom, right * zoom, zoom)
//            buf.fillRect(left * zoom, bottom * zoom, (SCREEN_W - left) * zoom, zoom)
//        }
//        // lines from top to bottom
//        if (top < bottom) {
//            buf.fillRect(left * zoom, top * zoom, zoom, (bottom - top) * zoom)
//            buf.fillRect(right * zoom, top * zoom, zoom, (bottom - top) * zoom)
//        } else {
//            buf.fillRect(left * zoom, 0, zoom, bottom * zoom)
//            buf.fillRect(left * zoom, top * zoom, zoom, (SCREEN_H - top) * zoom)
//            buf.fillRect(right * zoom, 0, zoom, bottom * zoom)
//            buf.fillRect(right * zoom, top * zoom, zoom, (SCREEN_H - top) * zoom)
//        }
//    }

//        // draw monitor window
//        monitorWindow.draw {
//            val delta = Duration.between(prev, curr)
//            val fps = (1000.0 / delta.toMillis()).roundToInt()
//            it.color = Color(0, 0, 0)
//            it.drawString("${fps} fps", 0, 50)
//            it.drawString("SCX=${memory.get(ADDR_SCX)}", 0, 100)
//            it.drawString("SCY=${memory.get(ADDR_SCY)}", 0, 150)
//        }
//        prev = curr
//    }
    println("Emulation finished")
}

private val DUMMY_TILE_DATA =
    listOf(0x3C, 0x7E, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x7E, 0x5E, 0x7E, 0x0A, 0x7C, 0x56, 0x38, 0x7C)

//class MockMemoryImpl : Memory {
//    var counter = 0
//    override fun get(addr: Address): Int8 {
//        return when (addr) {
//            ADDR_SCX -> (counter++ / 200) % 256
//            ADDR_SCY -> (counter++ / 400) % 256
//            ADDR_WX -> 10
//            ADDR_WY -> 100
//            // 7	LCD and PPU enable	0=Off, 1=On
//            // 6	Window tile map area	0=9800-9BFF, 1=9C00-9FFF
//            // 5	Window enable	0=Off, 1=On
//            // 4	BG and Window tile data area	0=8800-97FF, 1=8000-8FFF
//            // 3	BG tile map area	0=9800-9BFF, 1=9C00-9FFF
//            // 2	OBJ size	0=8x8, 1=8x16
//            // 1	OBJ enable	0=Off, 1=On
//            // 0	BG and Window enable/priority	0=Off, 1=On
//            ADDR_LCDC -> 0b10100011
//            ADDR_BGP -> 0b00_01_10_11
//            ADDR_OBP0 -> 0b00_01_10_11
//            ADDR_OBP1 -> 0b00_01_10_11
//            in 0x8000..0x8FFF -> DUMMY_TILE_DATA[addr % 16] // BG and Window tile data area (LCDC4=1)
//            in 0x8800..0x97FF -> DUMMY_TILE_DATA[addr % 16] // BG and Window tile data area (LCDC4=0)
//            in 0x9800..0x9BFF -> 0 // BG/Window tile map data (LCDC3/6=0)
//            in 0x9C00..0x9FFF -> 0 // BG/Window tile map data (LCDC3/6=1)
//            in 0xFE00..0xFE9F -> dummyOAM(addr) // OAM (4byte * 40)
//            else -> throw IllegalArgumentException("Address = $addr")
//        }
//    }
//
//    private fun dummyOAM(addr: Address): Int8 {
//        val i = (addr - 0xFE00) / 4
//        val x = i * 8 * 2
//        val y = i * 8
//        // Bit7   BG and Window over OBJ (0=No, 1=BG and Window colors 1-3 over the OBJ)
//        // Bit6   Y flip          (0=Normal, 1=Vertically mirrored)
//        // Bit5   X flip          (0=Normal, 1=Horizontally mirrored)
//        // Bit4   Palette number  **Non CGB Mode Only** (0=OBP0, 1=OBP1)
//        // Bit3   Tile VRAM-Bank  **CGB Mode Only**     (0=Bank 0, 1=Bank 1)
//        // Bit2-0 Palette number  **CGB Mode Only**     (OBP0-7)
//        return listOf(y, x, 0, 0b00000000 + 0b10000000 * (i % 2))[addr % 4]
//    }
//}

