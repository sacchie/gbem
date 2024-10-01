package emulator

import emulator.cpu.Int16
import emulator.cpu.Int8
import emulator.ppu.DebugParams
import emulator.ppu.PpuMode

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
    val wram: MutableList<Int8> = MutableList(0x2000) { 0 },
    val externalRam: MutableList<Int8> = MutableList(0x2000) { 0 }, // TODO banking for non-MBC1
    val vram: MutableList<Int8> = MutableList(0x2000) { 0 },
    val hram: MutableList<Int8> = MutableList(HRAM_RANGE.count()) { 0 },
    val oam: MutableList<Int8> = MutableList(OAM_RANGE.count()) { 0 },

    var LCDC: Int8 = 0,
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
    var LYC: Int8 = 0,
) {
    companion object {
        val HRAM_RANGE = 0xFF80..0xFFFE
        val OAM_RANGE = 0xFE00..0xFE9F
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

data class LcdStatusData(
    var lycIntSelected: Boolean = false,
    val modeSelected: MutableMap<PpuMode, Boolean> = mutableMapOf(
        PpuMode.MODE0 to false,
        PpuMode.MODE1 to false,
        PpuMode.MODE2 to false
    ),
)

data class State(
    val register: RegisterData = RegisterData(),
    val memory: MemoryData = MemoryData(),
    val timer: TimerData = TimerData(),
    var ppuMode: PpuMode = PpuMode.MODE2,
    var halted: Boolean = false,
    val p1: P1 = P1(),
    var ramEnable: Boolean = false, /* currently only for MBC1 (cartridgeType = $03) */
    val lcdStatusData: LcdStatusData = LcdStatusData(),
    var windowInternalLineCounter: Int = 0,
    val ppuDebugParams: DebugParams = DebugParams(drawBackground = true, drawWindow = true, drawSprites = true),
    var cycleCount: Int = 0,
)
