package ppu

import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE


// 160x144 pixel LCD
// 8x8 tiles (20x18)
// palette
// 3 layers: background, window, objects

// VRAM: $8000-97FF (0x1800(6144) bytes, 16 bytes/tile, 384 tiles)
// BG/Window Tile maps: $9800-$9BFF, $9C00-$9FFF

// VRAM state, some I/O registers -> screen pixels

fun render(memory: Memory) {
    val WIDTH = 256
    val HEIGHT = 256

    // Ref. https://gbdev.io/pandocs/Tile_Data.html
    val COLOR = listOf(Color(0x08, 0x18, 0x20),  Color(0x34, 0x68, 0x56), Color(0x88, 0xc0, 0x70), Color(0xe0, 0xf8, 0xd0))

    val mainFrame = JFrame("gbem")
    mainFrame.defaultCloseOperation = EXIT_ON_CLOSE
    mainFrame.isResizable = false
    val canvas = Canvas()
    canvas.setPreferredSize(Dimension(WIDTH, HEIGHT))

    mainFrame.contentPane.add(canvas)
    mainFrame.pack()
    mainFrame.setLocationRelativeTo(null)

    mainFrame.isVisible = true

    val gMain = canvas.getGraphics()
    val buffer = canvas.createImage(WIDTH, HEIGHT)
    val gBuffer = buffer.getGraphics()

    // read VRAM

    // TODO render full background (256x256)
    val LCDC = memory.get(0xFF40)
    val bgEnabled = (LCDC and 0x0001) == 1
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000)  0x9C00 else  0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000

    gBuffer.clearRect(0, 0, WIDTH, HEIGHT)

    for (iTile in 0..1023) {
        val tileX = iTile % 32
        val tileY = iTile / 32
        val tileId = memory.get(bgTileMapHead + iTile)
        val tileDataAddress = if (LCDC4) 0x8000 + tileId*16 else (if(tileId <128) 0x9000 else 0x8000) + tileId*16
        for (iTileDataBytePair in 0..7) {
            val byte0 = memory.get(tileDataAddress + 2*iTileDataBytePair + 0)
            val byte1 = memory.get(tileDataAddress + 2*iTileDataBytePair + 1)

            val y = tileY * 8 + iTileDataBytePair

            for (bit in 0..7) {
                val x = tileX * 8 + (7 - bit)

                val colorId: Int2 = ((byte0 and (1 shl bit)) shr bit) + ((byte1 and (1 shl bit)) shr bit) * 2
                gBuffer.color = COLOR[colorId]
                gBuffer.drawLine(x, y, x, y)
                gMain.drawImage(buffer, 0, 0, canvas)
            }
        }
    }

    // get Block0, OBJ0 tile data
//    vram.

    // reconstruct pixel states
}

typealias Address = Int
typealias Int8 = Int
typealias Int2 = Int

interface Memory {
    fun get(addr: Address): Int8
}

interface VRAM {
    fun get(addr: Address): Int8

    fun getPixelAt(x: Int, y: Int, startAddress: Address): Int2
}

class VRAMImpl : VRAM {
    override fun get(addr: Address): Int8 {
        TODO("Not yet implemented")
    }

    // x: 0(left) -> 7(right), y: 0(top) -> 7(bottom)
    override fun getPixelAt(x: Int, y: Int, startAddress: Address): Int2 {
        // (0,0)pixelの2bitを取る
        val byte0 = get(startAddress+0+0)
        val byte1 = get(startAddress+0+1)
        //byte1(7) byte0(7)
        val pixel: Int2 = (byte1 shr 6) + (byte0 shr 7)
        return pixel
    }
}