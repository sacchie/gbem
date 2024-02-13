
import cpu.*
import ppu.Address
import ppu.Int8

data class RegisterData(
    var pc: Int16 = 0,
    var af: Int16 = 0,
    var bc: Int16 = 0,
    var de: Int16 = 0,
    var hl: Int16 = 0,
    var sp: Int16 = 0,
    var ime: Boolean = false,
    var callDepthForDebug: Int = 0,
)

data class MemoryData(
    val ram: MutableList<Int8> = MutableList(0x2000) { 0 },
    val vram: MutableList<Int8> = MutableList(0x2000) { 0 },
    val hram: MutableList<Int8> = MutableList(HRAM_RANGE.count()) { 0 },
    val oam: MutableList<Int8> = MutableList(OAM_RANGE.count()) { 0 },

    var LCDC: Int8 = 0,
    var STAT: Int8 = 0,
    var BGP: Int8 = 0,
    var OBP0: Int8 = 0,
    var OBP1: Int8 = 0,
    var SCY: Int8 = 0,
    var SCX: Int8 = 0,
    var WY: Int8 = 0,
    var WX: Int8 = 0,
    var LY: Int8 = 0,
    var IF: Int8 = 0,
    var IE: Int8 = 0,
) {
    companion object {
        val HRAM_RANGE = 0xFF80..0xFFFE
        val OAM_RANGE = 0xFE00..0xFE9F
    }
}

interface Memory : cpu.Memory, ppu.Memory {
    fun getCartridgeType(): Int8
    fun getRomSize(): Int8
    fun getRamSize(): Int8
}

abstract class Timer {
    abstract fun updateDivCounter(updateFn: (x: Int8) -> Int8)
    abstract fun getTac(): Int8
    abstract fun getTma(): Int8
    abstract fun getTima(): Int8
    abstract fun updateTima(updateFn: (x: Int8) -> Int8)
    abstract fun getTimaCounter(): Int8
    abstract fun updateTimaCounter(updateFn: (x: Int8) -> Int8)

    // TODO
    // FF04に何かwriteしたらDIVは0x00にする
    // STOP Opでもresetされ、STOP mode解除されるまでtickが起こらなくなる
    fun tick(cycleCount: Int): Boolean {
        updateDivCounter { (it + cycleCount) % (64 * 256) }

        if (getTac() and 0b100 == 0) {
            return false
        }

        updateTimaCounter { it + cycleCount }
        val divider = when (getTac().and(0b11)) {
            0b00 -> 1024
            0b01 -> 16
            0b10 -> 64
            0b11 -> 256
            else -> throw RuntimeException("invalid tac")
        }
        if (getTimaCounter() >= divider) {
            updateTimaCounter { it - divider }
            updateTima { it + 1 }
            if (getTima() > 0xFF) {
                updateTima { getTma() }
                return true
            }
        }
        return false
    }
}

data class TimerData(
    var divCounter: Int8 = 0,
    var timaCounter: Int8 = 0,
    var tima: Int8 = 0,
    var tma: Int8 = 0,
    var tac: Int8 = 0,
)

data class P1(
    var dPad: Int8 = 0b1111,
    var buttons: Int8 = 0b1111,
    var selectDPad: Boolean = false,
    var selectButtons: Boolean = false,
)

