import cpu.*
import java.io.FileInputStream

fun loop(maxIterations: Int, memory: Memory, registers: Registers) {
    //  state = memory & registers

    registers.pc().set(0x100)

    repeat(maxIterations) {
        val pc = registers.pc().get()
        val op = parse(memory, pc)
        System.err.println("${pc.toString(16)}: $op")
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
    System.err.println("cartridgeType: ${cartridgeType.toString(16)}, romSize: ${romSize.toString(16)}")

    val registers = Registers()
    loop(Int.MAX_VALUE, memory, registers)
    System.err.println("Emulation finished")
}

// TODO C000-CFFF: 4 KiB Work RAM (WRAM)
class MemoryImpl(private val romByteArray: ByteArray) : Memory {
    private val ram = MutableList(0x20000) {0}

    fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF

    fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF

    private fun romBankEnd() = 0x7FFF

    override fun get8(addr: Int16): Int8 = when (addr) {
        in 0x0000..romBankEnd() -> romByteArray[addr].toInt() and 0xFF
        in 0xC000..0xDFFF -> ram[addr - 0xC000]
        else -> throw RuntimeException("Invalid address: ${addr.toString(16)}")
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
        else -> throw RuntimeException("Invalid address: ${addr.toString(16)}")
    }

    override fun set8(addr: Int16, int8: Int8) {
        when (addr) {
            in 0xC000..0xDFFF -> {
                ram[addr - 0xC000] = int8
                System.err.println("set8: ${addr.toString(16)} <- $int8")
            }
            else -> throw RuntimeException("Invalid address: ${addr.toString(16)}")
        }
    }

    override fun set16(addr: Int16, int16: Int16) {
        when (addr) {
            in 0xC000..0xDFFE -> {
                ram[addr - 0xC000 + 0] = int16.lo()
                ram[addr - 0xC000 + 1] = int16.hi()
                System.err.println("set16: ${addr.toString(16)} <- $int16")
            }
            else -> throw RuntimeException("Invalid address: ${addr.toString(16)}")
        }
    }
}