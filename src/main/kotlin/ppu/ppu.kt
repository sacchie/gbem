package ppu

import java.awt.*
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE


// 160x144 pixel LCD
// 8x8 tiles (20x18)
// palette
// 3 layers: background, window, objects

// VRAM: ,0x8000-97FF (0x1800(6144) bytes, 16 bytes/tile, 384 tiles)
// BG/Window Tile maps: ,0x9800-,0x9BFF, ,0x9C00-,0x9FFF

// VRAM state, some I/O registers -> screen pixels


const val ADDR_LCDC = 0xFF40

val DUMMY_DATA = listOf(0x3C ,0x7E ,0x42 ,0x42 ,0x42 ,0x42 ,0x42 ,0x42 ,0x7E ,0x5E ,0x7E ,0x0A ,0x7C ,0x56 ,0x38 ,0x7C)

class MockMemoryImpl : Memory {
    override fun get(addr: Address): Int8 {
        return when (addr) {
            ADDR_LCDC -> 0b10100011
            else -> DUMMY_DATA.get(addr % 16)
        }
    }
}

const val WIDTH = 256
const val HEIGHT = 256

fun render(memory: Memory, gBuffer: Graphics, zoom: Int) {
    // Ref. https://gbdev.io/pandocs/Tile_Data.html
    val COLOR = listOf(Color(0x08, 0x18, 0x20),  Color(0x34, 0x68, 0x56), Color(0x88, 0xc0, 0x70), Color(0xe0, 0xf8, 0xd0))

    // render full background (256x256)
    val LCDC = memory.get(ADDR_LCDC)
    val bgEnabled = (LCDC and 0x0001) == 1
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000)  0x9C00 else  0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000

    // TODO add test for refactoring
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
                gBuffer.fillRect(x*zoom, y*zoom,zoom,zoom)
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