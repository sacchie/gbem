import cpu.*
import ppu.Address
import ppu.COLOR
import ppu.Window
import ppu.drawViewport

const val ADDR_TIMA = 0xFF05
const val ADDR_TMA = 0xFF06
const val ADDR_TAC = 0xFF07
const val ADDR_IF = 0xFF0F
fun loop(maxIterations: Int, memory: Memory, timer: Timer, drawMainWindow: () -> Unit) {
    val registers = InMemoryRegisters()

    //  state = memory & registers
    registers.setPc(0x100)

    var halted = false

    val state = object : State {
        override fun setHalted(b: Boolean) { halted = b }
        override fun getHalted(): Boolean { return halted }
    }

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

        handleInterrupts(memory, registers, state)

        if (!halted) {
            val pc = registers.getPc()
            val op = cpu.op.parse(memory, pc)
            System.err.println(
                "${(if (registers.callDepthForDebug >= 0) "*" else "-").repeat(Math.abs(registers.callDepthForDebug))} 0x${
                    pc.toString(
                        16
                    )
                }: $op"
            )
            //  CPUがstateを更新
            op.run(registers, memory, state)
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

data class Timer(
    private var divCounter: Int8 = 0,
    private var timaCounter: Int8 = 0,
    private var tima: Int8 = 0,
    var tma: Int8 = 0,
    var tac: Int8 = 0
) {
    // TODO
    // FF04に何かwriteしたらDIVは0x00にする
    // STOP Opでもresetされ、STOP mode解除されるまでtickが起こらなくなる
    fun tick(cycleCount: Int): Boolean {
        divCounter += cycleCount
        divCounter %= 64 * 256

        if (tac and 0b100 == 0) {
            return false
        }

        timaCounter += cycleCount
        val divider = when (tac.and(0b11)) {
            0b00 -> 1024
            0b01 -> 16
            0b10 -> 64
            0b11 -> 256
            else -> throw RuntimeException("invalid tac")
        }
        if (timaCounter >= divider) {
            timaCounter -= divider
            tima++
            if (tima > 0xFF) {
                tima = tma
                return true
            }
        }
        return false
    }

    fun getDiv() = (divCounter / 64) % 256
    fun getTima() = tima
    fun setTima(v: Int8) {
        tima = v
    }
}

fun main(args: Array<String>) {
    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom11.gb")!!.readAllBytes()
    val timer = Timer()
    val memory = object : MemoryImpl(romByteArray) {
        override fun setTima(value: Int8) = timer.setTima(value)
        override fun setTac(value: Int8) {
            timer.tac = value
        }
    }

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

    loop(Int.MAX_VALUE, memory, timer) {
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

// C000-CFFF: 4 KiB Work RAM (WRAM)
// 8000-97FF: VRAM Tile Data https://gbdev.io/pandocs/Tile_Data.html
// 9800-9FFF: VRAM Tile Maps https://gbdev.io/pandocs/Tile_Maps.html
abstract class MemoryImpl(private val romByteArray: ByteArray) : cpu.Memory, ppu.Memory {
    companion object {
        val HRAM_RANGE = 0xFF80..0xFFFE
        val OAM_RANGE = 0xFE00..0xFE9F
    }

    private val ram = MutableList(0x2000) { 0 }
    private val vram = MutableList(0x2000) { 0 }
    private val hram = MutableList(HRAM_RANGE.count()) { 0 }
    private val oam = MutableList(OAM_RANGE.count()) { 0 }

    private var LCDC: Int8 = 0
    private var STAT: Int8 = 0
    private var BGP: Int8 = 0
    private var OBP0: Int8 = 0
    private var OBP1: Int8 = 0
    private var SCY: Int8 = 0
    private var SCX: Int8 = 0
    private var WY: Int8 = 0
    private var WX: Int8 = 0
    private var LY: Int8 = 0
    private var IF: Int8 = 0
    private var IE: Int8 = 0

    fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF

    fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF
    fun getRamSize() = romByteArray[0x0149].toInt() and 0xFF

    private fun romBankEnd() = 0x7FFF

    override fun get8(addr: Int16): cpu.Int8 = when (addr) {
        in 0x0000..romBankEnd() -> romByteArray[addr].toInt() and 0xFF
        in 0xC000..0xDFFF -> ram[addr - 0xC000]
        in 0x8000..0x9FFF -> vram[addr - 0x8000]
        in HRAM_RANGE -> hram[addr - HRAM_RANGE.first]
        in OAM_RANGE -> oam[addr - OAM_RANGE.first]
        ADDR_IF -> {
            IF
        }
        0xFF40 -> LCDC
        0xFF41 -> STAT
        0xFF42 -> SCY
        0xFF43 -> SCX
        0xFF44 -> LY
        0xFF47 -> BGP
        0xFF48 -> OBP0
        0xFF49 -> OBP1
        0xFF4A -> WY
        0xFF4B -> WX
        0xFFFF -> IE
        else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
    }

    override fun get16(addr: Int16): Int16 = when (addr) {
        in 0x0000..romBankEnd() -> {
            val lo = romByteArray[addr + 0].toInt() and 0xFF
            val hi = romByteArray[addr + 1].toInt() and 0xFF
            hi.shl(8) + lo
        }

        in 0x8000..0x9FFE -> {
            val lo = vram[addr - 0x8000 + 0]
            val hi = vram[addr - 0x8000 + 1]
            hi.shl(8) + lo
        }

        in 0xC000..0xDFFE -> {
            val lo = ram[addr - 0xC000 + 0]
            val hi = ram[addr - 0xC000 + 1]
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
            0xFF01 -> {}
            0xFF02 -> {}
            0xFF05 -> setTima(int8)
            0xFF07 -> setTac(int8)
            ADDR_IF -> {
                IF = int8
            }

            0xFF26 -> {}
            0xFF25 -> {}
            0xFF24 -> {}
            0xFF40 -> {
                LCDC = int8
            }

            0xFF41 -> {
                STAT = int8
            }

            0xFF42 -> {
                SCY = int8
            }

            0xFF43 -> {
                SCX = int8
            }

            0xFF47 -> {
                BGP = int8
            }

            0xFF48 -> {
                OBP0 = int8
            }

            0xFF49 -> {
                OBP1 = int8
            }

            0xFF4A -> {
                WY = int8
            }

            0xFF4B -> {
                WX = int8
            }

            in HRAM_RANGE -> {
                hram[addr - HRAM_RANGE.first] = int8
                System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            0xFFFF -> {
                IE = int8
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

            in HRAM_RANGE.first..(HRAM_RANGE.last - 1) -> {
                hram[addr - HRAM_RANGE.first + 0] = int16.lo()
                hram[addr - HRAM_RANGE.first + 1] = int16.hi()
                System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
            }

            else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
        }
    }

    override fun get(addr: Address): ppu.Int8 {
        return get8(addr)
    }

    abstract fun setTima(value: Int8)
    abstract fun setTac(value: Int8)

    override fun getIfForDebug(): Int8 {
        return IF
    }
}

