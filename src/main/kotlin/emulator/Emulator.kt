package emulator

import State
import cpu.run
import ppu.Handlers
import ppu.LCDColor
import ppu.drawScanlineInViewport

const val ADDR_IF = 0xFF0F

abstract class Emulation(romByteArray: ByteArray) {
    val state = State(romByteArray)

    abstract fun startDrawingScanLine(drawScanLine: (drawPixel: (x: Int, y: Int, color: LCDColor) -> Unit) -> Unit)

    fun run(maxIterationCount: Long = Long.MAX_VALUE) {
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

        loop(maxIterationCount, state)
    }

    fun getHandlers(): Handlers {
        return object : Handlers {
            override fun onA(pressed: Boolean) = state.setButtons(0, pressed)
            override fun onB(pressed: Boolean) = state.setButtons(1, pressed)
            override fun onStart(pressed: Boolean) = state.setButtons(3, pressed)
            override fun onSelect(pressed: Boolean) = state.setButtons(2, pressed)
            override fun onUp(pressed: Boolean) = state.setDPad(2, pressed)
            override fun onDown(pressed: Boolean) = state.setDPad(3, pressed)
            override fun onLeft(pressed: Boolean) = state.setDPad(1, pressed)
            override fun onRight(pressed: Boolean) = state.setDPad(0, pressed)
        }
    }

    private fun loop(maxIterationCount: Long, state: State) {
        state.register.pc = 0x100

        val registers = state.registers()
        val memory = state.memory()
        val timer = state.timer()

        var count = 0L
        while (count++ < maxIterationCount) {
            cpu.handleInterrupts(memory, registers, state.state())

            if (!state.halted) {
                val pc = registers.getPc()
                val op = cpu.op.parse(memory, pc)
//            System.err.println(
//                "${(if (state.register.callDepthForDebug >= 0) "*" else "-").repeat(Math.abs(state.register.callDepthForDebug))} 0x${
//                    pc.toString(
//                        16
//                    )
//                }: $op"
//            )
                //  CPUがstateを更新
                op.run(registers, memory, state.state())
            }

            // Timer
            val CYCLE_COUNT = 8 // FIXME とりあえず平均値近くで設定しているだけ（2-byte instructionを想定）
            if (timer.tick(CYCLE_COUNT)) {
                memory.set8(ADDR_IF, memory.get8(ADDR_IF) or 0b100)
            }

            //  PPUがstate.memory.VRAM領域を見て画面を更新
            if (count % 200L == 0L) {
                startDrawingScanLine {
                    drawScanlineInViewport(memory, state.memory.LY, it)
                }

                if (++(state.memory.LY) == 154) {
                    state.memory.LY = 0
                }
            }

            //  APUがstate.memory.AUDIO領域を見て音を出す
            //  割り込みがあったらコールバックを実行
        }
    }
}