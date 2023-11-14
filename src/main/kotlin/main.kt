
import cpu.*
import ppu.Address
import ppu.LCDColor
import ppu.Window
import java.awt.Color

fun loop(maxIterations: Int, memory: Memory, registers: Registers) {
    //  state = memory & registers
    registers.pc().set(0x100)

    repeat(maxIterations) {
        val pc = registers.pc().get()
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

        val op = parse(memory, pc)
        System.err.println("${"*".repeat(registers.callDepthForDebug)} 0x${pc.toString(16)}: $op")
        //  CPUがstateを更新
        op.run(registers, memory)
        // System.err.println(registers)
        //  PPUがstate.memory.VRAM領域を見て画面を更新
        //  APUがstate.memory.AUDIO領域を見て音を出す
        //  割り込みがあったらコールバックを実行
    }
}

fun main(args: Array<String>) {
    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()
    val memory = MemoryImpl(romByteArray)

    val cartridgeType = memory.getCartridgeType()
    val romSize = memory.getRomSize()
    System.err.println("cartridgeType: 0x${cartridgeType.toString(16)}, romSize: 0x${romSize.toString(16)}")

    val registers = Registers()

    // Create Background Debug Window
    val width = 256
    val height = 256
    val zoom = 5

    val mainWindow = Window(zoom * 160, zoom * 144, "gbem")
    val backgroundDebugWindow = Window(zoom * width, zoom * height, "gbem background debug")

    // Ref. https://gbdev.io/pandocs/Tile_Data.html
    val COLOR = mapOf(
        LCDColor.Black to Color(0x08, 0x18, 0x20),
        LCDColor.DarkGray to Color(0x34, 0x68, 0x56),
        LCDColor.LightGray to Color(0x88, 0xc0, 0x70),
        LCDColor.White to Color(0xe0, 0xf8, 0xd0),
    )

    // Create Monitor Window
    val monitorWindow = Window(500, 500, "gbem monitor")

    loop(Int.MAX_VALUE, memory, registers)

//    var prev = Instant.now()
//    while (true) {
//        val curr = Instant.now()

//    // draw main window
//    mainWindow.draw { buf ->
//        drawViewport(memory) { x, y, color ->
//            buf.color = COLOR[color]
//            buf.fillRect(x * zoom, y * zoom, zoom, zoom)
//        }
//    }
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

// C000-CFFF: 4 KiB Work RAM (WRAM)
// 8000-97FF: VRAM Tile Data https://gbdev.io/pandocs/Tile_Data.html
// 9800-9FFF: VRAM Tile Maps https://gbdev.io/pandocs/Tile_Maps.html
class MemoryImpl(private val romByteArray: ByteArray) : cpu.Memory, ppu.Memory {
    companion object {
        val HRAM_RANGE = 0xFF80..0xFFFE
    }

    private val ram = MutableList(0x2000) { 0 }
    private val vram = MutableList(0x2000) { 0 }
    private val hram = MutableList(HRAM_RANGE.count()) { 0 }

    fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF

    fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF

    private fun romBankEnd() = 0x7FFF

    override fun get8(addr: Int16): cpu.Int8 = when (addr) {
        in 0x0000..romBankEnd() -> romByteArray[addr].toInt() and 0xFF
        in 0xC000..0xDFFF -> ram[addr - 0xC000]
        in 0x8000..0x9FFF -> vram[addr - 0x8000]
        in HRAM_RANGE -> hram[addr - HRAM_RANGE.first]
        0xFF44 -> 0 // LY TODO
        else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
    }

    override fun get16(addr: Int16): Int16 = when (addr) {
        in 0x0000..romBankEnd() -> {
            val lo = romByteArray[addr + 0].toInt() and 0xFF
            val hi = romByteArray[addr + 1].toInt() and 0xFF
            hi.shl(8) + lo
        }

        in 0xC000..0xDFFE -> {
            val lo = ram[addr - 0xC000 + 0]
            val hi = ram[addr - 0xC000 + 1]
            hi.shl(8) + lo
        }

        in 0x8000..0x9FFE -> {
            val lo = vram[addr - 0x8000 + 0]
            val hi = vram[addr - 0x8000 + 1]
            hi.shl(8) + lo
        }

        else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
    }

    override fun set8(addr: Int16, int8: cpu.Int8) {
        when (addr) {
            in 0xC000..0xDFFF -> {
                ram[addr - 0xC000] = int8
                System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            in 0x8000..0x9FFF -> {
                vram[addr - 0x8000] = int8
                System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }
            // TODO: https://gbdev.io/pandocs/Hardware_Reg_List.html
            0xFF07 -> {}
            0xFF0F -> {}
            0xFFFF -> {}
            0xFF26 -> {}
            0xFF25 -> {}
            0xFF24 -> {}
            0xFF40 -> {}
            0xFF43 -> {}
            0xFF47 -> {}
            0xFF42 -> {}
            0xFF01 -> {}
            0xFF02 -> {}
            in HRAM_RANGE -> {
                hram[addr - HRAM_RANGE.first] = int8
                System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
        }
    }

    override fun set16(addr: Int16, int16: Int16) {
        when (addr) {
            in 0xC000..0xDFFE -> {
                ram[addr - 0xC000 + 0] = int16.lo()
                ram[addr - 0xC000 + 1] = int16.hi()
                System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
            }

            in 0x8000..0x9FFE -> {
                vram[addr - 0x8000 + 0] = int16.lo()
                vram[addr - 0x8000 + 1] = int16.hi()
                System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
            }

            else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
        }
    }
    override fun get(addr: Address): ppu.Int8 {
        return get8(addr)
    }
}

