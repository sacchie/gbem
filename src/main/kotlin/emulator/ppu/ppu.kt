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
    val lcdc = LCDC(memory.get(ADDR_LCDC))
    // Ignoring LCDC.0 (BG Enabled)
    val bgTileMapHead: Address = if (lcdc.isBgTileMapArea0x9c00()) 0x9C00 else 0x9800

    for (iTile in 0..1023) {
        val tileX = iTile % 32
        val tileY = iTile / 32
        val tileId = memory.get(bgTileMapHead + iTile)
        drawBackgroundTileToScreen(memory, tileX, tileY, tileId, lcdc, drawPixelToScreen)
    }
}

fun drawBackgroundTileToScreen(
    memory: Memory,
    tileX: Int,
    tileY: Int,
    tileId: Int8,
    lcdc: LCDC,
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit
) {
    val BGP = memory.get(ADDR_BGP)
    for (yOnTile in 0..7) {
        for (xOnTile in 0..7) {
            val xOnScreen = tileX * TILE_W + xOnTile
            val yOnScreen = tileY * TILE_H + yOnTile
            val dotData = getDotDataOfPixelOnTileForBackgroundAndWindow(memory, lcdc, tileId, xOnTile, yOnTile)
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

data class DebugParams(
    var drawBackground: Boolean,
    var drawWindow: Boolean,
    var drawSprites: Boolean
) {
    override fun toString(): String {
        return "BG:${drawBackground}/Win:${drawWindow}/Sp:${drawSprites}"
    }
}

@JvmInline
value class LCDC(private val value: Int8) {
    fun isBgAndWindowEnabled(): Boolean = value and 1 > 0
    fun isObjEnabled(): Boolean = value and 1.shl(1) > 0
    fun isObjSize8x16(): Boolean = value and 1.shl(2) > 0
    fun isBgTileMapArea0x9c00(): Boolean = value and 1.shl(3) > 0
    fun isBgAndWindowTileDataArea0x8000(): Boolean = value and 1.shl(4) > 0
    fun isWindowEnabled(): Boolean = value and 1.shl(5) > 0
    fun isWindowTileMapArea0x9c00(): Boolean = value and 1.shl(6) > 0
    fun isLCDAndPPUEnabled(): Boolean = value and 1.shl(7) > 0
}

/**
 * draw one scanline for LY = ly
 */
fun drawScanlineInViewport(
    memory: Memory,
    ly: Int,
    windowInternalLineCounter: Int,
    incrementWindowInternalLineCounter: () -> Unit,
    debugParams: DebugParams,
    drawPixelToScreen: (x: Int, y: Int, color: LCDColor) -> Unit,
) {
    if (ly >= VIEWPORT_H) {
        return
    }

    val bgAndWindowDotDataMap = MutableList<Int2?>(VIEWPORT_W) { null }
    val lcdc = LCDC(memory.get(ADDR_LCDC))
    val bgAndWindowEnabled = lcdc.isBgAndWindowEnabled()
    val windowEnabled = lcdc.isWindowEnabled()
    for (lx in 0 until VIEWPORT_W) {
        val bgDotData = if (debugParams.drawBackground && bgAndWindowEnabled) getDotDataForBackdround(
            memory,
            lcdc,
            ly,
            lx
        ) else null
        val windowDotData = if (debugParams.drawWindow && windowEnabled && bgAndWindowEnabled) getDotDataForWindow(
            memory,
            lcdc,
            ly,
            lx,
            windowInternalLineCounter
        ) else null
        bgAndWindowDotDataMap[lx] = windowDotData ?: bgDotData
    }

    // このscanlineで、1dotでもwindowを描画しようとするときのみ、window internal line counterをincrementする必要がある
    if (windowEnabled && (0 until VIEWPORT_W).any {
            getDotDataForWindow(
                memory,
                lcdc,
                ly,
                it,
                windowInternalLineCounter
            ) != null
        }) {
        incrementWindowInternalLineCounter()
    }

    val spriteDataMap = mutableMapOf<Int, Pair<LCDColor, Boolean>>()
    if (debugParams.drawSprites && lcdc.isObjEnabled()) {
        forEachSpritePixelForScanlineInViewportToBuffer(memory, lcdc, ly) { x, color, bgAndWindowOverObj ->
            spriteDataMap[x] = Pair(color, bgAndWindowOverObj)
        }
    }

    val BGP = memory.get(ADDR_BGP)
    for (lx in 0 until VIEWPORT_W) {
        val spriteData = spriteDataMap[lx]
        val bgAndWindowDotData = bgAndWindowDotDataMap[lx]
        if (spriteData == null || (spriteData.second && bgAndWindowDotData != null && bgAndWindowDotData != 0)) {
            // lx にスプライトが無い、あるいはあるがBG/Window優先
            bgAndWindowDotData?.let { drawPixelToScreen(lx, ly, getColorForBackgroundAndWindow(it, BGP)) }
        } else {
            // スプライト優先
            drawPixelToScreen(lx, ly, spriteData.first)
        }
    }
}

private fun getDotDataForBackdround(
    memory: Memory,
    lcdc: LCDC,
    ly: Int,
    lx: Int,
): Int2 {
    val bgTileMapHead: Address = if (lcdc.isBgTileMapArea0x9c00()) 0x9C00 else 0x9800
    val SCX = memory.get(ADDR_SCX)
    val SCY = memory.get(ADDR_SCY)
    val yOnScreen = (SCY + ly) % SCREEN_H
    val tileY = yOnScreen / TILE_H
    val yOnTile = yOnScreen % TILE_H

    val xOnScreen = (SCX + lx) % SCREEN_W
    val tileX = xOnScreen / TILE_W
    val xOnTile = xOnScreen % TILE_W

    val iTile = tileX + 32 * tileY
    val tileId = memory.get(bgTileMapHead + iTile)

    return getDotDataOfPixelOnTileForBackgroundAndWindow(memory, lcdc, tileId, xOnTile, yOnTile)
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

// @return null は window がないケース
private fun getDotDataForWindow(
    memory: Memory,
    lcdc: LCDC,
    ly: Int,
    lx: Int,
    windowInternalLineCounter: Int
): Int2? {
    val windowTileMapHead: Address = if (lcdc.isWindowTileMapArea0x9c00()) 0x9C00 else 0x9800
    val WX = memory.get(ADDR_WX) // X position plus 7
    val WY = memory.get(ADDR_WY)
    if (ly < WY) {
        return null
    }
    val yOnWindow = windowInternalLineCounter
    val tileY = yOnWindow / TILE_H
    val yOnTile = yOnWindow % TILE_H
    if (lx < WX - 7) {
        return null
    }
    val xOnWindow = lx - (WX - 7)
    val tileX = xOnWindow / TILE_W
    val xOnTile = xOnWindow % TILE_W

    val iTile = tileX + 32 * tileY
    val tileId = memory.get(windowTileMapHead + iTile)

    return getDotDataOfPixelOnTileForBackgroundAndWindow(memory, lcdc, tileId, xOnTile, yOnTile)
}

private fun forEachSpritePixelForScanlineInViewportToBuffer(
    memory: Memory,
    lcdc: LCDC,
    ly: Int,
    cb: (x: Int, color: LCDColor, bgAndWindowOverObj: Boolean) -> Unit
) {
    // OAM (0xFE00-FE9F) から ly にあるsprite（上限10個）を取ってきて、描画
    val is8x16Mode = lcdc.isObjSize8x16()
    var drawCount = 0
    for (nthOam in 0..39) {
        val oamData = memory.getOamData(nthOam)
        val yPosition = oamData.yPosition
        val xPosition = oamData.xPosition
        val tileId = oamData.tileId
        val attributes = oamData.attributes
        val bgAndWindowOverObj = attributes.and(0b10000000) > 0
        val yFlip = attributes.and(0b01000000) > 0
        val xFlip = attributes.and(0b00100000) > 0
        val OBP = if (attributes.and(0b00010000) > 0) memory.get(ADDR_OBP1) else memory.get(ADDR_OBP0)
        val ySize = if (is8x16Mode) 16 else 8
        // スプライトの存在範囲y座標：yPosition - yPosition + 7/15
        if (ly in yPosition until yPosition + ySize) {
            drawCount++
            val yOnTile = if (yFlip) ySize - 1 - (ly - yPosition) else ly - yPosition
            for (xTmp in 0 until 8) {
                val xOnTile = if (xFlip) 7 - (xTmp) else xTmp
                val xOnScreen = xPosition + xTmp
                val dotData = getDotDataOfPixelOnTileForSprites(memory, tileId, xOnTile, yOnTile)
                val color = getColorForSprite(dotData, OBP)
                if (color != null) {
                    cb(xOnScreen, color, bgAndWindowOverObj)
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
    lcdc: LCDC,
    tileId: Int8,
    xOnTile: Int,
    yOnTile: Int
): Int2 {
    val tileDataBaseAddress =
        if (lcdc.isBgAndWindowTileDataArea0x8000()) 0x8000 else (if (tileId < 128) 0x9000 else 0x8000)
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

data class OamData(
    val yPosition: Int8,
    val xPosition: Int8,
    val tileId: Int8,
    val attributes: Int8
)

interface Memory {
    fun get(addr: Address): Int8

    fun getLY(): Int8

    fun getOamData(nth: Int): OamData {
        assert(nth in 0..39)
        val oamHeadAddress = 0xFE00 + 4 * nth
        return OamData(
            yPosition = get(oamHeadAddress) - 16,
            xPosition = get(oamHeadAddress + 1) - 8,
            tileId = get(oamHeadAddress + 2),
            attributes = get(oamHeadAddress + 3)
        )
    }
}
