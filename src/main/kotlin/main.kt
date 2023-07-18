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
        //  PPUがstate.memory.VRAM領域を見て画面を更新
        //  APUがstate.memory.AUDIO領域を見て音を出す
        //  割り込みがあったらコールバックを実行
    }
}

fun main(args: Array<String>) {
    // load file
    val romByteArray = object {}.javaClass.getResourceAsStream("rom.gb")!!.readAllBytes()
    val memory = MemoryImpl(romByteArray)
    val registers = Registers()
    loop(10, memory, registers)
    println("Emulation finished")
}

// TODO C000-CFFF: 4 KiB Work RAM (WRAM)
class MemoryImpl(private val romByteArray: ByteArray) : Memory {
    override fun get8(addr: Int16): Int8 {
        return romByteArray[addr].toInt() and 0xFF
    }

    override fun get16(addr: Int16): Int16 {
        val lo = romByteArray[addr+0].toInt() and 0xFF
        val hi = romByteArray[addr +1].toInt() and 0xFF
        return hi.shl(8) + lo
    }

    override fun set8(addr: Int16, int8: Int8) {
        // TODO
        println("set8: ${addr} <- $int8")
    }

    override fun set16(addr: Int16, int16: Int16) {
        // TODO
        println("set16: ${addr} <- $int16")
    }
}