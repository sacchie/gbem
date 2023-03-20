package ppu


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
const val ADDR_WY = 0xFF4A
const val ADDR_WX = 0xFF4B

/**
 * Renders full background (256x256)
 */
fun drawBackgroundToScreen(memory: Memory, drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit) {
    val LCDC = memory.get(ADDR_LCDC)
    // Ignoring LCDC.0 (BG Enabled)
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000) 0x9C00 else 0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000

    for (iTile in 0..1023) {
        val tileX = iTile % 32
        val tileY = iTile / 32
        val tileId = memory.get(bgTileMapHead + iTile)
        drawBackgroundTileToScreen(memory, tileX, tileY, tileId, LCDC4, drawPixelToScreen)
    }
}

fun drawBackgroundTileToScreen(
    memory: Memory,
    tileX: Int,
    tileY: Int,
    tileId: Int8,
    LCDC4: Boolean,
    drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit
) {
    for (yOnTile in 0..7) {
        for (xOnTile in 0..7) {
            val xOnScreen = tileX * TILE_W + xOnTile
            val yOnScreen = tileY * TILE_H + yOnTile
            val colorId: Int2 = getColorIdOfPixelOnTile(memory, LCDC4, tileId, xOnTile, yOnTile)
            drawPixelToScreen(xOnScreen, yOnScreen, colorId)
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
        drawScanlineInViewport(memory, ly, drawPixelToScreen)
    }
}

/**
 * draw one scanline for LY = ly
 */
fun drawScanlineInViewport(
    memory: Memory,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit,
) {
    val LCDC = memory.get(ADDR_LCDC)
    val bgAndWindowEnabled = (LCDC and 0x0001) == 1
    if (bgAndWindowEnabled) {
        drawBackgroundForScanlineInViewport(memory, LCDC, ly, drawPixelToScreen)
    }
    val windowEnabled = (LCDC and 1.shl(5)) > 0
    if (windowEnabled && bgAndWindowEnabled) {
        drawWindowForScanlineInViewport(memory, LCDC, ly, drawPixelToScreen)
    }
    // TODO draw sprites
}

private fun drawBackgroundForScanlineInViewport(
    memory: Memory,
    LCDC: Int8,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit
) {
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000) 0x9C00 else 0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000
    val SCX = memory.get(ADDR_SCX)
    val SCY = memory.get(ADDR_SCY)
    val yOnScreen = (SCY + ly) % SCREEN_H
    val tileY = yOnScreen / TILE_H
    val yOnTile = yOnScreen % TILE_H
    for (lx in 0 until VIEWPORT_W) {
        val xOnScreen = (SCX + lx) % SCREEN_W
        val tileX = xOnScreen / TILE_W
        val xOnTile = xOnScreen % TILE_W

        val iTile = tileX + 32 * tileY
        val tileId = memory.get(bgTileMapHead + iTile)

        val colorId: Int2 = getColorIdOfPixelOnTile(memory, LCDC4, tileId, xOnTile, yOnTile)
        drawPixelToScreen(lx, ly, colorId)
    }
}

private fun drawWindowForScanlineInViewport(
    memory: Memory,
    LCDC: Int8,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, colorId: Int2) -> Unit
) {
    val windowTileMapHead: Address = if ((LCDC and 1.shl(6)) > 0) 0x9C00 else 0x9800
    val LCDC4 = (LCDC and 1.shl(4)) > 0
    val WX = memory.get(ADDR_WX) // X position plus 7
    val WY = memory.get(ADDR_WY)
    if (ly < WY) {
        return
    }
    val yOnWindow = ly - WY
    val tileY = yOnWindow / TILE_H
    val yOnTile = yOnWindow % TILE_H
    for (lx in 0 until VIEWPORT_W) {
        if (lx < WX - 7) continue
        val xOnWindow = lx - (WX - 7)
        val tileX = xOnWindow / TILE_W
        val xOnTile = xOnWindow % TILE_W

        val iTile = tileX + 32 * tileY
        val tileId = memory.get(windowTileMapHead + iTile)

        val colorId: Int2 = getColorIdOfPixelOnTile(memory, LCDC4, tileId, xOnTile, yOnTile)
        drawPixelToScreen(lx, ly, colorId)
    }
}

/**
 * LCDC4, タイルID、タイル上の(x,y) → カラーID
 */
private fun getColorIdOfPixelOnTile(
    memory: Memory,
    LCDC4: Boolean,
    tileId: Int8,
    xOnTile: Int,
    yOnTile: Int
): Int2 {
    val tileDataBaseAddress = if (LCDC4) 0x8000 else (if (tileId < 128) 0x9000 else 0x8000)
    val TILE_BYTES = 16
    val tileDataHeadAddress = tileDataBaseAddress + tileId * TILE_BYTES
    val byte0 = memory.get(tileDataHeadAddress + 2 * yOnTile + 0)
    val byte1 = memory.get(tileDataHeadAddress + 2 * yOnTile + 1)
    val bit = 7 - xOnTile
    val colorId: Int2 = ((byte0 and (1 shl bit)) shr bit) + ((byte1 and (1 shl bit)) shr bit) * 2
    return colorId
}

typealias Address = Int
typealias Int8 = Int
typealias Int2 = Int

interface Memory {
    fun get(addr: Address): Int8
}