class State(
    private val romByteArray: ByteArray,
    val register: RegisterData = RegisterData(),
    val memory: MemoryData = MemoryData(),
    val timer: TimerData = TimerData(),
    var halted: Boolean = false,
    private val p1: P1 = P1(),
) {
    fun registers() = object : Registers {
        override fun toString() = objToStringHex(this)
        override fun getAf(): Int16 = register.af
        override fun setAf(x: Int16) {
            register.af = x
        }

        override fun getBc(): Int16 = register.bc
        override fun setBc(x: Int16) {
            register.bc = x
        }

        override fun getDe(): Int16 = register.de
        override fun setDe(x: Int16) {
            register.de = x
        }

        override fun getHl(): Int16 = register.hl
        override fun setHl(x: Int16) {
            register.hl = x
        }

        override fun getSp(): Int16 = register.sp
        override fun setSp(x: Int16) {
            register.sp = x
        }

        override fun getPc(): Int16 = register.pc
        override fun setPc(x: Int16) {
            register.pc = x
        }

        override fun getIme() = register.ime
        override fun setIme(on: Boolean) {
            register.ime = on
        }

        private fun getFlagOn(bitPos: Int): Boolean = register.af.and(1.shl(bitPos)) > 0
        private fun setFlagOn(bitPos: Int, on: Boolean) {
            register.af = if (on) {
                register.af.or(1.shl(bitPos))
            } else {
                register.af.and(1.shl(bitPos).inv().and(0xFFFF))
            }
        }

        override fun isZeroOn(): Boolean = getFlagOn(7)
        override fun setZero(on: Boolean) = setFlagOn(7, on)
        override fun isSubtractionOn(): Boolean = getFlagOn(6)
        override fun setSubtraction(on: Boolean) = setFlagOn(6, on)
        override fun isHalfCarryOn(): Boolean = getFlagOn(5)
        override fun setHalfCarry(on: Boolean) = setFlagOn(5, on)
        override fun isCarryOn(): Boolean = getFlagOn(4)
        override fun setCarry(on: Boolean) = setFlagOn(4, on)

        override fun incCallDepthForDebug(diff: Int) {
            register.callDepthForDebug += diff
        }
    }


    // C000-CFFF: 4 KiB Work RAM (WRAM)
    // 8000-97FF: VRAM Tile Data https://gbdev.io/pandocs/Tile_Data.html
    // 9800-9FFF: VRAM Tile Maps https://gbdev.io/pandocs/Tile_Maps.html
    fun memory() = object : Memory {
        override fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF
        override fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF
        override fun getRamSize() = romByteArray[0x0149].toInt() and 0xFF
        private fun romBankEnd() = 0x7FFF
        override fun get8(addr: Int16): cpu.Int8 = when (addr) {
            in 0x0000..romBankEnd() -> romByteArray[addr].toInt() and 0xFF
            in 0xC000..0xDFFF -> memory.ram[addr - 0xC000]
            in 0x8000..0x9FFF -> memory.vram[addr - 0x8000]
            in MemoryData.HRAM_RANGE -> memory.hram[addr - MemoryData.HRAM_RANGE.first]
            in MemoryData.OAM_RANGE -> memory.oam[addr - MemoryData.OAM_RANGE.first]
            0xFF00 -> {
                if (p1.selectButtons && !p1.selectDPad) {
                    0x20 + p1.buttons
                } else if (p1.selectDPad && !p1.selectButtons) {
                    0x10 + p1.dPad
                } else if (!p1.selectDPad && !p1.selectButtons){
                    0x0F
                } else {
                    throw RuntimeException("Invalid P1 select")
                }
            }

            ADDR_IF -> memory.IF
            0xFF40 -> memory.LCDC
            0xFF41 -> memory.STAT
            0xFF42 -> memory.SCY
            0xFF43 -> memory.SCX
            0xFF44 -> memory.LY
            0xFF47 -> memory.BGP
            0xFF48 -> memory.OBP0
            0xFF49 -> memory.OBP1
            0xFF4A -> memory.WY
            0xFF4B -> memory.WX
            0xFFFF -> memory.IE
            else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
        }

        override fun get16(addr: Int16): Int16 = when (addr) {
            in 0x0000..romBankEnd() -> {
                val lo = romByteArray[addr + 0].toInt() and 0xFF
                val hi = romByteArray[addr + 1].toInt() and 0xFF
                hi.shl(8) + lo
            }

            in 0x8000..0x9FFE -> {
                val lo = memory.vram[addr - 0x8000 + 0]
                val hi = memory.vram[addr - 0x8000 + 1]
                hi.shl(8) + lo
            }

            in 0xC000..0xDFFE -> {
                val lo = memory.ram[addr - 0xC000 + 0]
                val hi = memory.ram[addr - 0xC000 + 1]
                hi.shl(8) + lo
            }

            else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
        }

        override fun set8(addr: Int16, int8: cpu.Int8) {
            when (addr) {
                in 0xC000..0xDFFF -> {
                    memory.ram[addr - 0xC000] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
                }

                in 0x8000..0x9FFF -> {
                    memory.vram[addr - 0x8000] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
                }
                // TODO: https://gbdev.io/pandocs/Hardware_Reg_List.html
                0xFF00 -> {
                    p1.selectDPad = int8 and 0b10000 == 0
                    p1.selectButtons = int8 and 0b100000 == 0
                }
                0xFF01 -> {}
                0xFF02 -> {}
                0xFF05 -> setTima(int8)
                0xFF07 -> setTac(int8)
                ADDR_IF -> memory.IF = int8
                0xFF26 -> {}
                0xFF25 -> {}
                0xFF24 -> {}
                0xFF40 -> memory.LCDC = int8
                0xFF41 -> memory.STAT = int8
                0xFF42 -> memory.SCY = int8
                0xFF43 -> memory.SCX = int8
                0xFF46 -> {
                    // OAM DMA Transfer
                    val startAddr = int8 shl 8
                    for (i in 0 until 0xA0) {
                        memory.oam[i] = get8(startAddr + i)
                    }
                }

                0xFF47 -> memory.BGP = int8
                0xFF48 -> memory.OBP0 = int8
                0xFF49 -> memory.OBP1 = int8
                0xFF4A -> memory.WY = int8
                0xFF4B -> memory.WX = int8

                in MemoryData.HRAM_RANGE -> {
                    memory.hram[addr - MemoryData.HRAM_RANGE.first] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
                }

                0xFFFF -> {
                    memory.IE = int8
                }

                else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
            }
        }

        override fun set16(addr: Int16, int16: Int16) {
            when (addr) {
                in 0xC000..0xDFFE -> {
                    memory.ram[addr - 0xC000 + 0] = int16.lo()
                    memory.ram[addr - 0xC000 + 1] = int16.hi()
//                    System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
                }

                in 0x8000..0x9FFE -> {
                    memory.vram[addr - 0x8000 + 0] = int16.lo()
                    memory.vram[addr - 0x8000 + 1] = int16.hi()
//                    System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
                }

                in MemoryData.HRAM_RANGE.first..(MemoryData.HRAM_RANGE.last - 1) -> {
                    memory.hram[addr - MemoryData.HRAM_RANGE.first + 0] = int16.lo()
                    memory.hram[addr - MemoryData.HRAM_RANGE.first + 1] = int16.hi()
//                    System.err.println("set16: [0x${addr.toString(16)}] <- 0x${int16.toString(16)}")
                }

                else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
            }
        }

        override fun get(addr: Address): ppu.Int8 {
            return get8(addr)
        }

        override fun getLY() = memory.LY

        override fun getIfForDebug(): Int8 {
            return memory.IF
        }

        fun setTima(value: Int8) {
            timer.tima = value
        }

        fun setTac(value: Int8) {
            timer.tac = value
        }
    }

    fun timer(): Timer = object : Timer() {
        override fun updateDivCounter(updateFn: (x: Int8) -> Int8) {
            timer.divCounter = updateFn(timer.divCounter)
        }

        override fun getTac(): Int8 = timer.tac

        override fun getTma(): Int8 = timer.tma

        override fun getTima(): Int8 = timer.tima

        override fun updateTima(updateFn: (x: Int8) -> Int8) {
            timer.tima = updateFn(timer.tima)
        }

        override fun getTimaCounter(): Int = timer.timaCounter

        override fun updateTimaCounter(updateFn: (x: Int8) -> Int8) {
            timer.timaCounter = updateFn(timer.timaCounter)
        }
    }

    fun state(): cpu.State = object : cpu.State {
        override fun setHalted(b: Boolean) {
            halted = b
        }

        override fun getHalted(): Boolean {
            return halted
        }
    }

    fun setDPad(bit: Int, pressed: Boolean) {
        p1.dPad = if (pressed) {
            p1.dPad and (0b0001).shl(bit).inv()
        } else {
            p1.dPad or (0b0001).shl(bit)
        }
    }

    fun setButtons(bit: Int, pressed: Boolean) {
        p1.buttons = if (pressed) {
            p1.buttons and (0b0001).shl(bit).inv()
        } else {
            p1.buttons or (0b0001).shl(bit)
        }
    }
}
