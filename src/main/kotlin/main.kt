import cpu.handleInterrupts
import cpu.run
import ppu.COLOR
import ppu.Handlers
import ppu.Window
import ppu.drawScanlineInViewport

const val ADDR_IF = 0xFF0F

fun loop(maxIterations: Int, state: State, drawMainWindow: () -> Unit) {
    state.register.pc = 0x100

    val registers = state.registers()
    val memory = state.memory()
    val timer = state.timer()

    repeat(maxIterations) {
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
        if (it % 200 == 0) {
            drawMainWindow()
            if (++(state.memory.LY) == 154) {
                state.memory.LY = 0
            }
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

    val zoom = 3
    val mainWindow = Window(zoom * 160, zoom * 144, "gbem", object : Handlers {
        override fun onA(pressed: Boolean) = state.setButtons(0, pressed)
        override fun onB(pressed: Boolean) = state.setButtons(1, pressed)
        override fun onStart(pressed: Boolean) = state.setButtons(3, pressed)
        override fun onSelect(pressed: Boolean) = state.setButtons(2, pressed)
        override fun onUp(pressed: Boolean) = state.setDPad(2, pressed)
        override fun onDown(pressed: Boolean) = state.setDPad(3, pressed)
        override fun onLeft(pressed: Boolean) = state.setDPad(1, pressed)
        override fun onRight(pressed: Boolean) = state.setDPad(0, pressed)
    })

    loop(Int.MAX_VALUE, state) {
        mainWindow.draw { buf ->
            drawScanlineInViewport(memory, memory.getLY()) { x, y, color ->
                buf.color = COLOR[color]
                buf.fillRect(x * zoom, y * zoom, zoom, zoom)
            }
        }
    }
    println("Emulation finished")
}
