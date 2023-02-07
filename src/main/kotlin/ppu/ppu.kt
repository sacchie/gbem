package ppu

import java.awt.*


// 160x144 pixel LCD
// 8x8 tiles (20x18)
// palette
// 3 layers: background, window, objects

// VRAM: ,0x8000-97FF (0x1800(6144) bytes, 16 bytes/tile, 384 tiles)
// BG/Window Tile maps: ,0x9800-,0x9BFF, ,0x9C00-,0x9FFF

// VRAM state, some I/O registers -> screen pixels


const val ADDR_LCDC = 0xFF40
const val ADDR_SCY = 0xFF42
const val ADDR_SCX = 0xFF43

fun drawScreen(memory: Memory, drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit) {
    // render full background (256x256)
    val LCDC = memory.get(ADDR_LCDC)
    val bgEnabled = (LCDC and 0x0001) == 1
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000)  0x9C00 else  0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000

    for (iTile in 0..1023) {
        val tileX = iTile % 32
        val tileY = iTile / 32
        val tileId = memory.get(bgTileMapHead + iTile)
        val tileDataBaseAddress = if (LCDC4) 0x8000 else (if (tileId < 128) 0x9000 else 0x8000)
        drawBackgroundTileToScreen(memory, tileX, tileY, tileId, tileDataBaseAddress, drawPixelToScreen)
    }
}

fun drawBackgroundTileToScreen(memory: Memory, tileX: Int, tileY: Int, tileId: Int8, tileDataBaseAddress: Address, drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit) {
    val tileDataAddress = tileDataBaseAddress + tileId*16
    for (iTileDataBytePair in 0..7) {
        val byte0 = memory.get(tileDataAddress + 2*iTileDataBytePair + 0)
        val byte1 = memory.get(tileDataAddress + 2*iTileDataBytePair + 1)

        val y = tileY * 8 + iTileDataBytePair
        for (bit in 0..7) {
            val x = tileX * 8 + (7 - bit)
            val colorId: Int2 = ((byte0 and (1 shl bit)) shr bit) + ((byte1 and (1 shl bit)) shr bit) * 2
            drawPixelToScreen(x, y, colorId)
        }
    }
}

const val TILE_W = 8
const val TILE_H = 8
const val SCREEN_W = 256
const val SCREEN_H = 256
const val VIEWPORT_W = 160
const val VIEWPORT_H = 144

/**
 * The graphics are rendered from top to bottom in a loop like this:
 * - Load row LY of the background tile view to the line buffer.
 * - Overwrite the line buffer with row LY from the window tile view.
 * - Sprite engine generates a 1-pixel section of the sprites, where they intersect LY and overwrites the line buffer with this.
 */
fun drawViewport(memory: Memory, drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit) {
    for (ly in 0 until VIEWPORT_H) {
        // TODO set LY register

        //  draw one scanline for LY = ly
        val LCDC = memory.get(ADDR_LCDC)
        val bgEnabled = (LCDC and 0x0001) == 1
        if (bgEnabled) {
            val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000) 0x9C00 else 0x9800
            val LCDC4 = (LCDC and 0b10000) == 0b10000

            val SCY = memory.get(ADDR_SCY)
            val yScreen = (SCY + ly) % SCREEN_H
            val tileY = yScreen / TILE_H
            val yOnTile = yScreen % TILE_H
            val SCX = memory.get(ADDR_SCX)
            for (lx in 0 until VIEWPORT_W) {
                val xScreen = (SCX + lx) % SCREEN_W
                val tileX = xScreen / TILE_W
                val xOnTile = xScreen % TILE_W
                val iTile = tileX + 32 * tileY
                // draw pixel (xOnTile, yOnTile) in iTile to (lx, ly)
                val tileId = memory.get(bgTileMapHead + iTile)
                val tileDataBaseAddress = if (LCDC4) 0x8000 else (if (tileId < 128) 0x9000 else 0x8000)

                //
                val tileDataAddress = tileDataBaseAddress + tileId*16
                val iTileDataBytePair = yOnTile

                val byte0 = memory.get(tileDataAddress + 2*iTileDataBytePair + 0)
                val byte1 = memory.get(tileDataAddress + 2*iTileDataBytePair + 1)

                val bit = 7 - xOnTile
                val colorId: Int2 = ((byte0 and (1 shl bit)) shr bit) + ((byte1 and (1 shl bit)) shr bit) * 2
                drawPixelToScreen(lx, ly, colorId)
                //
            }
        }
    }
}

typealias Address = Int
typealias Int8 = Int
typealias Int2 = Int

interface Memory {
    fun get(addr: Address): Int8
}
