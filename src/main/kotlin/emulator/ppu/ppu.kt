package emulator.ppu


// 160x144 pixel LCD
// 8x8 tiles (20x18)
// palette
// 3 layers: background, window, objects

// VRAM: ,0x8000-97FF (0x1800(6144) bytes, 16 bytes/tile, 384 tiles)
// BG/Window Tile maps: ,0x9800-,0x9BFF, ,0x9C00-,0x9FFF

// VRAM state, some I/O registers -> screen pixels

// Note about palette
// for sprites, DotData = 0 means transparent

const val ADDR_LCDC = 0xFF40
const val ADDR_SCY = 0xFF42
const val ADDR_SCX = 0xFF43
const val ADDR_BGP = 0xFF47
const val ADDR_OBP0 = 0xFF48
const val ADDR_OBP1 = 0xFF49
const val ADDR_WY = 0xFF4A
const val ADDR_WX = 0xFF4B

/**
 * Renders full background (256x256)
 */
fun drawBackgroundToScreen(memory: Memory, drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit) {
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
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit
) {
    val BGP = memory.get(ADDR_BGP)
    for (yOnTile in 0..7) {
        for (xOnTile in 0..7) {
            val xOnScreen = tileX * TILE_W + xOnTile
            val yOnScreen = tileY * TILE_H + yOnTile
            val dotData = getDotDataOfPixelOnTileForBackgroundAndWindow(memory, LCDC4, tileId, xOnTile, yOnTile)
            val color = getColorForBackgroundAndWindow(dotData, BGP)
            drawPixelToScreen(xOnScreen, yOnScreen, color)
        }
    }
}

const val TILE_W = 8
const val TILE_H = 8
const val SCREEN_W = 256
const val SCREEN_H = 256
const val VIEWPORT_W = 160
const val VIEWPORT_H = 144

enum class LCDColor {
    White, LightGray, DarkGray, Black;
}

/**
 * draw one scanline for LY = ly
 */
fun drawScanlineInViewport(
    memory: Memory,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit,
) {
    if (ly >= VIEWPORT_H) {
        return
    }

    val pixelMap = mutableMapOf<Int, LCDColor>()

    val LCDC = memory.get(ADDR_LCDC)
    val bgAndWindowEnabled = (LCDC and 0x0001) == 1
    if (bgAndWindowEnabled) {
        drawBackgroundForScanlineInViewport(memory, LCDC, ly) { x, _, color -> pixelMap[x] = color }
    }
    val windowEnabled = (LCDC and 1.shl(5)) > 0
    if (windowEnabled && bgAndWindowEnabled) {
        drawWindowForScanlineInViewport(memory, LCDC, ly) { x, _, color -> pixelMap[x] = color }
    }
    putSpritePixelsForScanlineInViewportToBuffer(memory, LCDC, ly) { x, color, bgAndWindowOverObj ->
        if (bgAndWindowOverObj) {
            if (pixelMap[x] == null) {
                pixelMap[x] = color
            }
        } else {
            pixelMap[x] = color
        }
    }

    pixelMap.forEach { (x, color) ->
        drawPixelToScreen(x, ly, color)
    }
}

private fun drawBackgroundForScanlineInViewport(
    memory: Memory,
    LCDC: Int8,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit
) {
    val bgTileMapHead: Address = if ((LCDC and 0b1000) == 0b1000) 0x9C00 else 0x9800
    val LCDC4 = (LCDC and 0b10000) == 0b10000
    val SCX = memory.get(ADDR_SCX)
    val SCY = memory.get(ADDR_SCY)
    val yOnScreen = (SCY + ly) % SCREEN_H
    val tileY = yOnScreen / TILE_H
    val yOnTile = yOnScreen % TILE_H
    val BGP = memory.get(ADDR_BGP)
    for (lx in 0 until VIEWPORT_W) {
        val xOnScreen = (SCX + lx) % SCREEN_W
        val tileX = xOnScreen / TILE_W
        val xOnTile = xOnScreen % TILE_W

        val iTile = tileX + 32 * tileY
        val tileId = memory.get(bgTileMapHead + iTile)

        val dotData: Int2 = getDotDataOfPixelOnTileForBackgroundAndWindow(memory, LCDC4, tileId, xOnTile, yOnTile)
        val color = getColorForBackgroundAndWindow(dotData, BGP)
        drawPixelToScreen(lx, ly, color)
    }
}

private fun getColorForBackgroundAndWindow(dotData: Int2, BGP: Int8): LCDColor {
    return toColor(
        when (dotData) {
            0 -> BGP.and(0b11)
            1 -> BGP.and(0b1100).shr(2)
            2 -> BGP.and(0b110000).shr(4)
            3 -> BGP.and(0b11000000).shr(6)
            else -> throw IllegalArgumentException()
        }
    )
}

