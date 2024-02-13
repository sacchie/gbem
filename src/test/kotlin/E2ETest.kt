
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ppu.LCDColor
import ppu.drawScanlineInViewport
import java.net.URL

internal class E2ETest {
    @ParameterizedTest
    @ValueSource(strings = ["01-special", "02-interrupts", "03-op%20sp,hl"])
    fun loopTest(romFileName: String) {
        // TODO run on CI
        val romByteArray = URL("https://github.com/retrio/gb-test-roms/raw/master/cpu_instrs/individual/${romFileName}.gb").openStream().use {
            it.readAllBytes()
        }
        val state = State(romByteArray)
        val memory = state.memory()
        val buf: Array<Array<LCDColor>> = Array(160) { Array(144) { LCDColor.White } }
        loop({ it < 10000000 }, state) {
            drawScanlineInViewport(memory, memory.getLY()) { x, y, color ->
                buf[x][y] = color
            }
        }

        val expected = "#####                                       ##                                                                                                                 \n" +
                " ##  ##                                      ##                                                                                                                 \n" +
                " ##  ##   ####    #####   #####   ####    #####                                                                                                                 \n" +
                " #####       ##  ##      ##      ##  ##  ##  ##                                                                                                                 \n" +
                " ##       #####   ####    ####   ######  ##  ##                                                                                                                 \n" +
                " ##      ##  ##      ##      ##  ##      ##  ##                                                                                                                 \n" +
                " ##       #####  #####   #####    ####    #####"
        val dumped = dump(buf)
        println(dumped)
        assertThat(dumped).contains(expected)
    }

    private fun dump(buf: Array<Array<LCDColor>>): String {
        var s = ""
        for (y in 0 until 144) {
            for (x in 0 until 160) {
                s +=
                    when (buf[x][y]) {
                        LCDColor.White -> " "
                        LCDColor.LightGray -> "."
                        LCDColor.DarkGray -> "+"
                        LCDColor.Black -> "#"
                    }

            }
            s += "\n"
        }
        return s
    }
}
