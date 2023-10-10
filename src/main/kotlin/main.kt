import cpu.*
import java.io.FileInputStream

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
    loop(Int.MAX_VALUE, memory, registers)
    System.err.println("Emulation finished")
}

// C000-CFFF: 4 KiB Work RAM (WRAM)
// 8000-97FF: VRAM Tile Data https://gbdev.io/pandocs/Tile_Data.html
// 9800-9FFF: VRAM Tile Maps https://gbdev.io/pandocs/Tile_Maps.html
class MemoryImpl(private val romByteArray: ByteArray) : Memory {
    companion object {
        val HRAM_RANGE = 0xFF80..0xFFFE
    }

    private val ram = MutableList(0x2000) {0}
    private val vram = MutableList(0x2000) {0}
    private val hram = MutableList(HRAM_RANGE.count()) {0}

    fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF

    fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF

    private fun romBankEnd() = 0x7FFF

    override fun get8(addr: Int16): Int8 = when (addr) {
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

    override fun set8(addr: Int16, int8: Int8) {
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
}