// null means transparent
private fun getColorForSprite(dotData: Int2, OBP: Int8): LCDColor? {
    return (when (dotData) {
        0 -> null // transparent
        1 -> OBP.and(0b1100).shr(2)
        2 -> OBP.and(0b110000).shr(4)
        3 -> OBP.and(0b11000000).shr(6)
        else -> throw IllegalArgumentException()
    })?.let { toColor(it) }
}

private fun toColor(value: Int2) = when (value) {
    0 -> LCDColor.White
    1 -> LCDColor.LightGray
    2 -> LCDColor.DarkGray
    3 -> LCDColor.Black
    else -> throw IllegalArgumentException()
}

private fun drawWindowForScanlineInViewport(
    memory: Memory,
    LCDC: Int8,
    ly: Int,
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit
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
    val BGP = memory.get(ADDR_BGP)
    for (lx in 0 until VIEWPORT_W) {
        if (lx < WX - 7) continue
        val xOnWindow = lx - (WX - 7)
        val tileX = xOnWindow / TILE_W
        val xOnTile = xOnWindow % TILE_W

        val iTile = tileX + 32 * tileY
        val tileId = memory.get(windowTileMapHead + iTile)

        val dotData = getDotDataOfPixelOnTileForBackgroundAndWindow(memory, LCDC4, tileId, xOnTile, yOnTile)
        val color = getColorForBackgroundAndWindow(dotData, BGP)
        drawPixelToScreen(lx, ly, color)
    }
}

private fun putSpritePixelsForScanlineInViewportToBuffer(
    memory: Memory,
    LCDC: Int8,
    ly: Int,
    putPixel: (x: Int, color: LCDColor, bgAndWindowOverObj: Boolean) -> Unit
) {
    // OAM (0xFE00-FE9F) から ly にあるsprite（上限10個）を取ってきて、描画
    val is8x16Mode = LCDC.and(0b100) > 0
    var drawCount = 0
    for (oamHeadAddress in 0xFE00..0xFE9F step 4) {
        val yPosition = memory.get(oamHeadAddress) - 16
        val xPosition = memory.get(oamHeadAddress + 1) - 8
        val tileId = memory.get(oamHeadAddress + 2)
        val attributes = memory.get(oamHeadAddress + 3)
        val bgAndWindowOverObj = attributes.and(0b10000000) > 0
        val yFlip = attributes.and(0b01000000) > 0
        val xFlip = attributes.and(0b00100000) > 0
        val OBP = if (attributes.and(0b00010000) > 0) memory.get(ADDR_OBP1) else memory.get(ADDR_OBP0)
        // スプライトの存在範囲y座標：yPosition - yPosition + 7/15
        if (ly in yPosition until yPosition + if (is8x16Mode) 16 else 8) {
            drawCount++
            val yOnTile = if (yFlip) 15 - (ly - yPosition) else ly - yPosition
            for (xTmp in 0 until 8) {
                val xOnTile = if (xFlip) 7 - (xTmp) else xTmp
                val xOnScreen = xPosition + xTmp
                val dotData = getDotDataOfPixelOnTileForSprites(memory, tileId, xOnTile, yOnTile)
                val color = getColorForSprite(dotData, OBP)
                if (color != null) {
                    putPixel(xOnScreen, color, bgAndWindowOverObj)
                }
            }
            if (drawCount == 10) {
                break
            }
        }
    }
}

/**
 * LCDC4, タイルID、タイル上の(x,y) → dot data
 */
private fun getDotDataOfPixelOnTileForBackgroundAndWindow(
    memory: Memory,
    LCDC4: Boolean,
    tileId: Int8,
    xOnTile: Int,
    yOnTile: Int
): Int2 {
    val tileDataBaseAddress = if (LCDC4) 0x8000 else (if (tileId < 128) 0x9000 else 0x8000)
    return getDotDataOfPixelOnTileForTileDataBaseAddress(memory, tileDataBaseAddress, tileId, xOnTile, yOnTile)
}

private fun getDotDataOfPixelOnTileForSprites(
    memory: Memory,
    tileId: Int8,
    xOnTile: Int,
    yOnTile: Int
): Int2 {
    return getDotDataOfPixelOnTileForTileDataBaseAddress(memory, 0x8000, tileId, xOnTile, yOnTile)
}

private fun getDotDataOfPixelOnTileForTileDataBaseAddress(
    memory: Memory,
    tileDataBaseAddress: Address,
    tileId: Int8,
    xOnTile: Int,
    yOnTile: Int
): Int2 {
    val TILE_BYTES = 16
    val tileDataHeadAddress = tileDataBaseAddress + tileId * TILE_BYTES
    val byte0 = memory.get(tileDataHeadAddress + 2 * yOnTile + 0)
    val byte1 = memory.get(tileDataHeadAddress + 2 * yOnTile + 1)
    val bit = 7 - xOnTile
    val dotData: Int2 = ((byte0 and (1 shl bit)) shr bit) + ((byte1 and (1 shl bit)) shr bit) * 2
    return dotData
}

typealias Address = Int
typealias Int8 = Int
typealias Int2 = Int

interface Memory {
    fun get(addr: Address): Int8
    fun getLY(): Int8
}
