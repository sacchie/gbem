package emulator

import emulator.cpu.*
import emulator.cpu.Int8
import emulator.cpu.op.parse
import emulator.ppu.*

const val ADDR_IF = 0xFF0F

interface JoypadHandlers {
    fun onA(pressed: Boolean)
    fun onB(pressed: Boolean)
    fun onStart(pressed: Boolean)
    fun onSelect(pressed: Boolean)
    fun onUp(pressed: Boolean)
    fun onDown(pressed: Boolean)
    fun onLeft(pressed: Boolean)
    fun onRight(pressed: Boolean)
}

interface DebugHandlers {
    fun toggleDrawBackground()
    fun toggleDrawWindow()
    fun toggleDrawSprites()
    fun printOamData()
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

fun makeRegisters(register: RegisterData) = object : Registers {
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

interface Memory : emulator.cpu.Memory, emulator.ppu.Memory {
    fun getCartridgeType(): Int8
    fun getRomSize(): Int8
    fun getRamSize(): Int8
}

fun Memory.enableInterruptFlag(flag: Int8) = set8(ADDR_IF, get8(ADDR_IF) or flag)


interface LcdStatus {
    fun getLycIntSelect(): Boolean
    fun setLycIntSelect(selected: Boolean)
    fun getModeSelect(ppuMode: PpuMode): Boolean
    fun setModeSelect(ppuMode: PpuMode, selected: Boolean)
    fun getLycEqualsLy(): Boolean
    fun getPpuMode(): PpuMode
}

// TODO
// - MBC1
// - ROM size = 32 KiB, # of rom banks = 2 (no banking)
// - RAM size = 8 KiB, 1 bank

// C000-CFFF: 4 KiB Work RAM (WRAM)
// 8000-97FF: VRAM Tile Data https://gbdev.io/pandocs/Tile_Data.html
// 9800-9FFF: VRAM Tile Maps https://gbdev.io/pandocs/Tile_Maps.html
fun makeMemory(
    romByteArray: ByteArray,
    memory: MemoryData,
    timer: TimerData,
    p1: P1,
    setRamEnable: (on: Boolean) -> Unit,
    lcdStatus: LcdStatus,
) = object : Memory {
    override fun getCartridgeType() = romByteArray[0x0147].toInt() and 0xFF
    override fun getRomSize() = romByteArray[0x0148].toInt() and 0xFF
    override fun getRamSize() = romByteArray[0x0149].toInt() and 0xFF
    private fun romBankEnd() = 0x7FFF
    override fun get8(addr: Int16): emulator.cpu.Int8 = when (addr) {
        in 0x0000..romBankEnd() -> romByteArray[addr].toInt() and 0xFF
        in 0xC000..0xDFFF -> memory.wram[addr - 0xC000]
        in 0x8000..0x9FFF -> memory.vram[addr - 0x8000]
        in 0xA000..0xBFFF -> {
            if (getCartridgeType() != 0x3) {
                throw RuntimeException()
            }
            memory.externalRam[addr - 0xA000]
        }

        in MemoryData.HRAM_RANGE -> memory.hram[addr - MemoryData.HRAM_RANGE.first]
        in MemoryData.OAM_RANGE -> memory.oam[addr - MemoryData.OAM_RANGE.first]
        0xFF00 -> {
            if (p1.selectButtons && !p1.selectDPad) {
                0x20 + p1.buttons
            } else if (p1.selectDPad && !p1.selectButtons) {
                0x10 + p1.dPad
            } else if (!p1.selectDPad && !p1.selectButtons) {
                0x0F
            } else {
                throw RuntimeException("Invalid emulator.P1 select")
            }
        }

        0xFF05 -> timer.tima

        0xFF0F -> memory.IF
        0xFF12 -> 0 /* TODO audio */
        0xFF17 -> 0 /* TODO audio */
        0xFF26 -> 0 /* TODO NR52: Audio master control */
        0xFF40 -> memory.LCDC
        0xFF41 ->
            (when (lcdStatus.getPpuMode()) {
                PpuMode.MODE0 -> 0
                PpuMode.MODE1 -> 1
                PpuMode.MODE2 -> 2
                PpuMode.MODE3 -> 3
            }).or(
                if (lcdStatus.getLycEqualsLy()) 1.shl(2) else 0
            ).or(
                if (lcdStatus.getModeSelect(PpuMode.MODE0)) 1.shl(3) else 0
            ).or(
                if (lcdStatus.getModeSelect(PpuMode.MODE1)) 1.shl(4) else 0
            ).or(
                if (lcdStatus.getModeSelect(PpuMode.MODE2)) 1.shl(5) else 0
            ).or(
                if (lcdStatus.getLycIntSelect()) 1.shl(6) else 0
            )

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
        in 0x0000 until romBankEnd() -> {
            val lo = romByteArray[addr + 0].toInt() and 0xFF
            val hi = romByteArray[addr + 1].toInt() and 0xFF
            hi.shl(8) + lo
        }

        in 0x8000 until 0x9FFF -> {
            val lo = memory.vram[addr - 0x8000 + 0]
            val hi = memory.vram[addr - 0x8000 + 1]
            hi.shl(8) + lo
        }

        in 0xC000 until 0xDFFF -> {
            val lo = memory.wram[addr - 0xC000 + 0]
            val hi = memory.wram[addr - 0xC000 + 1]
            hi.shl(8) + lo
        }

        in MemoryData.HRAM_RANGE.first until MemoryData.HRAM_RANGE.last -> {
            val lo = memory.hram[addr - MemoryData.HRAM_RANGE.first + 0]
            val hi = memory.hram[addr - MemoryData.HRAM_RANGE.first + 1]
            hi.shl(8) + lo
        }

        else -> throw RuntimeException("Invalid address: 0x${addr.toString(16)}")
    }

    override fun set8(addr: Int16, int8: emulator.cpu.Int8) {
        when (addr) {
            0x0000 -> {
                if (getCartridgeType() != 0x3 /* not MBC1 */) {
                    throw RuntimeException()
                } else {
                    setRamEnable(int8 == 0xA)
                }
            }

            0x2000 -> {} /* just ignore, implementaion required for ROM banking */
            0x4000 -> {} /* just ignore, implementaion required for ROM banking */

            in 0xA000..0xBFFF -> {
                if (getCartridgeType() != 0x3) {
                    throw RuntimeException()
                }
                memory.externalRam[addr - 0xA000] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            in 0xC000..0xDFFF -> {
                memory.wram[addr - 0xC000] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            in 0x8000..0x9FFF -> {
                memory.vram[addr - 0x8000] = int8
//                    System.err.println("set8: [0x${addr.toString(16)}] <- 0x${int8.toString(16)}")
            }

            in MemoryData.OAM_RANGE -> {
                // TODO direct access to OAM is not allowed during mode 2 and 3
                memory.oam[addr - MemoryData.OAM_RANGE.first] = int8
            }

            // TODO: https://gbdev.io/pandocs/Hardware_Reg_List.html
            0xFF00 -> {
                p1.selectDPad = int8 and 0b10000 == 0
                p1.selectButtons = int8 and 0b100000 == 0
            }

            0xFF01 -> {}
            0xFF02 -> {}
            0xFF05 -> setTima(int8)
            0xFF06 -> setTma(int8)
            0xFF07 -> setTac(int8)
            0xFF0F -> memory.IF = int8
            in 0xFF10..0xFF26 -> {} /* TODO audio */
            in 0xFF30..0xFF3F -> {} /* TODO audio */
            0xFF40 -> memory.LCDC = int8
            0xFF41 -> {
                lcdStatus.setModeSelect(PpuMode.MODE0, int8.and(1.shl(3)) != 0)
                lcdStatus.setModeSelect(PpuMode.MODE1, int8.and(1.shl(4)) != 0)
                lcdStatus.setModeSelect(PpuMode.MODE2, int8.and(1.shl(5)) != 0)
                lcdStatus.setLycIntSelect(int8.and(1.shl(6)) != 0)
            }

            0xFF42 -> memory.SCY = int8
            0xFF43 -> memory.SCX = int8
            0xFF45 -> {
                // assert(int8 < 154)
                memory.LYC = int8
            }

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
                memory.wram[addr - 0xC000 + 0] = int16.lo()
                memory.wram[addr - 0xC000 + 1] = int16.hi()
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

    override fun get(addr: Address): Int8 {
        return get8(addr)
    }

    override fun getLY() = memory.LY

    override fun getIfForDebug(): Int8 {
        return memory.IF
    }

    fun setTima(value: Int8) {
        timer.tima = value
    }

    fun setTma(value: Int8) {
        timer.tma = value
    }

    fun setTac(value: Int8) {
        timer.tac = value
    }
}

fun makeTimer(timer: TimerData): Timer = object : Timer() {
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


abstract class Emulation(private val romByteArray: ByteArray) {
    val state = State()
    val stepFn: () -> Unit

    init {
        val registers = makeRegisters(state.register)
        val memory = makeMemory(
            romByteArray,
            state.memory,
            state.timer,
            state.p1,
            { on -> state.ramEnable = on },
            makeLcdStatus()
        )
        val timer = makeTimer(state.timer)

        val haltState = object : HaltState {
            override fun setHalted(b: Boolean) {
                state.halted = b
            }

            override fun getHalted(): Boolean {
                return state.halted
            }
        }

        val cartridgeType = memory.getCartridgeType()
        val romSize = memory.getRomSize()
        val ramSize = memory.getRamSize()

        /*
        System.err.println(
            "cartridgeType: 0x${cartridgeType.toString(16)}, romSize: 0x${romSize.toString(16)}, ramSize: 0x${
                ramSize.toString(
                    16
                )
            })"
        )
        */

        // initialize like boot ROM
        registers.setA(0x01)
        registers.setZero(true)
        registers.setSubtraction(false)
        // TODO set or reset according to header checksum
        registers.setB(0x00)
        registers.setC(0x13)
        registers.setD(0x00)
        registers.setE(0xD8)
        registers.setH(0x01)
        registers.setL(0x4D)
        state.register.pc = 0x100
        state.register.sp = 0xFFFE

        stepFn = {
            handleInterrupts(memory, registers, haltState)

            val cycleCount = if (!state.halted) {
                val pc = registers.getPc()
                val op = parse(memory, pc)
                /*
if (count > 1000000L) {
System.err.println(
    "${(if (state.register.callDepthForDebug >= 0) "*" else "-").repeat(Math.abs(state.register.callDepthForDebug))} 0x${
        pc.toString(
            16
        )
    }: $op"
)
                }
                */
                //  CPUがstateを更新
                op.run(registers, memory, haltState)
            } else {
                4
            }

            // emulator.Timer
            if (timer.tick(cycleCount)) {
                memory.enableInterruptFlag(0b100)
            }

            state.cycleCount += cycleCount

            fun incrementLY() {
                state.memory.LY++
                if (state.memory.LYC == state.memory.LY && state.lcdStatusData.lycIntSelected) {
                    memory.enableInterruptFlag(0b10)
                }
                state.cycleCount %= 456
            }

            // TODO Mode2, 3の間はOAMにアクセスできない
            if (state.ppuMode == PpuMode.MODE2 && state.cycleCount >= 80) {
                state.ppuMode = PpuMode.MODE3
            } else if (state.ppuMode == PpuMode.MODE3 && state.cycleCount >= 80 + 170) {
                state.ppuMode = PpuMode.MODE0
                if (state.lcdStatusData.modeSelected[PpuMode.MODE0]!!) {
                    memory.enableInterruptFlag(0b10)
                }
                startDrawingScanLine(state.memory.LY, state.ppuDebugParams) {
                    drawScanlineInViewport(
                        memory,
                        state.memory.LY,
                        state.windowInternalLineCounter,
                        { state.windowInternalLineCounter++ },
                        state.ppuDebugParams,
                        it
                    )
                }
            } else if (state.ppuMode == PpuMode.MODE0 && state.cycleCount >= 80 + 170 + 206) {
                incrementLY()

                if (state.memory.LY == 144) {
                    state.ppuMode = PpuMode.MODE1 // VBLANK
                    memory.enableInterruptFlag(0b1)
                    if (state.lcdStatusData.modeSelected[PpuMode.MODE1]!!) {
                        memory.enableInterruptFlag(0b10)
                    }
                    state.windowInternalLineCounter = 0
                } else {
                    state.ppuMode = PpuMode.MODE2
                    if (state.lcdStatusData.modeSelected[PpuMode.MODE2]!!) {
                        memory.enableInterruptFlag(0b10)
                    }
                }
            } else if (state.ppuMode == PpuMode.MODE1 && state.cycleCount >= 456) {
                incrementLY()

                if (state.memory.LY == 154) {
                    state.ppuMode = PpuMode.MODE2
                    if (state.lcdStatusData.modeSelected[PpuMode.MODE2]!!) {
                        memory.enableInterruptFlag(0b10)
                    }
                    state.memory.LY = 0
                }
            }

            //  APUがstate.memory.AUDIO領域を見て音を出す
            //  割り込みがあったらコールバックを実行
        }

    }

    abstract fun startDrawingScanLine(
        ly: Int,
        ppuDebugParams: DebugParams,
        drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit
    )

    fun getJoypadHandlers(): JoypadHandlers {
        fun setDPad(bit: Int, pressed: Boolean) {
            state.p1.dPad = if (pressed) {
                state.p1.dPad and (0b0001).shl(bit).inv()
            } else {
                state.p1.dPad or (0b0001).shl(bit)
            }
        }

        fun setButtons(bit: Int, pressed: Boolean) {
            state.p1.buttons = if (pressed) {
                state.p1.buttons and (0b0001).shl(bit).inv()
            } else {
                state.p1.buttons or (0b0001).shl(bit)
            }
        }

        return object : JoypadHandlers {
            override fun onA(pressed: Boolean) = setButtons(0, pressed)
            override fun onB(pressed: Boolean) = setButtons(1, pressed)
            override fun onStart(pressed: Boolean) = setButtons(3, pressed)
            override fun onSelect(pressed: Boolean) = setButtons(2, pressed)
            override fun onUp(pressed: Boolean) = setDPad(2, pressed)
            override fun onDown(pressed: Boolean) = setDPad(3, pressed)
            override fun onLeft(pressed: Boolean) = setDPad(1, pressed)
            override fun onRight(pressed: Boolean) = setDPad(0, pressed)
        }
    }

    fun getDebugHandlers(): DebugHandlers {
        return object : DebugHandlers {
            override fun toggleDrawBackground() {
                state.ppuDebugParams.drawBackground = !state.ppuDebugParams.drawBackground
            }

            override fun toggleDrawWindow() {
                state.ppuDebugParams.drawWindow = !state.ppuDebugParams.drawWindow
            }

            override fun toggleDrawSprites() {
                state.ppuDebugParams.drawSprites = !state.ppuDebugParams.drawSprites
            }

            override fun printOamData() {
                val memory = makeMemory(romByteArray, state.memory, state.timer, state.p1, { on -> }, makeLcdStatus())
                for (i in 0..39) {
                    println("$i OAM data: ${memory.getOamData(i)}")
                }
            }
        }
    }

    fun makeLcdStatus() = object : LcdStatus {
        override fun getLycIntSelect() = state.lcdStatusData.lycIntSelected

        override fun setLycIntSelect(selected: Boolean) {
            state.lcdStatusData.lycIntSelected = selected
        }

        override fun getModeSelect(ppuMode: PpuMode) = state.lcdStatusData.modeSelected[ppuMode]!!

        override fun setModeSelect(ppuMode: PpuMode, selected: Boolean) {
            state.lcdStatusData.modeSelected[ppuMode] = selected
        }

        override fun getLycEqualsLy() = state.memory.LYC == state.memory.LY

        override fun getPpuMode() = state.ppuMode
    }

    fun step() {
        stepFn()
    }

    fun run(maxLoopCount: Long = Long.MAX_VALUE) {
        for (i in 0 until maxLoopCount) {
            step()
        }
    }
}
